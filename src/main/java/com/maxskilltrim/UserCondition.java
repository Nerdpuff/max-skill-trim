package com.maxskilltrim;

import net.runelite.api.Client;
import net.runelite.api.Skill;

import javax.swing.*;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.locks.Condition;

public class UserCondition extends JPanel
{
    public static String[] Operators = new String[]{ "=", "<", "<=", ">", ">=", "!=" };
    // 'Base' can only be used on right hand side, so has to be manually typed out
    public static String[] Keys = new String[]{ "Level", "Curr", "xp" };
    public static String DefaultCondition = "Level = 99";


    public String left;
    public String operator;
    public String right;

    public UserCondition(String rawString)
    {
        String[] parts = rawString.split(" ");

        if (parts.length < 3)
            throw  new RuntimeException("blah");

        left = parts[0];
        operator = parts[1];
        right = parts[2];
    }

    public String toString()
    {
        return left + " " + operator + " " + right;
    }

    public boolean Eval(Client client, Skill skill)
    {
        int l = EvalSide(client, skill, left);
        int r = EvalSide(client, skill, right);

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
