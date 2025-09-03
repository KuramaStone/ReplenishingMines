package com.github.kuramastone.replenishingmines.utils;

import com.github.kuramastone.replenishingmines.ReplenishAPI;
import com.github.kuramastone.replenishingmines.ReplenishingMines;
import com.mojang.brigadier.StringReader;
import net.minecraft.block.BlockState;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;

import java.util.Map;
import java.util.stream.Collectors;

public class BlockUtils {

    public static BlockStateArgument parseBlockState(String input) throws Exception {
        StringReader reader = new StringReader(input);
        BlockStateArgument arg = BlockStateArgumentType.blockState(CommandManager.createRegistryAccess(ReplenishingMines.getServer().getRegistryManager())).parse(reader);

        // Base state state
        return arg;
    }

    public static String blockStateToString(BlockState state) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        StringBuilder sb = new StringBuilder(id.toString());

        if (!state.getEntries().isEmpty()) {
            sb.append("[");
            sb.append(state.getEntries().entrySet().stream()
                    .map(BlockUtils::formatEntry)
                    .collect(Collectors.joining(",")));
            sb.append("]");
        }

        return sb.toString();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String formatEntry(Map.Entry<Property<?>, Comparable<?>> entry) {
        Property property = entry.getKey();
        Comparable value = entry.getValue();
        return property.getName() + "=" + property.name(value);
    }

}
