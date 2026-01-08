package com.maxskilltrim;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(MaxSkillTrimConfig.GROUP_NAME)
public interface MaxSkillTrimConfig extends Config
{
    String GROUP_NAME = "maxskilltrim";
    String CONFIG_VERSION = "configVersion";
    String SHOW_MAX_LEVEL_TRIM = "showMaxLevelTrim";
    String SELECTED_MAX_LEVEL_TRIM = "selectedMaxLevelTrim";
    String SHOW_MAX_EXPERIENCE_TRIM = "showMaxExperienceTrim";
    String SELECTED_MAX_EXPERIENCE_TRIM = "selectedMaxExperienceTrim";
    String TRIM_CONDITIONS = "trimConditions";
    String SHOW_NAV_BUTTON = "showNavButton";

    @ConfigItem(
            keyName = SHOW_MAX_LEVEL_TRIM,
            name = "Show trims on level 99 skills?",
            description = "Toggles whether or not show skill trims on level 99 skills"
    )
    default boolean showMaxLevelTrim() { return true; }

    @ConfigItem(
            keyName = SELECTED_MAX_LEVEL_TRIM,
            name = "Selected max skill trim",
            description = "Name of the selected max skill trim",
            hidden = true
    )
    default String getSelectedMaxLevelTrimFilename()
    {
        return "full-trim.png";
    }

    @ConfigItem(
            keyName = SHOW_MAX_EXPERIENCE_TRIM,
            name = "Show trims on 200m skills?",
            description = "Toggles whether or not show skill trims on 200m skills"
    )
    default boolean getShowMaxExperienceTrim() { return true; }

    @ConfigItem(
            keyName = SELECTED_MAX_EXPERIENCE_TRIM,
            name = "Selected max skill trim",
            description = "Name of the selected max skill trim",
            hidden = true
    )
    default String getSelectedMaxExperienceTrimFilename()
    {
        return "full-trim.png";
    }

    @ConfigItem(
            keyName = TRIM_CONDITIONS,
            name = "Trim conditions",
            description = "The conditions under which to apply trims",
            hidden = true
    )
    default String getTrimConditions()
    {
        return "full-trim.png:" + UserCondition.DefaultCondition;
    }

    @ConfigItem(
            keyName = SHOW_NAV_BUTTON,
            name = "Show navigation button in sidebar?",
            description = "Toggles whether or not show the navigation button (icon) in the Runelite sidebar"
    )
    default boolean getShowNavButton() { return true; }


}
