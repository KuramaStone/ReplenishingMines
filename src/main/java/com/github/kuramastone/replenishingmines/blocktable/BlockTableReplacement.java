package com.github.kuramastone.replenishingmines.blocktable;

import com.github.kuramastone.replenishingmines.ReplenishAPI;
import com.github.kuramastone.replenishingmines.ReplenishingMines;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockStateArgument;
import org.jetbrains.annotations.Nullable;

/**
 *
 * @param state BlockState to replace
 * @param blockTable Table to replace it with
 */
public record BlockTableReplacement(BlockState state, String blockTable) {

    public @Nullable BlockTableData getBlockTable() {
        return ReplenishingMines.getApi().getBlockTableManager().getTable(blockTable);
    }

}
