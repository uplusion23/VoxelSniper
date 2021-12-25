package com.thevoxelbox.voxelsniper.brush;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.thevoxelbox.voxelsniper.VoxelMessage;
import com.thevoxelbox.voxelsniper.schematic.RessourceManager;
import com.thevoxelbox.voxelsniper.schematic.Schematic;
import com.thevoxelbox.voxelsniper.schematic.exception.InvalidSchematicException;
import com.thevoxelbox.voxelsniper.schematic.exception.MultipleSchematicFoundException;
import com.thevoxelbox.voxelsniper.schematic.exception.NoSchematicFoundException;
import com.thevoxelbox.voxelsniper.snipe.SnipeData;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.util.BlockVector;

public class StencilBrush extends Brush {

    private Schematic schematic;
    private boolean ignoreAir = true;

    private List<Block> selection;

    public StencilBrush() {
        this.setName("Stencil");
    }

    @Override
    public void info(VoxelMessage vm) {
        vm.brushName(this.getName());
        vm.brushMessage(
                this.schematic != null ? "Loaded Schematic: " + this.schematic.getName() : "No Schematic Loaded");
        vm.brushMessage(ChatColor.BLUE + "Pasting air: " + (this.ignoreAir ? "False" : "True"));
    }

    @Override
    protected final void arrow(final SnipeData v) {

        if (this.selection == null)
            this.selection = new ArrayList<>();

        switch (this.selection.size()) {
            case 0:
                this.selection.add(this.getTargetBlock());
                v.sendMessage("First corner selected");
                v.sendMessage("Now select the opposite corner");
                break;
            case 1:
                this.selection.add(this.getTargetBlock());
                v.sendMessage("Second corner selected");
                v.sendMessage("Now select the origin");
                break;
            case 2:
                this.selection.add(this.getTargetBlock());
                v.sendMessage("Origin selected");
                v.sendMessage("Now type : /b stencil save <schematicName>");
                break;
            default:
                v.sendMessage("Now type : /b stencil save <schematicName>");
                break;
        }
    }

    @Override
    protected final void powder(final SnipeData v) {
        if (this.schematic == null) {
            v.sendMessage(ChatColor.RED + "No Schematic Loaded");
            return;
        }

        this.schematic.paste(this.getTargetBlock().getLocation(), this.ignoreAir);
    }

    @Override
    public final void parseParameters(final String triggerHandle, final String[] params, final SnipeData v) {

        switch (params[0].toLowerCase()) {
            case "unload":
                this.schematic = null;
                v.sendMessage(ChatColor.BLUE + "Schematic unloaded");
                break;
            case "load":
                this.loadSchematic(triggerHandle, params, v);
                break;
            case "save":
                this.saveSchematic(triggerHandle, params, v);
                break;
            case "reset":
                this.selection = new ArrayList<>();
                v.sendMessage(ChatColor.BLUE + "Selection reseted");
                break;
            case "list":
                this.listSchematics(triggerHandle, params, v);
                break;
            case "air":
                this.ignoreAir = !this.ignoreAir;
                v.sendMessage(ChatColor.BLUE + "Pasting air: " + (this.ignoreAir ? "False" : "True"));
                break;
            case "help":
                this.printHelp(triggerHandle, params, v);
                break;
            default:
                v.sendMessage(ChatColor.RED + "Invalid brush parameters! use /b stencil help for more info");
                break;
        }
    }

    @Override
    public List<String> registerArguments() {
        List<String> arguments = new ArrayList<>();

        arguments.add("unload");
        arguments.add("load");
        arguments.add("save");
        arguments.add("reset");
        arguments.add("list");
        arguments.add("air");

        return arguments;
    }

    private void loadSchematic(final String triggerHandle, final String[] params, final SnipeData v) {
        if (params.length < 2) {
            v.sendMessage("Invalid parameters, please supply a schematic name.");
            v.sendMessage("Example: /b " + triggerHandle + " load <schematicName>");
            return;
        }

        try {
            this.schematic = RessourceManager.loadSchematic(params[1]);
        } catch (NoSchematicFoundException e) {
            v.sendMessage(ChatColor.RED + "No Schematic found with the name " + params[1]);
            return;
        } catch (MultipleSchematicFoundException e) {
            v.sendMessage(ChatColor.RED + "Multiple Schematics found with the name " + params[1] + ".");
            v.sendMessage("Please specify the schematic name exactly as it is in the list : ");
            for (String schematicName : e.getSchematics()) {
                v.sendMessage("Example: /b " + triggerHandle + " load " + schematicName);
            }
            return;
        } catch (InvalidSchematicException e) {
            v.sendMessage(ChatColor.RED + "The Schematic " + params[1] + " is invalid.");
            return;
        }

        v.sendMessage(ChatColor.BLUE + "Schematic loaded");
    }

