package com.github.kuramastone.replenishingmines.region;

import com.github.kuramastone.ReplenishingMines;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.text.Style;
import net.minecraft.util.dynamic.Codecs;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class RegionData {

    /**
     * BlockPos is the offset from the origin, not the absolute position
     */
    private List<Pair<BlockPos, BlockState>> statesByPosition;

    public RegionData(List<Pair<BlockPos, BlockState>> statesByPosition) {
        this.statesByPosition = statesByPosition;
        sort();
    }

    public RegionData(Section section) {
        this.statesByPosition = new ArrayList<>();
        for (int y : section.getKeys().stream().map(o -> Integer.valueOf(o.toString())).toList()) {
            for (int x : section.getSection("%s".formatted(y)).getKeys().stream().map(o -> Integer.valueOf(o.toString())).toList()) {
                for (int z : section.getSection("%s.%s".formatted(y, x)).getKeys().stream().map(o -> Integer.valueOf(o.toString())).toList()) {
                    BlockPos pos = new BlockPos(x, y, z);
                    String json = section.getString("%s.%s.%s".formatted(y, x, z));
                    BlockState state = deserializeBlockState(json);
                    statesByPosition.add(Pair.of(pos, state));
                }
            }
        }
        sort();
    }

    private void sort() {
        statesByPosition.sort(Comparator
                .comparing((Pair<BlockPos, BlockState> pair) -> pair.getLeft().getY())
                .thenComparing(pair -> pair.getLeft().getX())
                .thenComparing(pair -> pair.getLeft().getZ())
        );
    }

    public void saveTo(Section section) {
        section.clear();
        for (Pair<BlockPos, BlockState> pair : statesByPosition) {
            BlockPos pos = pair.getKey();
            section.set("%s.%s.%s".formatted(pos.getY(), pos.getX(), pos.getZ()), serializeBlockState(pair.getValue()));
        }
    }

    public List<Pair<BlockPos, BlockState>> getStatesByPosition() {
        return statesByPosition;
    }

    public int size() {
        return statesByPosition.size();
    }

    public static String serializeBlockState(BlockState state) {
        // Serialize using the BlockState codec
        JsonElement jsonElement = BlockState.CODEC.encodeStart(JsonOps.INSTANCE, state).getOrThrow();
        return jsonElement.toString(); // Convert to JSON string
    }

    public static BlockState deserializeBlockState(String json) {
        // Parse the JSON string into a JsonElement
        JsonElement jsonElement = JsonParser.parseString(json);

        // Deserialize using the BlockState codec
        return BlockState.CODEC.parse(JsonOps.INSTANCE, jsonElement).getOrThrow();
    }

}
