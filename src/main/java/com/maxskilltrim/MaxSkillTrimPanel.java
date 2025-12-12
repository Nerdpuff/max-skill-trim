package com.maxskilltrim;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.LinkBrowser;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.Objects;

@Slf4j
@Singleton
public class MaxSkillTrimPanel extends PluginPanel
{
    @Inject
    ConfigManager configManager;

    @Inject
    public MaxSkillTrimPanel(MaxSkillTrimConfig config, MaxSkillTrimPlugin plugin, @Named("developerMode") boolean developerMode)
    {
        JPanel container = new JPanel();
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(3, 3, 3, 3);
        container.setLayout(layout);

        JComboBox<String> maxLevelComboBox = buildComboBoxPanel(MaxSkillTrimConfig.SELECTED_MAX_LEVEL_TRIM, config.getSelectedMaxLevelTrimFilename());
        JComboBox<String> maxExperienceComboBox = buildComboBoxPanel(MaxSkillTrimConfig.SELECTED_MAX_EXPERIENCE_TRIM, config.getSelectedMaxExperienceTrimFilename());

        JButton openFolderButton = new JButton("Open Folder");
        openFolderButton.addActionListener(e ->
        {
            try
            {
                Desktop.getDesktop().open(MaxSkillTrimPlugin.MAXSKILLTRIMS_DIR);
            }
            catch (Exception ex)
            {
                log.warn(null, ex);
            }
        });
        constraints.gridx = 0;
        constraints.gridy = 0;
        container.add(openFolderButton, constraints);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener((ev) -> {
            refreshComboBoxOptions(maxLevelComboBox);
            refreshComboBoxOptions(maxExperienceComboBox);
        });
        constraints.gridx = 1;
        constraints.gridy = 0;
        container.add(refreshButton, constraints);

        JButton getMoreTrimsButton = new JButton("Get more trims!");
        getMoreTrimsButton.addActionListener((e) -> LinkBrowser.browse("https://github.com/Nerdpuff/max-skill-trim/tree/custom-trims"));
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(getMoreTrimsButton, constraints);

        JLabel maxLevelTrimLabel = new JLabel("Max level trim");
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(maxLevelTrimLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(maxLevelComboBox, constraints);

        JLabel maxExperienceTrimLabel = new JLabel("Max experience trim");
        constraints.gridx = 0;
        constraints.gridy = 4;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(maxExperienceTrimLabel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 5;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(maxExperienceComboBox, constraints);

        if (developerMode) addDeveloperSection(plugin, container);

        add(container);
    }

    private static void addDeveloperSection(MaxSkillTrimPlugin plugin, JPanel container) {
        JPanel devPanel = new JPanel();
        devPanel.setLayout(new BoxLayout(devPanel, BoxLayout.Y_AXIS));
        devPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        devPanel.setBorder(BorderFactory.createTitledBorder("Developer Tools"));

        JPanel developerPanel = new JPanel(new GridBagLayout());

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        JCheckBox enableOverridesCheckbox = new JCheckBox("Enable Overrides");
        enableOverridesCheckbox.addActionListener(e -> plugin.setMockOverridesEnabled(enableOverridesCheckbox.isSelected()));
        buttonConstraints.gridx = 0;
        buttonConstraints.gridy = 0;
        buttonConstraints.gridwidth = GridBagConstraints.REMAINDER;
        buttonConstraints.anchor = GridBagConstraints.LINE_START;
        developerPanel.add(enableOverridesCheckbox, buttonConstraints);

        GridBagConstraints skillConstraints = new GridBagConstraints();
        skillConstraints.insets = new Insets(2, 2, 2, 2);
        skillConstraints.anchor = GridBagConstraints.LINE_START;
        skillConstraints.fill = GridBagConstraints.HORIZONTAL;

        SkillData[] skills = SkillData.values();
        for (int i = 0; i < skills.length; i++) {
            SkillData skill = skills[i];
            skillConstraints.gridx = 0;
            skillConstraints.gridy = i + 1;
            developerPanel.add(new JLabel(skill.name()), skillConstraints);

            // Max Level checkbox
            skillConstraints.gridx = 1;
            JCheckBox maxLevelToggle = new JCheckBox("ML");
            maxLevelToggle.addActionListener(e -> plugin.setMockTrimState(skill, maxLevelToggle.isSelected(), TrimType.MAX_LEVEL));
            developerPanel.add(maxLevelToggle, skillConstraints);

            // Max Experience checkbox
            skillConstraints.gridx = 2;
            JCheckBox maxExpToggle = new JCheckBox("ME");
            maxExpToggle.addActionListener(e -> plugin.setMockTrimState(skill, maxExpToggle.isSelected(), TrimType.MAX_EXPERIENCE));
            developerPanel.add(maxExpToggle, skillConstraints);
        }

        JScrollPane scrollPane = new JScrollPane(developerPanel);
        scrollPane.setPreferredSize(new Dimension(220, 400));
        scrollPane.setBorder(null);
        devPanel.add(scrollPane);

        GridBagConstraints devConstraints = new GridBagConstraints();
        devConstraints.gridx = 0;
        devConstraints.gridy = 6;
        devConstraints.gridwidth = GridBagConstraints.REMAINDER;
        devConstraints.fill = GridBagConstraints.BOTH;
        devConstraints.weightx = 1.0;
        devConstraints.weighty = 1.0;
        container.add(devPanel, devConstraints);
    }

    private JComboBox<String> buildComboBoxPanel(String selectedTrimConfigKey, String selectedFilename) {
        JComboBox<String> comboBox = new JComboBox<>();

        // Forces long item names to not cause the JPanel to overflow.
        comboBox.setPrototypeDisplayValue("");

        refreshComboBoxOptions(comboBox);

        comboBox.addItemListener((e) -> {
            if (e.getStateChange() == ItemEvent.SELECTED && configManager != null)
            {
                configManager.setConfiguration(MaxSkillTrimConfig.GROUP_NAME, selectedTrimConfigKey, e.getItem());
            }
        });

        comboBox.setSelectedItem(selectedFilename);

        return comboBox;
    }

    private void refreshComboBoxOptions(JComboBox<String> comboBox)
    {
        Object selectedItem = comboBox.getSelectedItem();
        comboBox.removeAllItems();

        for (File f : Objects.requireNonNull(MaxSkillTrimPlugin.MAXSKILLTRIMS_DIR.listFiles()))
        {
            comboBox.addItem(f.getName());
        }

        comboBox.setSelectedItem(selectedItem);
    }
}
