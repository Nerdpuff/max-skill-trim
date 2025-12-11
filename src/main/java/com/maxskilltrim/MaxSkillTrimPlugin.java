package com.maxskilltrim;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.widgets.*;
import net.runelite.api.gameval.InterfaceID;
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
import java.util.Objects;

@Slf4j
@PluginDescriptor(
        name = "Max Skill Trim"
)
public class MaxSkillTrimPlugin extends Plugin
{
    private static final Trim maxLevelTrim = new Trim(-666, TrimType.MAX_LEVEL);
    private static final Trim maxExperienceTrim = new Trim(-667, TrimType.MAX_EXPERIENCE);
    private MaxSkillTrimPanel maxSkillTrimPanel;
    @Inject
    private MaxSkillTrimConfig maxSkillTrimConfig;
    private NavigationButton navButton;
    @Inject
    private ClientToolbar pluginToolbar;
    public static final File MAXSKILLTRIMS_DIR = new File(RuneLite.RUNELITE_DIR.getPath() + File.separator + "max-skill-trims");
    private static final int SCRIPTID_STATS_INIT = 394;
    private static final int SCRIPTID_STATS_REFRESH = 393;
    private static final int WIDGET_CHILD_ID_MASK = 0xFFFF;
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    private Widget currentWidget;
    private SkillData lastSkillBuiltTrimFor;
    private final Widget[] maxLevelTrimWidgets = new Widget[SkillData.values().length];
    private final Widget[] maxExperienceTrimWidgets = new Widget[SkillData.values().length];
    private static final String PROGRESS_BAR_PLUGIN_AUTHOR = "m0bilebtw";
    private static final String PROGRESS_BAR_PLUGIN_NAME = "SkillsTabProgressBarsPlugin";

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

