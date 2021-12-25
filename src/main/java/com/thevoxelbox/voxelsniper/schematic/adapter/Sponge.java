package com.thevoxelbox.voxelsniper.schematic.adapter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.thevoxelbox.voxelsniper.schematic.Schematic;
import com.thevoxelbox.voxelsniper.schematic.Schematic.Constants;
import com.thevoxelbox.voxelsniper.schematic.exception.InvalidSchematicException;

import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.BlockVector;
import org.jnbt.ByteArrayTag;
import org.jnbt.CompoundTag;
import org.jnbt.IntTag;
import org.jnbt.NBTInputStream;
import org.jnbt.NBTOutputStream;
import org.jnbt.ShortTag;
import org.jnbt.Tag;

public class Sponge implements IAdapter {

    public Schematic read(File file) throws InvalidSchematicException {

        try (NBTInputStream nis = new NBTInputStream(new FileInputStream(file))) {

            Tag rootTag = nis.readTag();

            if (!rootTag.getName().equals(Constants.SCHEMATIC_TAG))
                throw new InvalidSchematicException("Invalid NBT file");

            CompoundTag schematicTag = (CompoundTag) rootTag;

            String[] requiredTags = { Constants.VERSION_TAG, Constants.DATA_VERSION_TAG, Constants.HEIGHT_TAG,
                    Constants.WIDTH_TAG, Constants.LENGTH_TAG };

            for (String tag : requiredTags) {
                if (!schematicTag.getValue().containsKey(tag))
                    throw new InvalidSchematicException("Invalid NBT file, no " + tag + " tag found");
            }

            IntTag versionTag = (IntTag) schematicTag.getValue().get(Constants.VERSION_TAG);

            int schematicVersion = versionTag.getValue();

            if (schematicVersion > 3 || schematicVersion < 1)
                throw new InvalidSchematicException("Invalid NBT file, version " + schematicVersion + " is not supported");

            ShortTag heightTag = (ShortTag) schematicTag.getValue().get(Constants.HEIGHT_TAG);
            ShortTag widthTag = (ShortTag) schematicTag.getValue().get(Constants.WIDTH_TAG);
            ShortTag lengthTag = (ShortTag) schematicTag.getValue().get(Constants.LENGTH_TAG);

            if(heightTag == null || widthTag == null || lengthTag == null)
                throw new InvalidSchematicException("Invalid NBT file, height, width or length tag is null");

            short width = widthTag.getValue();
            short height = heightTag.getValue();
            short length = lengthTag.getValue();

            Schematic schematic = new Schematic(width, height, length, file.getName());


            CompoundTag blockContainer;
            String paletteTagName;
            String blockDataTagName;

            switch (schematicVersion) {
                case 3:
                    blockContainer = (CompoundTag) schematicTag.getValue().get(Constants.V3.BLOCKS_CONTAINER_TAG);
                    paletteTagName = Constants.V3.BLOCKS_PALETTE_TAG;
                    blockDataTagName = Constants.V3.BLOCKS_DATA_TAG;
                    break;
                case 1:
                case 2:
                default:
                    blockContainer = schematicTag;
                    paletteTagName = Constants.V2.PALETTE_TAG;
                    blockDataTagName = Constants.V2.BLOCKS_TAG;
                    break;
            }

            CompoundTag paletteTag = (CompoundTag) blockContainer.getValue().get(paletteTagName);

            if (paletteTag == null)
                throw new InvalidSchematicException("Invalid NBT file, no palette tag found");


                // Reverse the palette
            Map<Integer, String> palette = paletteTag.getValue().entrySet().stream()
                    .collect(HashMap::new, (m, e) -> m.put(((IntTag) e.getValue()).getValue(), e.getKey()), Map::putAll);

            byte[] blocks = (byte[]) blockContainer.getValue().get(blockDataTagName).getValue();


            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    for (int z = 0; z < length; z++) {
                        int index = x + z * width + y * width * length;

                        int blockId = 0;
                        int varintLength = 0;

                        // Read varInt, for more information see : https://wiki.vg/VarInt_And_VarLong
                        do {
                            blockId |= (blocks[index] & 127) << (7 * varintLength);
                            varintLength++;
                            if (varintLength > 5) {
                                throw new InvalidSchematicException("VarInt too long");
                            }
                        } while ((blocks[index] & 128) == 128);

                        String sBlockData = palette.get(blockId);
                        if (sBlockData == null)
                            sBlockData = "minecraft:air";

                        BlockData blockData = Bukkit.createBlockData(sBlockData);
                        schematic.setBlockAt(x, y, z, blockData);

                    }
                }
            }

