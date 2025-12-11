package com.maxskilltrim;

import net.runelite.api.widgets.Widget;

public class MaxSkillTrimWidget {
    SkillData skillData;
    TrimType trimType;
    boolean isMockEnabled;
    Widget widget;

    MaxSkillTrimWidget(SkillData skillData, TrimType trimType, boolean isMockEnabled, Widget widget) {
        this.skillData = skillData;
        this.trimType = trimType;
        this.isMockEnabled = isMockEnabled;
        this.widget = widget;
    }
}
