package com.maxskilltrim;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.HashTable;
import net.runelite.api.Skill;
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
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class MaxSkillTrimPanel extends PluginPanel
{
    @Inject
    ConfigManager configManager;

    MaxSkillTrimPlugin plugin;

    JPanel conditionsPanel = new JPanel();

    boolean overridesEnabled = false;
    Hashtable<Skill, String> overrides = new Hashtable<>();

    @Inject
    public MaxSkillTrimPanel(MaxSkillTrimConfig config, MaxSkillTrimPlugin plugin, @Named("developerMode") boolean developerMode)
    {
        this.plugin = plugin;
        JPanel container = new JPanel();
        container.setBackground(ColorScheme.DARK_GRAY_COLOR);
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.insets = new Insets(3, 3, 3, 3);
        container.setLayout(layout);

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
            refreshConditions(config.getTrimConditions());
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

        conditionsPanel.setLayout(new BoxLayout(conditionsPanel, BoxLayout.Y_AXIS));

        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        container.add(conditionsPanel, constraints);

        if (developerMode) addDeveloperSection(container);

        add(container);
        refreshConditions(config.getTrimConditions());
    }

    private void addDeveloperSection(JPanel container) {
        JPanel devPanel = new JPanel();
        devPanel.setLayout(new BoxLayout(devPanel, BoxLayout.Y_AXIS));
        devPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        devPanel.setBorder(BorderFactory.createTitledBorder("Developer Tools"));

        JPanel developerPanel = new JPanel(new GridBagLayout());

        GridBagConstraints buttonConstraints = new GridBagConstraints();
        JCheckBox enableOverridesCheckbox = new JCheckBox("Enable Overrides");
        enableOverridesCheckbox.addActionListener(e ->
        {
            this.overridesEnabled = enableOverridesCheckbox.isSelected();
            plugin.updateTrims();
        });
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

            JComboBox<String> overrideSelector = new JComboBox<>();
            skillConstraints.gridx = 1;
            overrideSelector.addActionListener(e ->
            {
                overrides.put(skill.getSkill(), (String)overrideSelector.getSelectedItem());
                plugin.updateTrim(skill.getSkill());
            });

            overrideSelector.addItem(null);

            for (File f : Objects.requireNonNull(MaxSkillTrimPlugin.MAXSKILLTRIMS_DIR.listFiles()))
                overrideSelector.addItem(f.getName());

            developerPanel.add(overrideSelector, skillConstraints);
        }

        JScrollPane scrollPane = new JScrollPane(developerPanel);
        scrollPane.setPreferredSize(new Dimension(220, 400));
        scrollPane.setBorder(null);
        devPanel.add(scrollPane);

        GridBagConstraints devConstraints = new GridBagConstraints();
        devConstraints.gridx = 0;
        devConstraints.gridy = 3;
        devConstraints.gridwidth = GridBagConstraints.REMAINDER;
        devConstraints.fill = GridBagConstraints.BOTH;
        devConstraints.weightx = 1.0;
        devConstraints.weighty = 1.0;
        container.add(devPanel, devConstraints);
    }

    public String selectTrim(Client client, Skill skill)
    {
        if (overridesEnabled)
            return overrides.getOrDefault(skill, null);

        return Arrays.stream(conditionsPanel.getComponents())
                .filter(c -> c.getClass() == TrimWithCondition.class)
                .map(c -> (TrimWithCondition)c)
                .filter(c -> c.enabled)
                .filter(c -> c.IsActive(client, skill))
                .map(c -> c.name)
                .findFirst()
                .orElse(null);
    }

    private void changed()
    {
        saveConditions();
        plugin.updateTrims();
    }

    private void saveConditions()
    {
        if (configManager == null)
            return;

        String baked =  Arrays.stream(conditionsPanel.getComponents())
                        .filter(c -> c.getClass() == TrimWithCondition.class)
                        .map(c -> (TrimWithCondition)c)
                        .map(TrimWithCondition::Serialize)
                        .collect(Collectors.joining(";"));

        configManager.setConfiguration(MaxSkillTrimConfig.GROUP_NAME, MaxSkillTrimConfig.TRIM_CONDITIONS, baked);
    }

    private void refreshConditions(String trimConditionConfig)
    {
        conditionsPanel.removeAll();

        Set<String> oldFiles = Arrays.stream(trimConditionConfig
                        .split(";"))
                        .map(s -> new TrimWithCondition(this::changed, s))
                        .map((saved) -> {
                            conditionsPanel.add(saved);
                            return saved.name;
                        }).collect(Collectors.toSet());

        for (File newFile : Objects.requireNonNull(MaxSkillTrimPlugin.MAXSKILLTRIMS_DIR.listFiles()))
        {
            if (oldFiles.contains(newFile.getName()))
                continue;

            conditionsPanel.add(new TrimWithCondition(this::changed, newFile));
        }

        conditionsPanel.revalidate();
        conditionsPanel.repaint();
    }
}