            // Try to read metadata
            CompoundTag metadataTag = (CompoundTag) schematicTag.getValue().get(Constants.METADATA_TAG);
            if (metadataTag != null) {
                readMetadata(metadataTag, schematic);
            }

            return schematic;
        } catch (IOException e) {
            throw new InvalidSchematicException("Invalid NBT file");
        }
    }


    private void readMetadata(CompoundTag metadataTag, Schematic schematic) {
        Map<String, Tag> metadata = metadataTag.getValue();

        if (metadata.containsKey(Constants.ORIGIN_X_TAG) &&
                metadata.containsKey(Constants.ORIGIN_Y_TAG) &&
                metadata.containsKey(Constants.ORIGIN_Z_TAG)) {
            ShortTag originXTag = (ShortTag) metadata.get(Constants.ORIGIN_X_TAG);
            ShortTag originYTag = (ShortTag) metadata.get(Constants.ORIGIN_Y_TAG);
            ShortTag originZTag = (ShortTag) metadata.get(Constants.ORIGIN_Z_TAG);

            schematic.setOrigin(
                    new BlockVector(
                            originXTag.getValue(),
                            originYTag.getValue(),
                            originZTag.getValue()));
        }
    }

    public void write(File file, Schematic schematic) {

        Map<String, Tag> schematicMap = new HashMap<>();
        schematicMap.put("Width", new ShortTag("Width", schematic.getWidth()));
        schematicMap.put("Height", new ShortTag("Height", schematic.getHeight()));
        schematicMap.put("Length", new ShortTag("Length", schematic.getLength()));

        Map<String, Integer> palette = new HashMap<>();
        int paletteMax = 0;

        ByteArrayOutputStream bos = new ByteArrayOutputStream(
                schematic.getWidth() * schematic.getHeight() * schematic.getLength());

        for (int i = 0; schematic.getBlocks().size() > i; i++) {
            BlockData block = schematic.getBlocks().get(i);
            String state = block.getAsString();

            int blockId = 0;

            if (!palette.containsKey(state)) {
                palette.put(state, paletteMax);
                blockId = paletteMax;
                paletteMax++;
            } else {
                blockId = palette.get(state);
            }

            // Write varInt, for more information see : https://wiki.vg/VarInt_And_VarLong
            while ((blockId & -128) != 0) {
                bos.write((blockId & 127) | 128);
                blockId >>>= 7;
            }
            bos.write(blockId);
        }

        byte[] blockIds = bos.toByteArray();

        Map<String, Tag> blocksContainerMap = new HashMap<>();

        blocksContainerMap.put("Data", new ByteArrayTag("Data", blockIds));

        Map<String, Tag> paletteMap = new HashMap<>();

        for (Entry<String, Integer> entry : palette.entrySet()) {
            paletteMap.put(entry.getKey(), new IntTag(entry.getKey(), entry.getValue()));
        }

        blocksContainerMap.put("Palette", new CompoundTag("Palette", paletteMap));

        schematicMap.put("Blocks", new CompoundTag("Blocks", blocksContainerMap));

        Map<String, Tag> metadataMap = new HashMap<>();

        metadataMap.put(Constants.ORIGIN_X_TAG,
                new ShortTag(Constants.ORIGIN_X_TAG, (short) schematic.getOrigin().getBlockX()));
        metadataMap.put(Constants.ORIGIN_Y_TAG,
                new ShortTag(Constants.ORIGIN_Y_TAG, (short) schematic.getOrigin().getBlockY()));
        metadataMap.put(Constants.ORIGIN_Z_TAG,
                new ShortTag(Constants.ORIGIN_Z_TAG, (short) schematic.getOrigin().getBlockZ()));

        schematicMap.put("Metadata", new CompoundTag("Metadata", metadataMap));
        schematicMap.put("Version", new IntTag("Version", 3));
        schematicMap.put("DataVersion", new IntTag("DataVersion", 2230));

        try (NBTOutputStream nbtos = new NBTOutputStream(new FileOutputStream(file))) {
            nbtos.writeTag(new CompoundTag("Schematic", schematicMap));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
