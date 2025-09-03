package com.github.kuramastone.replenishingmines;

import com.github.kuramastone.replenishingmines.blocktable.BlockTableData;
import com.github.kuramastone.replenishingmines.blocktable.BlockTableReplacement;
import com.github.kuramastone.replenishingmines.region.Region;
import com.github.kuramastone.replenishingmines.utils.PermissionUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.*;

public class RegenMineCommand {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                literal("regenmine")
                        .requires(RegenMineCommand::permissionCheck)
                        .then(literal("create")
                                .then(argument("id", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .then(argument("dimension", DimensionArgumentType.dimension())
                                                .then(argument("blockpos1", BlockPosArgumentType.blockPos())
                                                        .then(argument("blockpos2", BlockPosArgumentType.blockPos())
                                                                .executes(RegenMineCommand::create))
                                                )
                                        )
                                )
                        )
                        .then(literal("delete")
                                .then(argument("id", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .executes(RegenMineCommand::delete)
                                )
                        )
                        .then(literal("modify")
                                .then(argument("id", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .then(literal("brushloot")
                                                .then(argument("lootName", StringArgumentType.string()).suggests(RegenMineCommand::suggestLoot)
                                                        .executes(RegenMineCommand::modifyBrushLoot))
                                        )
                                        .then(literal("replacements")
                                                .then(argument("blockToReplace", BlockStateArgumentType.blockState(commandRegistryAccess))
                                                        .then(argument("blockTable", StringArgumentType.string()).suggests(RegenMineCommand::suggestBlockTables)
                                                                .executes(RegenMineCommand::replaceBlockTable)))
                                        )
                                        .then(literal("regenSpeedInTicks")
                                                .then(argument("regenSpeedInTicks", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                        .executes(RegenMineCommand::modifyRegenSpeedInTicks))
                                        )
                                        .then(literal("regenInstantly")
                                                .then(argument("regenInstantly", StringArgumentType.word()).suggests(RegenMineCommand::suggestBoolean)
                                                        .executes(RegenMineCommand::modifyRegenInstantly))
                                        )
                                        .then(literal("save")
                                                .executes(RegenMineCommand::modifySave)
                                        )
                                )
                        )
                        .then(literal("regen")
                                .then(argument("id", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .executes(RegenMineCommand::regenInstantly)))
                        .then(literal("reload")
                                .executes(RegenMineCommand::reload))
        );
    }

    private static int replaceBlockTable(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "id");
        String blockTable = StringArgumentType.getString(context, "blockTable");
        BlockState blockState = BlockStateArgumentType.getBlockState(context, "blockToReplace").getBlockState();

        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        BlockTableData blockTableData = ReplenishingMines.getApi().getBlockTableManager().getTable(blockTable);
        if (blockTableData == null) {
            context.getSource().sendMessage(Text.literal("Unknown state table! Don't forget to set it up!").formatted(Formatting.RED));
        }
        ReplenishingMines.getApi().getRegion(name).addBlockTableReplacement(new BlockTableReplacement(blockState, blockTable));

        // Your logic to modify the loot of the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' will now replace the specified state state with the '" + blockTable + "' table.").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static boolean permissionCheck(ServerCommandSource source) {
        if (source.hasPermissionLevel(2)) {
            return true;
        }

        if (source.getEntity() == null) {
            return true;
        }

        return PermissionUtils.hasPermission(source.getEntity().getUuid(), "ReplenishingMines.admin");
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        ReplenishingMines.getApi().reload();
        context.getSource().sendMessage(Text.literal("Reloaded!"));
        return Command.SINGLE_SUCCESS;
    }

    private static CompletableFuture<Suggestions> suggestBoolean(CommandContext<ServerCommandSource> serverCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        suggestionsBuilder.suggest("true");
        suggestionsBuilder.suggest("false");
        return suggestionsBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestLoot(CommandContext<ServerCommandSource> serverCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        for (String id : ReplenishingMines.getApi().getLootManager().getLootTables().keySet()) {
            suggestionsBuilder.suggest(id);
        }

        return suggestionsBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestBlockTables(CommandContext<ServerCommandSource> context, SuggestionsBuilder suggestionsBuilder) {
        for (String id : ReplenishingMines.getApi().getBlockTableManager().getBlockTables().keySet()) {
            suggestionsBuilder.suggest(id);
        }

        return suggestionsBuilder.buildFuture();
    }

    private static CompletableFuture<Suggestions> suggestRegions(CommandContext<ServerCommandSource> serverCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        for (String id : ReplenishingMines.getApi().getRegionMap().keySet()) {
            suggestionsBuilder.suggest(id);
        }

        return suggestionsBuilder.buildFuture();
    }

    private static int create(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String id = StringArgumentType.getString(context, "id");
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        BlockPos blockPos1 = BlockPosArgumentType.getBlockPos(context, "blockpos1");
        BlockPos blockPos2 = BlockPosArgumentType.getBlockPos(context, "blockpos2");

        // Your logic to create the regen mine

        Region region = new Region(id, dimension, blockPos1, blockPos2, null, 20 * 60, false);
        region.saveData();

        ReplenishingMines.getApi().registerRegion(id, region);
        context.getSource().sendFeedback(() -> Text.literal("Region '" + id + "' created in dimension '" + dimension.getRegistryKey().getValue().toString() + "' from "
                + "%s/%s/%s".formatted(blockPos1.getX(), blockPos1.getY(), blockPos1.getZ()) + " to "
                + "%s/%s/%s".formatted(blockPos2.getX(), blockPos2.getY(), blockPos2.getZ()) + ".").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "id");


        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        ReplenishingMines.getApi().deleteRegion(name);
        // Your logic to delete the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' deleted."), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int modifyBrushLoot(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "id");
        String lootName = StringArgumentType.getString(context, "lootName");

        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        ReplenishingMines.getApi().getRegion(name).setBrushableBlockLootTable(lootName);

        // Your logic to modify the loot of the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' loot modified to '" + lootName + "'").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int modifyRegenSpeedInTicks(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "id");
        int regenSpeed = IntegerArgumentType.getInteger(context, "regenSpeedInTicks");

        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        ReplenishingMines.getApi().getRegion(name).setRegenSpeedInTicks(regenSpeed);

        // Your logic to modify the loot of the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' regen speed modified to '" + regenSpeed + "'").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int modifyRegenInstantly(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "id");
        String string = StringArgumentType.getString(context, "regenInstantly");

        boolean regenInstantly;
        if (string.equalsIgnoreCase("true"))
            regenInstantly = true;
        else if (string.equalsIgnoreCase("false"))
            regenInstantly = false;
        else {
            context.getSource().sendMessage(Text.literal("Unknown boolean value!").formatted(Formatting.RED));
            return 0;
        }

        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        ReplenishingMines.getApi().getRegion(name).setRegenInstantly(regenInstantly);

        // Your logic to modify the loot of the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' regenInstantly set to '" + regenInstantly + "'").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int modifySave(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "id");

        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        ReplenishingMines.getApi().getRegion(name).saveData();
        ReplenishingMines.getApi().saveRegions();

        // Your logic to modify the loot of the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' has been saved to its current state.").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int regenInstantly(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "id");

        if (ReplenishingMines.getApi().getRegion(name) == null) {
            context.getSource().sendMessage(Text.literal("Unknown region!").formatted(Formatting.RED));
            return 0;
        }

        ReplenishingMines.getApi().getRegion(name).regenImmediately();

        // Your logic to modify the loot of the regen mine
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' will regenerate immediately.").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }
}
