package com.thevoxelbox.voxelsniper.command;

import com.thevoxelbox.voxelsniper.VoxelProfileManager;
import com.thevoxelbox.voxelsniper.snipe.Sniper;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class VoxelDefaultCommand extends VoxelCommand {

    public VoxelDefaultCommand() {
        super("VoxelDefault");
        setIdentifier("d");
        setPermission("voxelsniper.sniper");
    }

    @Override
    public boolean doCommand(Player player, String[] args) {
        Sniper sniper = VoxelProfileManager.getInstance().getSniperForPlayer(player);

        // Default command
        // Command: /d info, /d help
        if (args.length == 1 && (args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("info"))) {
            player.sendMessage(ChatColor.DARK_AQUA + "VoxelSniper 6.3.0 [1.18.1]");
            player.sendMessage(ChatColor.DARK_AQUA + "- Updated By: " + ChatColor.YELLOW + "uplusion23 -");
            player.sendMessage(ChatColor.DARK_AQUA + getName() + " Command Syntax:");
            player.sendMessage(ChatColor.GOLD + "/" + getActiveAlias());
            player.sendMessage(ChatColor.YELLOW + "    Resets tool to default values.");
            return true;
        }

        if (args.length == 0) {
            sniper.reset(sniper.getCurrentToolId());
            player.sendMessage(ChatColor.AQUA + "Brush settings reset to their default values.");
            return true;
        }
        
        return false;
    }

    @Override
    public List<String> doSuggestion(Player player, String[] args) {
        return new ArrayList<>();
    }
}