    private void saveSchematic(final String triggerHandle, final String[] params, final SnipeData v) {
        if (params.length < 2) {
            v.sendMessage("Invalid parameters, please supply a schematic name.");
            v.sendMessage("Example: /b " + triggerHandle + " save <schematicName>");
            return;
        }

        if (this.selection.size() < 3) {
            v.sendMessage(ChatColor.RED + "Invalid selection, please select 2 corners and the origin using the arrow");
            v.sendMessage("Example: /b " + triggerHandle + " save <schematicName>");
            return;
        }

        BlockVector firstCorner = this.selection.get(0).getLocation().toVector().toBlockVector();
        BlockVector secondCorner = this.selection.get(1).getLocation().toVector().toBlockVector();
        BlockVector origin = this.selection.get(2).getLocation().toVector().toBlockVector();

        short width = (short) (Math.max(firstCorner.getBlockX(), secondCorner.getBlockX())
                - Math.min(firstCorner.getBlockX(), secondCorner.getBlockX()) + 1);
        short height = (short) (Math.max(firstCorner.getBlockY(), secondCorner.getBlockY())
                - Math.min(firstCorner.getBlockY(), secondCorner.getBlockY()) + 1);
        short length = (short) (Math.max(firstCorner.getBlockZ(), secondCorner.getBlockZ())
                - Math.min(firstCorner.getBlockZ(), secondCorner.getBlockZ()) + 1);

        BlockVector lowCorner = new BlockVector(Math.min(firstCorner.getBlockX(), secondCorner.getBlockX()),
                Math.min(firstCorner.getBlockY(), secondCorner.getBlockY()),
                Math.min(firstCorner.getBlockZ(), secondCorner.getBlockZ()));
        BlockVector hightCorner = new BlockVector(Math.max(firstCorner.getBlockX(), secondCorner.getBlockX()),
                Math.max(firstCorner.getBlockY(), secondCorner.getBlockY()),
                Math.max(firstCorner.getBlockZ(), secondCorner.getBlockZ()));

        World world = this.selection.get(0).getWorld();

        Schematic schematic = new Schematic(width, height, length, params[1]);

        // 3 for loop to copy the blocks
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < length; z++) {
                    schematic.setBlockAt(x, y, z, world
                            .getBlockAt(lowCorner.getBlockX() + x, lowCorner.getBlockY() + y, lowCorner.getBlockZ() + z)
                            .getBlockData());
                }
            }
        }

        schematic.setOrigin(new BlockVector(origin.getBlockX() - lowCorner.getBlockX(),
                origin.getBlockY() - lowCorner.getBlockY(),
                origin.getBlockZ() - lowCorner.getBlockZ()));

        File dataFolder = Bukkit.getPluginManager().getPlugin("VoxelSniper").getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        File schematicFolder = new File(dataFolder, "schematics");
        if (!schematicFolder.exists()) {
            schematicFolder.mkdir();
        }
        File schematicFile = new File(schematicFolder, params[1] + ".schem");

        schematic.saveToFile(schematicFile);
        this.selection = new ArrayList<>();
        v.sendMessage(ChatColor.BLUE + "Schematic saved");

    }

    private void printHelp(final String triggerHandle, final String[] params, final SnipeData v) {
        v.sendMessage(ChatColor.BLUE + "Schematic commands:");
        v.sendMessage(ChatColor.AQUA + "Load a schematic: /b " + triggerHandle + " load <schematicName>");
        v.sendMessage(ChatColor.AQUA + "Unload schematic: /b " + triggerHandle + " unload");
        v.sendMessage(ChatColor.AQUA + "Save a selecton: /b " + triggerHandle + " save <schematicName>");
        v.sendMessage(ChatColor.AQUA + "Reset selection: /b " + triggerHandle + " reset");
        v.sendMessage(ChatColor.AQUA + "List schematics: /b " + triggerHandle + " list");
        v.sendMessage(ChatColor.AQUA + "Toggle air pasting: /b " + triggerHandle + " air");
    }

    private void listSchematics(final String triggerHandle, final String[] params, final SnipeData v) {
        File[] schematicFiles = RessourceManager.listSchematics();

        if (schematicFiles == null) {
            v.sendMessage(ChatColor.RED + "No schematics found");
            return;
        }
        v.sendMessage(ChatColor.BLUE + "Schematics:");
        for (File schematicFile : schematicFiles) {
            v.sendMessage(ChatColor.AQUA + schematicFile.getName());
        }
    }

    @Override
    public String getPermissionNode() {
        return "voxelsniper.brush.stencil";

    }
}
