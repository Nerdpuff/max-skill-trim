package com.maxskilltrim;

import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.stream.Collectors;

public class UserCondition extends JPanel
{
    public static String[] Operators = new String[]{ "=", "<", "<=", ">", ">=", "!=" };
    // 'Base' can only be used on right hand side, so has to be manually typed out
    public static String[] Keys = new String[]{ "Level", "Curr", "xp" };
    public static String DefaultCondition = "Level = 99";

    public String left;
    public String operator;
    public String right;

    public List<Skill> appliesTo = new ArrayList<>();

    public UserCondition(String rawString)
    {
        String[] parts = rawString.split(" ");

        if (parts.length < 3)
            throw new IllegalArgumentException("[" + rawString + "] should be at least three parts was " + parts.length);

        left = parts[0];
        operator = parts[1];
        right = parts[2];

        Arrays.stream(parts)
                .skip(3)
                .map(s -> {
                   try
                   {
                       return Skill.valueOf(s);
                   }
                   catch (Exception e)
                   {
                       return null;
                   }
                })
                .filter(Objects::nonNull)
                .forEach(s -> appliesTo.add(s));
    }

    public String toString()
    {
        return left + " " + operator + " " + right + appliesTo.stream().map(s -> " " + s).collect(Collectors.joining());
    }

    public boolean Eval(Client client, Skill skill)
    {
        if (!appliesTo.isEmpty() && !appliesTo.contains(skill))
            return false;

        int l;
        int r;

        try
        {
            l = EvalSide(client, skill, left);
            r = EvalSide(client, skill, right);
        } catch (Exception e) {
            return false;
        }

        switch(operator)
        {
            case "=":  return l == r;
            case "<":  return l < r;
            case "<=": return l <= r;
            case ">":  return l > r;
            case ">=": return l >= r;
            case "!=": return l != r;
        }

        throw new RuntimeException("Invalid operator");
    }

    public int ParseNumber(String value)
    {
        String numberPart = value;
        int multiplier = 1;
        if (value.endsWith("m") || value.endsWith("M"))
        {
            multiplier = 1000000;
            numberPart = value.substring(0, value.length() - 1);
        }
        if (value.endsWith("k") || value.endsWith("K"))
        {
            multiplier = 1000;
            numberPart = value.substring(0, value.length() - 1);
        }

        return Integer.parseInt(numberPart) * multiplier;
    }

    public int EvalSide(Client client, Skill skill, String value)
    {
        switch (value)
        {
            case "Level":
                return client.getRealSkillLevel(skill);
            case "Curr":
                return client.getBoostedSkillLevel(skill);
            case "Base":
                return Arrays.stream(client.getRealSkillLevels()).min().orElse(0);
            case "xp":
                return client.getSkillExperience(skill);
        }
        return ParseNumber(value);
    }
}
