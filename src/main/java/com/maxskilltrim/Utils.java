package com.maxskilltrim;

import net.runelite.api.widgets.Widget;

public class Utils {
    /// Detaches a widget from its parent
    static void detachWidget(Widget widget)
    {
        Widget parent = widget.getParent();
        if (parent == null)
            return;

        Widget[] siblings = parent.getChildren();
        if(siblings == null)
            return;

        for (int i = 0; i < siblings.length; i++) {
            if (widget == siblings[i]) {
                siblings[i] = null;
                break;
            }
        }
    }
}
