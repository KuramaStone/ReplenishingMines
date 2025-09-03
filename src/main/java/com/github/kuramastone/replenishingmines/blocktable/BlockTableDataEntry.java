package com.github.kuramastone.replenishingmines.blocktable;

import com.github.kuramastone.replenishingmines.utils.BlockUtils;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockStateArgument;

public record BlockTableDataEntry(BlockStateArgument state, double weight){

    public static BlockTableDataEntry load(String id, Section section) throws Exception {
        BlockStateArgument state = BlockUtils.parseBlockState(section.getString("block"));
        double weight = section.getDouble("weight");

        return new BlockTableDataEntry(state, weight);
    }

}

