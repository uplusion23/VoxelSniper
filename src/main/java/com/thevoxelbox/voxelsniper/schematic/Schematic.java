package com.thevoxelbox.voxelsniper.schematic;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.thevoxelbox.voxelsniper.schematic.adapter.Sponge;
import com.thevoxelbox.voxelsniper.schematic.exception.InvalidSchematicException;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BlockVector;

public class Schematic {

    private String name;

    private short width;
    private short height;
    private short length;
    private Map<Integer, BlockData> blocks;

    private BlockVector origin;

    public Schematic(short width, short height, short length, String name) {
        this.width = width;
        this.height = height;
        this.length = length;
        this.name = name;
        this.blocks = new HashMap<>();
        this.origin = new BlockVector(0, 0, 0);
    }

    public static Schematic fromFile(File file) throws InvalidSchematicException {
        // Only read Sponge format for the moment
        return new Sponge().read(file);
    }

    public void saveToFile(File file) {
        // Only write Sponge format for the moment
        new Sponge().write(file, this);
    }

    public void paste(Location location) {
        paste(location, false);
    }

    public void paste(Location location, boolean ignoreAir) {
        World world = location.getWorld();

        // 3 for loop to copy the blocks
        for (int x = 0; x < this.getWidth(); x++) {
            for (int y = 0; y < this.getHeight(); y++) {
                for (int z = 0; z < this.getLength(); z++) {
                    if (!(ignoreAir && this.getBlockAt(x, y, z).getMaterial().isAir())) {

                        world.getBlockAt(
                                location.getBlockX() + x - this.getOrigin().getBlockX(),
                                location.getBlockY() + y - this.getOrigin().getBlockY(),
                                location.getBlockZ() + z - this.getOrigin().getBlockZ())
                                .setBlockData(this.getBlockAt(x, y, z));
                    }
                }
            }
        }
    }

    public String getName() {
        return name;
    }

    public short getWidth() {
        return width;
    }

    public short getHeight() {
        return height;
    }

    public short getLength() {
        return length;
    }

    public Map<Integer, BlockData> getBlocks() {
        return blocks;
    }

    public BlockData getBlockAt(int x, int y, int z) {
        return blocks.get(x + z * width + y * width * length);
    }

    public void setBlockAt(int x, int y, int z, BlockData block) {
        blocks.put(x + z * width + y * width * length, block);
    }

    public BlockVector getOrigin() {
        return origin;
    }

    public void setOrigin(BlockVector origin) {
        this.origin = origin;
    }

    public static class Constants {

        public static final String SCHEMATIC_TAG = "Schematic";

        public static final String HEIGHT_TAG = "Height";
        public static final String WIDTH_TAG = "Width";
        public static final String LENGTH_TAG = "Length";
        public static final String VERSION_TAG = "Version";
        public static final String DATA_VERSION_TAG = "DataVersion";
        public static final String METADATA_TAG = "Metadata";

        public static final String ORIGIN_X_TAG = "OriginX";
        public static final String ORIGIN_Y_TAG = "OriginY";
        public static final String ORIGIN_Z_TAG = "OriginZ";

        public static class V1 {
            public static final String BLOCKS_TAG = "BlockData";
            public static final String PALETTE_TAG = "Palette";

        }

        public static class V2 {
            public static final String BLOCKS_TAG = "BlockData";
            public static final String PALETTE_TAG = "Palette";

        }

        public static class V3 {

            public static final String BLOCKS_CONTAINER_TAG = "Blocks";
            public static final String BLOCKS_DATA_TAG = "Data";
            public static final String BLOCKS_PALETTE_TAG = "Palette";

        }

    }
}
