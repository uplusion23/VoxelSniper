package com.thevoxelbox.voxelsniper.schematic.exception;

public class MultipleSchematicFoundException extends Exception {

    String[] schematics;

    public MultipleSchematicFoundException(String[] schematics) {
        this.schematics = schematics;
    }

    public String[] getSchematics() {
        return schematics;
    }

}
