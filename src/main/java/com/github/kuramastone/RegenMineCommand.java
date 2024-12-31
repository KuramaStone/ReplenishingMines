package com.github.kuramastone;

import com.github.kuramastone.replenishingmines.region.Region;
import com.github.kuramastone.replenishingmines.region.RegionData;
import com.github.kuramastone.utils.LuckPermsUtils;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.command.argument.RegistryEntryArgumentType;
import net.minecraft.loot.LootTable;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.concurrent.CompletableFuture;

public class RegenMineCommand {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess commandRegistryAccess, CommandManager.RegistrationEnvironment registrationEnvironment) {
        dispatcher.register(
                CommandManager.literal("regenmine")
                        .requires(RegenMineCommand::permissionCheck)
                        .then(CommandManager.literal("create")
                                .then(CommandManager.argument("name", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .then(CommandManager.argument("dimension", DimensionArgumentType.dimension())
                                                .then(CommandManager.argument("blockpos1", BlockPosArgumentType.blockPos())
                                                        .then(CommandManager.argument("blockpos2", BlockPosArgumentType.blockPos())
                                                                .executes(RegenMineCommand::create))
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("name", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .executes(RegenMineCommand::delete)
                                )
                        )
                        .then(CommandManager.literal("modify")
                                .then(CommandManager.argument("name", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .then(CommandManager.literal("brushloot")
                                                .then(CommandManager.argument("lootName", StringArgumentType.string())
                                                        .executes(RegenMineCommand::modifyBrushLoot))
                                        )
                                        .then(CommandManager.literal("regenSpeedInTicks")
                                                .then(CommandManager.argument("regenSpeedInTicks", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                                        .executes(RegenMineCommand::modifyRegenSpeedInTicks))
                                        )
                                        .then(CommandManager.literal("regenInstantly")
                                                .then(CommandManager.argument("regenInstantly", StringArgumentType.word()).suggests(RegenMineCommand::suggestBoolean)
                                                        .executes(RegenMineCommand::modifyRegenInstantly))
                                        )
                                        .then(CommandManager.literal("save")
                                                .executes(RegenMineCommand::modifySave)
                                        )
                                )
                        )
                        .then(CommandManager.literal("regen")
                                .then(CommandManager.argument("name", StringArgumentType.string()).suggests(RegenMineCommand::suggestRegions)
                                        .executes(RegenMineCommand::regenInstantly)))
                        .then(CommandManager.literal("reload")
                                .executes(RegenMineCommand::reload))
        );
    }

    private static boolean permissionCheck(ServerCommandSource source) {
        if(source.hasPermissionLevel(2)) {
            return true;
        }

        if(source.getEntity() == null) {
            return true;
        }

        return LuckPermsUtils.hasPermission(source.getEntity().getUuidAsString(), "ReplenishingMines.admin");
    }

    private static int reload(CommandContext<ServerCommandSource> context) {
        ReplenishingMines.getApi().reload();
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

    private static CompletableFuture<Suggestions> suggestRegions(CommandContext<ServerCommandSource> serverCommandSourceCommandContext, SuggestionsBuilder suggestionsBuilder) {
        for (String id : ReplenishingMines.getApi().getRegionMap().keySet()) {
            suggestionsBuilder.suggest(id);
        }

        return suggestionsBuilder.buildFuture();
    }

    private static int create(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        String name = StringArgumentType.getString(context, "name");
        ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
        BlockPos blockPos1 = BlockPosArgumentType.getBlockPos(context, "blockpos1");
        BlockPos blockPos2 = BlockPosArgumentType.getBlockPos(context, "blockpos2");

        // Your logic to create the regen mine

        Region region = new Region(dimension, blockPos1, blockPos2, null, 20 * 60, false);
        region.saveData();

        ReplenishingMines.getApi().registerRegion(name, region);
        context.getSource().sendFeedback(() -> Text.literal("Region '" + name + "' created in dimension '" + dimension + "' from " + blockPos1 + " to " + blockPos2 + ".").formatted(Formatting.GREEN), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int delete(CommandContext<ServerCommandSource> context) {
        String name = StringArgumentType.getString(context, "name");


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
        String name = StringArgumentType.getString(context, "name");
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
        String name = StringArgumentType.getString(context, "name");
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
        String name = StringArgumentType.getString(context, "name");
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
        String name = StringArgumentType.getString(context, "name");

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
        String name = StringArgumentType.getString(context, "name");

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
