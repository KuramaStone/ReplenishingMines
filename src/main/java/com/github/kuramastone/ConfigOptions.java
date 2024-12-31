package com.github.kuramastone;

import dev.dejvokep.boostedyaml.YamlDocument;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;

public class ConfigOptions {

    public int instantRegenBlocksPerTick;

    public ConfigOptions() {
        try {
            YamlDocument document = YamlDocument.create(
                    new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), ReplenishingMines.MODID), "config.yml"),
                    getClass().getResourceAsStream("/config.yml")
            );

            instantRegenBlocksPerTick = Math.max(1, document.getInt("Settings.instant regen.blocks per tick"));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
