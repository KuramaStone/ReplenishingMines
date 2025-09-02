package com.github.kuramastone.replenishingmines;

import com.github.kuramastone.replenishingmines.region.Region;
import com.github.kuramastone.replenishingmines.utils.Scheduler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReplenishingMines implements ModInitializer {
    public static final String MODID = "replenishingmines";

    // This logger is used to write text to the console and the log file.
    // It is considered best practice to use your mod id as the logger's name.
    // That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);
    private static MinecraftServer server;
    private static ReplenishAPI api;

    @Override
    public void onInitialize() {
        LOGGER.info("Starting {}", MODID);
        ServerLifecycleEvents.SERVER_STARTED.register(this::onStarted);
        ServerLifecycleEvents.SERVER_STOPPING.register(ms -> api.saveRegions());
        CommandRegistrationCallback.EVENT.register(RegenMineCommand::registerCommands);
        Scheduler.init();
    }

    private void onStarted(MinecraftServer ms) {
        server = ms;
        api = new ReplenishAPI();
        Scheduler.scheduleRepeating(this::tickRegionRegeneration, 1);
    }

    private void tickRegionRegeneration() {
        api.getRegionMap().values().forEach(Region::regenTick);
    }

    public static MinecraftServer getServer() {
        return server;
    }

    public static ReplenishAPI getApi() {
        return api;
    }
}