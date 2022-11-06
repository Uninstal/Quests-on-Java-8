package com.leonardobishop.quests.bukkit.util.chat;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexColorAdapter implements ColorAdapter {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    @Override
    public String color(String s) {
        if (s == null) return null;
        Matcher matcher = HEX_PATTERN.matcher(s);
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    @Override
    public String strip(String s) {
        if (s == null) return null;
        return ChatColor.stripColor(s);
    }

}
