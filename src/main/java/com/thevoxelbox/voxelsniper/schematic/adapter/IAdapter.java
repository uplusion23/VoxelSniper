package com.thevoxelbox.voxelsniper.schematic.adapter;

import java.io.File;

import com.thevoxelbox.voxelsniper.schematic.Schematic;
import com.thevoxelbox.voxelsniper.schematic.exception.InvalidSchematicException;

public interface IAdapter {
    

    public Schematic read(File file) throws InvalidSchematicException;

    public void write(File file, Schematic schematic);


}
