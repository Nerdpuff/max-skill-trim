package com.maxskilltrim;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.*;
import net.runelite.client.RuneLite;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@PluginDescriptor(
        name = "Max Skill Trim"
)
public class MaxSkillTrimPlugin extends Plugin
{
    private static final Trim maxLevelTrim = new Trim(-432432);
    private static final Trim maxExperienceTrim = new Trim(-432433);
    @Inject
    private MaxSkillTrimConfig maxSkillTrimConfig;
    private NavigationButton navButton;
    @Inject
    private ClientToolbar pluginToolbar;
    public static final File MAXSKILLTRIMS_DIR = new File(RuneLite.RUNELITE_DIR.getPath(), "max-skill-trims");
    private static final int SCRIPTID_STATS_INIT = 393;
    private static final int SCRIPTID_STATS_REFRESH = 394;
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    private Widget currentWidget;

    private boolean mockOverridesEnabled = false;

    public void setMockOverridesEnabled(boolean enabled) {
        this.mockOverridesEnabled = enabled;
        clientThread.invokeLater(this::updateTrims);
    }

    private final HashMap<Skill, Widget> trimWidgets = new HashMap<>();
    private final HashMap<Skill, Trim> trimOverrides = new HashMap<>();

    private final List<Skill> skillsInLastRow = new ArrayList<Skill>(3) {
        {
            add(Skill.CONSTRUCTION);
            add(Skill.SAILING);
            add(Skill.HUNTER);
        }
    };

