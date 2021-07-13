package com.leonardobishop.quests.bukkit.command;

import com.leonardobishop.quests.bukkit.BukkitQuestsPlugin;
import com.leonardobishop.quests.bukkit.util.CommandUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class AdminReloadCommandHandler implements CommandHandler {

    private final BukkitQuestsPlugin plugin;

    public AdminReloadCommandHandler(BukkitQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        sender.sendMessage(ChatColor.GRAY + "Please note that some options, such as storage, require a full restart for chances to take effect.");
        plugin.reloadConfig();
        plugin.reloadQuests();
        if (!plugin.getConfigProblems().isEmpty()) CommandUtils.showProblems(sender, plugin.getConfigProblems());
        sender.sendMessage(ChatColor.GREEN + "Quests successfully reloaded.");
    }

    @Override
    public @Nullable String getPermission() {
        return "quests.admin";
    }
}
