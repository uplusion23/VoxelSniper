package com.thevoxelbox.voxelsniper.schematic;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.io.Files;
import com.thevoxelbox.voxelsniper.schematic.exception.InvalidSchematicException;
import com.thevoxelbox.voxelsniper.schematic.exception.MultipleSchematicFoundException;
import com.thevoxelbox.voxelsniper.schematic.exception.NoSchematicFoundException;

import org.bukkit.Bukkit;

public class RessourceManager {

    public static final String SCHEMATIC_FOLDER = "schematics";

    /**
     * List all files in the schematics folder
     * 
     * @return List of files
     */
    public static File[] listSchematics() {
        File dataFolder = Bukkit.getPluginManager().getPlugin("VoxelSniper").getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdir();
        }
        File schematicFolder = new File(dataFolder, SCHEMATIC_FOLDER);
        if (!schematicFolder.exists()) {
            schematicFolder.mkdir();
        }
        return schematicFolder.listFiles();
    }

    public static Schematic loadSchematic(String name)
            throws NoSchematicFoundException, MultipleSchematicFoundException, InvalidSchematicException {
        final boolean hasExtention = name.endsWith(".schematic") || name.endsWith(".schem");

        List<File> schematicCandidate = Arrays.asList(listSchematics()).stream()
                .filter(s -> hasExtention ? s.getName().equals(name)
                        : Files.getNameWithoutExtension(s.getName()).equals(name)
                                && (s.getName().endsWith(".schematic")
                                        || s.getName().endsWith(".schem")))
                .collect(Collectors.toList());

        if (schematicCandidate.isEmpty()) {
            throw new NoSchematicFoundException();
        } else if (schematicCandidate.size() > 1) {
            throw new MultipleSchematicFoundException(
                    schematicCandidate.stream().map(File::getName).collect(Collectors.toList()).toArray(new String[0]));
        } else {
            return Schematic.fromFile(schematicCandidate.get(0));
        }

    }

}
