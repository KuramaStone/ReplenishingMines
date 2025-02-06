package com.github.kuramastone;

import com.github.kuramastone.replenishingmines.loot.LootManager;
import com.github.kuramastone.replenishingmines.region.Region;
import com.github.kuramastone.replenishingmines.region.RegionData;
import dev.dejvokep.boostedyaml.YamlDocument;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReplenishAPI {

    private static final Logger log = LogManager.getLogger(ReplenishAPI.class);
    private Map<String, Region> regionMap;
    private ConfigOptions configOptions;
    private LootManager lootManager;

    public ReplenishAPI() {
        loadConfig();
        loadRegions();
        loadLootTables();
    }

    private void loadLootTables() {
        lootManager = new LootManager();
        try {
            lootManager.load();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadConfig() {
        configOptions = new ConfigOptions();
    }

    public void loadRegions() {
        regionMap = new HashMap<>();
        try {
            YamlDocument document = YamlDocument.create(
                    new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), ReplenishingMines.MODID), "regions.data")
            );

            for (Object areaObj : document.getKeys()) {
                String id = areaObj.toString();
                String worldID = document.getString("%s.world".formatted(id));
                Identifier worldKey = Identifier.of(worldID);
                ServerWorld world = ReplenishingMines.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, worldKey));
                if (world != null) {
                    List<Integer> lowPosition = document.getIntList("%s.low".formatted(id));
                    List<Integer> highPosition = document.getIntList("%s.high".formatted(id));

                    BlockPos low = new BlockPos(lowPosition.get(0), lowPosition.get(1), lowPosition.get(2));
                    BlockPos high = new BlockPos(highPosition.get(0), highPosition.get(1), highPosition.get(2));
                    String lootID = document.getString("%s.lootID".formatted(id));
                    int regenSpeed = document.getInt("%s.regenSpeed".formatted(id));
                    boolean regenInstantly = document.getBoolean("%s.regenInstantly".formatted(id));
                    RegionData data = new RegionData(document.getSection("%s.data".formatted(id)));

                    regionMap.put(id, new Region(world, low, high, lootID, regenSpeed, regenInstantly, data));
                }
            }

        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveRegions() {
        try {
            YamlDocument document = YamlDocument.create(
                    new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), ReplenishingMines.MODID), "regions.data")
            );

            // Clear the current document to rewrite all data
            document.clear();

            // Save all regions
            for (Map.Entry<String, Region> entry : regionMap.entrySet()) {
                String id = entry.getKey();
                Region region = entry.getValue();

                document.set("%s.world".formatted(id), region.getWorld().getRegistryKey().getValue().toString());
                document.set("%s.low".formatted(id), List.of(region.getLow().getX(), region.getLow().getY(), region.getLow().getZ()));
                document.set("%s.high".formatted(id), List.of(region.getHigh().getX(), region.getHigh().getY(), region.getHigh().getZ()));
                document.set("%s.lootID".formatted(id), region.getBrushableBlockLootTable());
                document.set("%s.regenSpeed".formatted(id), region.getRegenSpeedInTicks());
                document.set("%s.regenInstantly".formatted(id), region.shouldRegenInstantly());
                region.getData().saveTo(document.createSection("%s.data".formatted(id)));
            }

            // Save changes to the file
            document.save();
        }
        catch (IOException e) {
            throw new RuntimeException("Failed to save regions to regions.data", e);
        }

    }

    public Region getRegion(String id) {
        return regionMap.get(id);
    }

    public Region registerRegion(String id, Region region) {
        return regionMap.put(id, region);
    }

    public Map<String, Region> getRegionMap() {
        return regionMap;
    }

    public void deleteRegion(String id) {
        regionMap.remove(id);
    }

    public ConfigOptions getConfigOptions() {
        return configOptions;
    }

    public LootManager getLootManager() {
        return lootManager;
    }

    public void reload() {
        loadLootTables();
        loadConfig();
    }
}
