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
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@PluginDescriptor(
        name = "Max Skill Trim"
)
public class MaxSkillTrimPlugin extends Plugin
{
    private static final Trim maxLevelTrim = new Trim(-432432, TrimType.MAX_LEVEL);
    private static final Trim maxExperienceTrim = new Trim(-432433, TrimType.MAX_EXPERIENCE);
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

    private final HashMap<String, MaxSkillTrimWidget> maxSkillTrimWidgetHashMap = new HashMap<>();

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
            removeTrimWidgetContainers(maxSkillTrimWidgetHashMap);
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

    private void removeTrimWidgetContainers(HashMap<String, MaxSkillTrimWidget> trims) {
        maxSkillTrimWidgetHashMap.values().removeIf(trim -> {
            if(trim.widget != null) {
                Widget[] children = trim.widget.getParent().getChildren();

                if(children == null) {
                    return true;
                }

                for (int i = 0; i < children.length; i++) {
                    if (trim.widget == children[i]) {
                        children[i] = null;
                    }
                }
            }
            return true;
        });
    }

    private void buildTrim(Widget parent) {
        if(parent.getType() != WidgetType.LAYER) return;

        int idx = (parent.getId() & 0xFFFF) - 1;
        SkillData skill = SkillData.get(idx);
        if (skill == null) return;

        MaxSkillTrimWidget maxLevel = maxSkillTrimWidgetHashMap.putIfAbsent(TrimType.MAX_LEVEL.name() + skill.name(), new MaxSkillTrimWidget(skill, TrimType.MAX_LEVEL, false, null));
        if(maxLevel != null && maxLevel.widget == null) {
            maxLevel.widget = createWidget(parent, skill, maxLevelTrim);
            updateTrim(maxLevel);
        }

        MaxSkillTrimWidget maxExp = maxSkillTrimWidgetHashMap.putIfAbsent(TrimType.MAX_EXPERIENCE.name() + skill.name(), new MaxSkillTrimWidget(skill, TrimType.MAX_EXPERIENCE, false, null));
        if(maxExp != null && maxExp.widget == null) {
            maxExp.widget = createWidget(parent, skill, maxExperienceTrim);
            updateTrim(maxExp);
        }
    }

    private Widget createWidget(Widget parent, SkillData skill, Trim trim) {
        boolean isSkillLastRow = skillsInLastRow.contains(skill.getSkill());

        return parent.createChild(-1, WidgetType.GRAPHIC)
                .setYPositionMode(isSkillLastRow ? WidgetPositionMode.ABSOLUTE_TOP + 1 : WidgetPositionMode.ABSOLUTE_TOP + 2)
                .setXPositionMode(WidgetPositionMode.ABSOLUTE_LEFT + 1)
                .setOriginalHeight(29)
                .setOriginalWidth(60)
                .setWidthMode(parent.getWidthMode())
                .setHeightMode(parent.getHeightMode())
                .setOpacity(255)
                .setSpriteId(trim.spriteID)
                .setName("Max Skill Trim - " + trim.trimType + " - " + skill.getSkill().getName());
    }

    private void updateTrim(MaxSkillTrimWidget widget) {
        int currentXP = client.getSkillExperience(widget.skillData.getSkill());
        boolean isMaxExperience = currentXP >= Experience.MAX_SKILL_XP;
        int currentLevel = Experience.getLevelForXp(currentXP);

        boolean showMaxLevelTrim = maxSkillTrimConfig.showMaxLevelTrim();
        boolean showMaxExperienceTrim = maxSkillTrimConfig.getShowMaxExperienceTrim();

        // Highest priority: developer mode with mock enabled
        if (mockOverridesEnabled) {
            widget.widget.setOpacity(widget.isMockEnabled ? 0 : 255);
            return;
        }

        boolean maxExpActive = showMaxExperienceTrim && isMaxExperience;
        boolean showTrim = false;

        if (widget.trimType == TrimType.MAX_EXPERIENCE) {
            showTrim = maxExpActive;
        }

        if (widget.trimType == TrimType.MAX_LEVEL) {
            showTrim = !maxExpActive && showMaxLevelTrim && currentLevel >= Experience.MAX_REAL_LEVEL;
        }

        widget.widget.setOpacity(showTrim ? 0 : 255);
    }

    void setMockTrimState(SkillData skill, boolean isMockEnabled, TrimType trimType) {
        MaxSkillTrimWidget widget = maxSkillTrimWidgetHashMap.get(trimType.name() + skill.name());
        if (widget != null) {
            widget.isMockEnabled = isMockEnabled;
            updateTrim(widget);
        }
    }

    void updateTrims() {
        for (MaxSkillTrimWidget widget : maxSkillTrimWidgetHashMap.values()) {
            updateTrim(widget);
        }
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