    @Provides
    MaxSkillTrimConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(MaxSkillTrimConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        if (!MAXSKILLTRIMS_DIR.exists())
        {
            MAXSKILLTRIMS_DIR.mkdirs();
        }

        addDefaultTrims();

        MaxSkillTrimPanel maxSkillTrimPanel = injector.getInstance(MaxSkillTrimPanel.class);

        BufferedImage icon;
        synchronized (ImageIO.class)
        {
            icon = ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream("/icon.png")));
        }

        navButton = NavigationButton.builder()
                .tooltip("Max Skill Trim")
                .priority(5)
                .icon(icon)
                .panel(maxSkillTrimPanel)
                .build();

        if(maxSkillTrimConfig.getShowNavButton()) {
            pluginToolbar.addNavigation(navButton);
        }

        overrideSprites(maxLevelTrim, maxSkillTrimConfig.getSelectedMaxLevelTrimFilename());
        overrideSprites(maxExperienceTrim, maxSkillTrimConfig.getSelectedMaxExperienceTrimFilename());

        if (client.getGameState() == GameState.LOGGED_IN) {
            clientThread.invoke(this::buildTrimWidgetContainers);
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        pluginToolbar.removeNavigation(navButton);
        clientThread.invoke(() -> {
            removeTrimWidgetContainers();
        });
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        if (event.getScriptId() == SCRIPTID_STATS_INIT || event.getScriptId() == SCRIPTID_STATS_REFRESH) {
            Widget widget = event.getScriptEvent().getSource();

            currentWidget = widget.getId() == InterfaceID.Stats.UNIVERSE ? null : widget;
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if ((event.getScriptId() == SCRIPTID_STATS_INIT || event.getScriptId() == SCRIPTID_STATS_REFRESH) && currentWidget != null) {
            buildTrim(currentWidget);
        }
    }

    private void buildTrimWidgetContainers() {
        Widget skillsContainer = client.getWidget(InterfaceID.Stats.UNIVERSE);
        if (skillsContainer == null) return;

        for (Widget skillTile : skillsContainer.getStaticChildren()) {
            buildTrim(skillTile);
        }
    }

    private void removeTrimWidgetContainers() {
        trimWidgets.values().forEach(trim -> {
            Widget[] children = trim.getParent().getChildren();

            if(children == null) {
                return;
            }

            for (int i = 0; i < children.length; i++) {
                if (trim == children[i]) {
                    children[i] = null;
                }
            }
        });
        trimWidgets.clear();
    }

    private void buildTrim(Widget parent) {
        if(parent.getType() != WidgetType.LAYER) return;

        int idx = (parent.getId() & 0xFFFF) - 1;
        SkillData skillData = SkillData.get(idx);
        if (skillData == null) return;
        Skill skill = skillData.getSkill();

        if (!trimWidgets.containsKey(skill))
            trimWidgets.put(skill, createWidget(parent, skill));

        updateTrim(skill);
    }

    private void updateTrim(Skill skill)
    {
        Widget trimWidget = trimWidgets.getOrDefault(skill, null);

        // Widgets have not been set up yet,
        // safe to ignore this attempt to update as it will load directly into the correct state when ready
        if (trimWidget == null)
            return;

        Trim trim = SelectTrim(skill);
        if (trim == null)
            trimWidget.setOpacity(255);
        else
            trimWidget.setOpacity(0).setSpriteId(trim.spriteID);
    }

    private Trim SelectTrim(Skill skill)
    {
        if (mockOverridesEnabled)
            return trimOverrides.getOrDefault(skill, null);

        if (maxSkillTrimConfig.getShowMaxExperienceTrim() && client.getSkillExperience(skill) >= Experience.MAX_SKILL_XP)
            return maxExperienceTrim;

        if (maxSkillTrimConfig.showMaxLevelTrim() && client.getRealSkillLevel(skill) >= Experience.MAX_REAL_LEVEL)
            return maxLevelTrim;

        return null;
    }

    private Widget createWidget(Widget parent, Skill skill) {
        boolean isSkillLastRow = skillsInLastRow.contains(skill);

        return parent.createChild(-1, WidgetType.GRAPHIC)
                .setYPositionMode(isSkillLastRow ? WidgetPositionMode.ABSOLUTE_TOP + 1 : WidgetPositionMode.ABSOLUTE_TOP + 2)
                .setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT + 1)
                .setOriginalHeight(29)
                .setOriginalWidth(60)
                .setWidthMode(parent.getWidthMode())
                .setHeightMode(parent.getHeightMode())
                .setOpacity(255)
                .setName("Max Skill Trim - " + skill.getName());
    }


    void setMockTrimState(Skill skill, TrimType trimType) {

        Trim trim = null;
        if (trimType != null)
        {
            switch (trimType)
            {
                case MAX_LEVEL:
                    trim = maxLevelTrim;
                    break;
                case MAX_EXPERIENCE:
                    trim = maxExperienceTrim;
                    break;
            }
        }
        trimOverrides.put(skill, trim);
        updateTrim(skill);
    }

    void updateTrims() {
        for (Skill skill : Skill.values())
            updateTrim(skill);
    }

    private void addDefaultTrims()
    {
        try
        {
            Files.copy(Objects.requireNonNull(getClass().getResourceAsStream("/full-trim.png")), Paths.get(MAXSKILLTRIMS_DIR.toString(), "/full-trim.png"), StandardCopyOption.REPLACE_EXISTING);
        }
        catch (IOException e)
        {
            log.warn(null, e);
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (event.getGroup().equals(MaxSkillTrimConfig.GROUP_NAME))
        {
            switch(event.getKey()) {
                case MaxSkillTrimConfig.SELECTED_MAX_LEVEL_TRIM:
                    overrideSprites(maxLevelTrim, event.getNewValue());
                    break;
                case MaxSkillTrimConfig.SELECTED_MAX_EXPERIENCE_TRIM:
                    overrideSprites(maxExperienceTrim, event.getNewValue());
                    break;
                case MaxSkillTrimConfig.SHOW_MAX_EXPERIENCE_TRIM:
                case MaxSkillTrimConfig.SHOW_MAX_LEVEL_TRIM:
                    clientThread.invokeLater(this::updateTrims);
                    break;
                case MaxSkillTrimConfig.SHOW_NAV_BUTTON:
                    boolean showNavButton = Boolean.TRUE.toString().equals(event.getNewValue());

                    if(showNavButton) pluginToolbar.addNavigation(navButton);
                    if(!showNavButton) pluginToolbar.removeNavigation(navButton);
                    break;
            }
        }
    }

    public SpritePixels getSpritePixels(String filename)
    {
        File spriteFile = new File(MAXSKILLTRIMS_DIR, filename);
        if (!spriteFile.exists()) {
            log.debug("Sprite doesn't exist ({}): ", spriteFile.getPath());
            spriteFile = new File(MAXSKILLTRIMS_DIR, "full-trim.png");
            if (!spriteFile.exists()) {
                log.debug("Fallback sprite doesn't exist ({}): ", spriteFile.getPath());
                return null;
            }
        }
        try
        {
            synchronized (ImageIO.class)
            {
                BufferedImage image = ImageIO.read(spriteFile);
                return ImageUtil.getImageSpritePixels(image, client);
            }
        }
        catch (RuntimeException | IOException ex)
        {
            log.debug("Unable to find image ({}): ", spriteFile.getPath());
        }
        return null;
    }

    void overrideSprites(Trim trim, String filename)
    {
        SpritePixels spritePixels = getSpritePixels(filename);

        if (spritePixels == null)
        {
            return;
        }

        client.getSpriteOverrides().remove(trim.spriteID);
        client.getWidgetSpriteCache().reset();
        client.getSpriteOverrides().put(trim.spriteID, spritePixels);
    }
}
