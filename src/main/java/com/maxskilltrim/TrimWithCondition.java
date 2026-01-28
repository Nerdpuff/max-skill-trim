package com.maxskilltrim;

import com.formdev.flatlaf.ui.FlatBorder;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.ui.DynamicGridLayout;

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
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        JPopupMenu skillSelectorMenu = new JPopupMenu();
        skillSelectorMenu.setLayout(new DynamicGridLayout(8, 3));
        skillSelectorMenu.setMaximumSize(new Dimension(300, 1000));

        for (Skill skill : Skill.values())
        {
            JCheckBox box = new JCheckBox(skill.getName());
            box.setSelected(condition.appliesTo.contains(skill));
            box.setAlignmentX(LEFT_ALIGNMENT);
            box.addActionListener(new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (box.isSelected())
                        condition.appliesTo.add(skill);
                    else
                        condition.appliesTo.remove(skill);

                    onChange.run();
                }
            });

            skillSelectorMenu.add(box);
        }

        skillSelectorMenu.add(new JMenuItem("Done"));

        setLayout(new BorderLayout());
        setBorder(new CompoundBorder(new EmptyBorder(0,0,1, 0), new LineBorder(Color.darkGray)));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.X_AXIS));
        header.setBorder(new EmptyBorder(0, 1, 0, 1));

        JLabel title = new JLabel(name);
        title.setBorder(new EmptyBorder(1,1,1,1));
        header.add(title, BorderLayout.LINE_START);

        header.add(new SpaceSponge(1000, 0));

        JButton skillSelector = new JButton();
        skillSelector.setText("Skills");
        skillSelector.setHorizontalAlignment(JButton.LEFT);
        skillSelector.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                skillSelectorMenu.show(skillSelector, 0,skillSelector.getHeight());
            }
        });
        skillSelector.setAlignmentX(RIGHT_ALIGNMENT);
        header.add(skillSelector, BorderLayout.LINE_END);

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

        final JPanel self = this;

        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem(new AbstractAction("Delete") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Container parent = self.getParent();
                parent.remove(self);
                onChange.run();
                parent.revalidate();
                parent.repaint();
            }
        });

        menu.add(delete);
        addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) // Right-click
                    menu.show(self, e.getX(), e.getY());
            }

            @Override
            public void mousePressed(MouseEvent e) {}

            @Override
            public void mouseReleased(MouseEvent e) {}

            @Override
            public void mouseEntered(MouseEvent e) {}

            @Override
            public void mouseExited(MouseEvent e) {}
        });
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