        maxSkillTrimPanel = injector.getInstance(MaxSkillTrimPanel.class);

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
            removeTrimWidgetContainers(maxLevelTrimWidgets);
            removeTrimWidgetContainers(maxExperienceTrimWidgets);
        });
    }

    @Subscribe
    public void onScriptPreFired(ScriptPreFired event) {
        if (event.getScriptId() == SCRIPTID_STATS_INIT || event.getScriptId() == SCRIPTID_STATS_REFRESH) {
            Widget widget = event.getScriptEvent().getSource();
            if (widget.getId() == InterfaceID.Stats.UNIVERSE) {
                currentWidget = null;
            } else {
                currentWidget = widget;
            }
        }
    }

    @Subscribe
    public void onScriptPostFired(ScriptPostFired event) {
        if (
            event.getScriptId() == SCRIPTID_STATS_INIT
            || event.getScriptId() == SCRIPTID_STATS_REFRESH
            && currentWidget != null
        ) {
            buildTrim(currentWidget);
            moveProgressBarsToTop(currentWidget);
        }
    }

    private void buildTrimWidgetContainers() {
        Widget skillsContainer = client.getWidget(InterfaceID.Stats.UNIVERSE);
        if (skillsContainer == null) {
            return;
        }

        for (Widget skillTile : skillsContainer.getStaticChildren()) {
            buildTrim(skillTile);
            moveProgressBarsToTop(skillTile);
        }
    }

    private void removeTrimWidgetContainers(Widget[] trims) {
        for(Widget trim: trims) {
            if (trim == null) {
                continue;
            }

            Widget[] children = trim.getParent().getChildren();
            if (children != null) {
                for (int i = 0; i < children.length; i++) {
                    if (trim == children[i]) {
                        children[i] = null;
                    }
                }
            }
        }
    }

    private int getChildId(int id) {
        return id & WIDGET_CHILD_ID_MASK;
    }

    private void buildTrim(Widget parent) {
        if (parent.getType() != WidgetType.LAYER) {
            log.error("buildTrim called with non-layer widget");
            return;
        }

        int idx = getChildId(parent.getId()) - 1;
        SkillData skill = SkillData.get(idx);
        if (skill == null) {
            return;
        }

        if (skill == lastSkillBuiltTrimFor) {
            return;
        }
        lastSkillBuiltTrimFor = skill;

        maxLevelTrimWidgets[idx] = createWidget(parent, skill, maxLevelTrim);
        maxExperienceTrimWidgets[idx] = createWidget(parent, skill, maxExperienceTrim);

        updateTrim(skill, maxLevelTrimWidgets[idx], maxLevelTrim);
        updateTrim(skill, maxExperienceTrimWidgets[idx], maxExperienceTrim);
    }

    private Widget createWidget(Widget parent, SkillData skill, Trim trim) {
        Widget t = parent.createChild(-1, WidgetType.GRAPHIC)
                .setYPositionMode(WidgetPositionMode.ABSOLUTE_TOP)
                .setOriginalHeight(parent.getOriginalHeight())
                .setOriginalWidth(parent.getOriginalWidth())
                .setWidthMode(parent.getWidthMode())
                .setHeightMode(parent.getHeightMode())
                .setOpacity(255)
                .setSpriteId(trim.spriteID)
                .setHasListener(true);

        JavaScriptCallback cb = ev -> updateTrim(skill, t, trim);
        t.setOnVarTransmitListener(cb);

        t.revalidate();

        return t;
    }

    private Boolean isProgressBarWidget(Widget widget) {
        final Object[] listeners;

        try {
            listeners = widget.getOnVarTransmitListener();
        } catch (NullPointerException e) {
            return false;
        }

        if (listeners == null) return false;

        for (Object listener : listeners) {
            if (listener == null) continue;

            String s = listener.toString();
            if (s.contains(PROGRESS_BAR_PLUGIN_AUTHOR) && s.contains(PROGRESS_BAR_PLUGIN_NAME)) return true;
        }

        return false;
    }

    private void moveProgressBarsToTop(Widget parent) {
        Widget[] children = parent.getChildren();
        if (children != null) {
            // Separate progress bar widgets and other widgets into separate lists
            ArrayList<Widget> progressBarWidgets = new ArrayList<Widget>();
            ArrayList<Widget> otherWidgets = new ArrayList<Widget>();
            for (Widget child : children) {
                if (isProgressBarWidget(child)) {
                    progressBarWidgets.add(child);
                } else {
                    otherWidgets.add(child);
                }
            }
            // Recombine them with progress bar widgets at the end, which also preserves order
            ArrayList<Widget> combinedWidgets = new ArrayList<Widget>(otherWidgets);
            combinedWidgets.addAll(progressBarWidgets);
            Widget[] reorderedChildren = combinedWidgets.toArray(new Widget[0]);
            System.arraycopy(reorderedChildren, 0, children, 0, children.length);
        }
    }

    private void updateTrim(SkillData skill, Widget widget, Trim trim) {
        final int currentXP = client.getSkillExperience(skill.getSkill());
        final boolean isMaxExperience =  currentXP >= Experience.MAX_SKILL_XP;
        final int currentLevel = Experience.getLevelForXp(currentXP);

        switch(trim.trimType) {
            case MAX_LEVEL:
                if(!maxSkillTrimConfig.showMaxLevelTrim() || (isMaxExperience && maxSkillTrimConfig.getShowMaxExperienceTrim())) {
                    widget.setOpacity(255);
                } else if(currentLevel >= Experience.MAX_REAL_LEVEL) {
                    widget.setOpacity(0);
                }
                break;
            case MAX_EXPERIENCE:
                if(!maxSkillTrimConfig.getShowMaxExperienceTrim()) {
                    widget.setOpacity(255);
                } else if(isMaxExperience) {
                    widget.setOpacity(0);
                }
                break;
        }
    }

    void updateTrims() {
        for(int i = 0; i < maxLevelTrimWidgets.length; i++) {
            SkillData skill = SkillData.get(i);
            if (skill != null) {
                updateTrim(skill, maxLevelTrimWidgets[i], maxLevelTrim);
            }
        }

        for(int i = 0; i < maxExperienceTrimWidgets.length; i++) {
            SkillData skill = SkillData.get(i);
            if (skill != null) {
                updateTrim(skill, maxExperienceTrimWidgets[i], maxExperienceTrim);
            }
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
        File spriteFile = new File(MAXSKILLTRIMS_DIR + File.separator + filename);
        if (!spriteFile.exists())
        {
            log.debug("Sprite doesn't exist (" + spriteFile.getPath() + "): ");
            return null;
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
            log.debug("Unable to find image (" + spriteFile.getPath() + "): ");
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
