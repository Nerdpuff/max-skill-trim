package com.maxskilltrim;

import javax.swing.*;
import java.awt.*;

public class SpaceSponge extends JComponent
{
    public SpaceSponge(int maxWidth, int maxHeight)
    {
        setMinimumSize(new Dimension(0,0));
        setPreferredSize(new Dimension(maxWidth, maxHeight));
        setMaximumSize(new Dimension(maxWidth, maxHeight));
    }
}
