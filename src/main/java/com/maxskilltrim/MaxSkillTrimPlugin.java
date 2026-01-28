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
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@PluginDescriptor(
        name = "Max Skill Trim"
)
public class MaxSkillTrimPlugin extends Plugin
{
    private static final Trim[] dynamicTrims = new Trim[]{
            new Trim(-432432), new Trim(-432433), new Trim(-432434), new Trim(-432435), new Trim(-432436),
            new Trim(-432437), new Trim(-432438), new Trim(-432439), new Trim(-432440), new Trim(-432441),
            new Trim(-432442), new Trim(-432443), new Trim(-432444), new Trim(-432445), new Trim(-432446),
            new Trim(-432447), new Trim(-432448), new Trim(-432449), new Trim(-432450), new Trim(-432451),
            new Trim(-432452) // 21 of them, one for each skill, the max amount of different trims you could display at once
    };
    @Inject
    ConfigManager configManager;
    
    @Inject
    private MaxSkillTrimConfig maxSkillTrimConfig;
    private NavigationButton navButton;
    @Inject
    private ClientToolbar pluginToolbar;
    public static final File MAXSKILLTRIMS_DIR = new File(RuneLite.RUNELITE_DIR.getPath(), "max-skill-trims");
    private static final BufferedImage ICON_NAVBAR = ImageUtil.loadImageResource(MaxSkillTrimPlugin.class, "/icon_navbar.png");
    private static final int SCRIPTID_STATS_INIT = 393;
    private static final int SCRIPTID_STATS_REFRESH = 394;
    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    private Widget currentWidget;
    MaxSkillTrimPanel maxSkillTrimPanel;

    private final HashMap<Skill, Widget> trimWidgets = new HashMap<>();

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

        upgradeConfigIfNeeded();
        addDefaultTrims();

        maxSkillTrimPanel = injector.getInstance(MaxSkillTrimPanel.class);

        navButton = NavigationButton.builder()
                .tooltip("Max Skill Trim")
                .priority(5)
                .icon(ICON_NAVBAR)
                .panel(maxSkillTrimPanel)
                .build();

        if(maxSkillTrimConfig.getShowNavButton()) {
            pluginToolbar.addNavigation(navButton);
        }

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

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        switch (event.getGameState())
        {
            case LOGIN_SCREEN:
                removeTrimWidgetContainers();
                break;
            case LOGGED_IN:
                buildTrimWidgetContainers();
                break;
        }
    }

    @Subscribe
    public void onStatChanged(StatChanged event)
    {
        updateTrim(event.getSkill());
    }

    private void buildTrimWidgetContainers() {
        Widget skillsContainer = client.getWidget(InterfaceID.Stats.UNIVERSE);
        if (skillsContainer == null) return;

        for (Widget skillTile : skillsContainer.getStaticChildren()) {
            buildTrim(skillTile);
        }
    }

    private void removeTrimWidgetContainers() {
        trimWidgets.values().forEach(Utils::detachWidget);
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

    public void updateTrim(Skill skill)
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
        String trimFile = maxSkillTrimPanel.selectTrim(client, skill);

        if (trimFile == null)
            return null;

        return trimForImage(trimFile);
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

    Trim trimForImage(String filename)
    {
        for (Trim t : dynamicTrims)
        {
            if (t.imageFile.equals(filename))
                return t;

            if (t.imageFile.isEmpty())
            {
                overrideSprites(t, filename);
                return t;
            }
        }

        //Recover a now unused trim
        Set<String> inUse = Arrays.stream(Skill.values())
                .map(s -> maxSkillTrimPanel.selectTrim(client, s))
                .collect(Collectors.toSet());

        for (Trim t : dynamicTrims)
        {
            if (!inUse.contains(t.imageFile))
            {
                overrideSprites(t, filename);
                return t;
            }
        }

        throw new RuntimeException("Out of dynamic trims");
    }

    void overrideSprites(Trim trim, String filename)
    {
        SpritePixels spritePixels = getSpritePixels(filename);

        if (spritePixels == null)
        {
            return;
        }

        log.info("Assigning trim " + filename + " to " + trim.spriteID);

        client.getSpriteOverrides().remove(trim.spriteID);
        client.getWidgetSpriteCache().reset();
        client.getSpriteOverrides().put(trim.spriteID, spritePixels);

        trim.imageFile = filename;
    }
    
    private void upgradeConfigIfNeeded()
    {
        int version = 0;
        String versionValue = configManager.getConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.CONFIG_VERSION);
        if (versionValue != null)
            version = Integer.parseInt(versionValue);

        final Integer CurrentVersion = 1;
        if (version > CurrentVersion)
            log.error("[Max Skill Trim] Config is from the future\nversion: " + versionValue + " is greater than current version: " + CurrentVersion + "\nconfig may not be read correctly\np.s. Can I have a go on your time machine? pretty please");

        for (;version < CurrentVersion; version++)
        {
            switch (version)
            {
                case 0:
                    upgradeConfigToV1();
                    break;
            }
        }
    }
    
    private void upgradeConfigToV1()
    {
        log.info("Upgrading " + MaxSkillTrimConfig.GROUP_NAME + " config to version 1");
        String xpTrim = configManager.getConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.SELECTED_MAX_EXPERIENCE_TRIM);
        String lvlTrim = configManager.getConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.SELECTED_MAX_LEVEL_TRIM);

        // null-safe, default true, boolean check
        boolean showXp = !"false".equals(configManager.getConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.SHOW_MAX_EXPERIENCE_TRIM));
        boolean showLvl = !"false".equals(configManager.getConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.SHOW_MAX_LEVEL_TRIM));

        // Lvl hid xp so no need to migrate that setting
        if (showLvl && xpTrim.equals(lvlTrim))
            showXp = false;

        String trimConditions = "";

        if (showXp)
            trimConditions += xpTrim + ":xp >= 200m;";

        if (showLvl)
            trimConditions += lvlTrim + ":Level >= 99;";

        configManager.setConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.TRIM_CONDITIONS, trimConditions);
        configManager.setConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.CONFIG_VERSION, "1");
    }
}
