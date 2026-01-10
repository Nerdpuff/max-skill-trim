package com.maxskilltrim;

import com.formdev.flatlaf.ui.FlatBorder;
import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.function.Consumer;

public class TrimWithCondition extends JPanel
{
    Runnable onChange;
    String name;
    boolean enabled;
    UserCondition condition;

    public TrimWithCondition(Runnable onChange, File file)
    {
        this.onChange = onChange;
        this.name = file.getName();
        this.condition = new UserCondition(UserCondition.DefaultCondition);
        this.enabled = false;
        Setup();
    }

    public TrimWithCondition(Runnable onChange, String baked)
    {
        this.onChange = onChange;

        String[] parts = baked.split(":");
        if (parts.length >= 2)
        {
            this.name = parts[0];
            this.condition = new UserCondition(parts[1]);
            this.enabled = parts.length < 3;
        }
        Setup();
    }

    public String Serialize()
    {
        return name + ":" + condition.toString() + (enabled ? "" : ":disabled");
    }

    public String toString()
    {
        return name;
    }

    private void Setup()
    {
        setLayout(new BorderLayout());
        setBorder(new CompoundBorder(new EmptyBorder(0,0,1, 0), new LineBorder(Color.darkGray)));

        JPanel header = new JPanel(new BorderLayout());

        JLabel title = new JLabel(name);
        title.setBorder(new EmptyBorder(1,1,1,1));
        header.add(title, BorderLayout.LINE_START);

        JCheckBox enabledBox = new JCheckBox( "", enabled);
        enabledBox.addActionListener((e) -> { this.enabled = enabledBox.isSelected(); this.onChange.run(); });
        enabledBox.setAlignmentX(LEFT_ALIGNMENT);
        header.add(enabledBox, BorderLayout.LINE_END);

        add(header, BorderLayout.NORTH);

        JLabel icon = new JLabel(new ImageIcon(MaxSkillTrimPlugin.MAXSKILLTRIMS_DIR + "/" + name));
        icon.setBorder(new EmptyBorder(2, 1, 2, 1));
        add(icon, BorderLayout.CENTER);


        JPanel conditionPanel = new JPanel();
        conditionPanel.setLayout(new BoxLayout(conditionPanel, BoxLayout.X_AXIS));
        {
            conditionPanel.add(new JLabel("On: "));
            conditionPanel.add(buildLeftSelector());
            conditionPanel.add(buildOperatorSelector());
            conditionPanel.add(buildRightSelector());
        }

        conditionPanel.setAlignmentX(LEFT_ALIGNMENT);
        add(conditionPanel, BorderLayout.SOUTH);
    }

    private JComboBox<String> buildLeftSelector() {
        JComboBox<String> leftSelector = new JComboBox<>(UserCondition.Keys);

        for (String s : UserCondition.Keys)
        {
            if (s.equals(this.condition.left))
            {
                leftSelector.setSelectedItem(s);
                break;
            }
        }

        leftSelector.addItemListener((s) -> { this.condition.left = (String)s.getItem(); this.onChange.run(); });
        leftSelector.setPreferredSize(new Dimension(65,20));
        leftSelector.setMaximumSize( leftSelector.getPreferredSize() );
        leftSelector.setToolTipText("Level: the current 'real' level\nCurr: the level including boosts/drains\nxp: The xp");
        return leftSelector;
    }

    private JComboBox<String> buildOperatorSelector() {
        JComboBox<String> selector = new JComboBox<>(UserCondition.Operators);

        for (String s : UserCondition.Operators)
        {
            if (s.equals(this.condition.left))
            {
                selector.setSelectedItem(s);
                break;
            }
        }

        selector.setSelectedItem(this.condition.operator);
        selector.addItemListener((s) -> { this.condition.operator = (String)s.getItem(); this.onChange.run(); });
        selector.setPreferredSize(new Dimension(47,20));
        selector.setMaximumSize(selector.getPreferredSize());
        return selector;
    }

    private JComboBox<String> buildRightSelector() {
        JComboBox<String> selector = new JComboBox<>(UserCondition.Keys);
        selector.setEditable(true);

        // Because this box is editable it doesn't technically have to map to the correct object
        selector.setSelectedItem(this.condition.right);

        selector.addItemListener((s) -> { this.condition.right = (String)s.getItem(); this.onChange.run(); });
        selector.setPreferredSize(new Dimension(65,20));
        selector.setMaximumSize(selector.getPreferredSize());

        selector.setToolTipText("You can enter a number, or 'Base' for your current base level");
        return selector;
    }

    boolean IsActive(Client client, Skill skill)
    {
        return condition.Eval(client, skill);
    }
}
