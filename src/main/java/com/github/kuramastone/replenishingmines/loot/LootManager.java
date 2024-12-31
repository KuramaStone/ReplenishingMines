package com.github.kuramastone.replenishingmines.loot;

import com.github.kuramastone.ReplenishingMines;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LootManager {
    private Map<String, LootTableData> lootTables;

    public  void load() throws IOException {
        lootTables = new HashMap<>();

        YamlDocument document = YamlDocument.create(
                new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), ReplenishingMines.MODID), "loot tables.yml"),
                ReplenishingMines.class.getResourceAsStream("/loot tables.yml")
        );

        for (Object idObj : document.getKeys()) {
            String id = idObj.toString();
            Section section = document.getSection(id);

            List<LootTableItemEntry> list = new ArrayList<>();
            for (Object index : section.getSection("items").getKeys()) {
                Section item = section.getSection("items.%s".formatted(index));
                String itemString = item.getString("item");
                if (!itemString.contains(":")) {
                    itemString = "minecraft:" + itemString;
                }

                Item actualItem = Registries.ITEM.get(Identifier.of(itemString));
                int weight = item.getInt("weight", 1);
                int amount = item.getInt("amount", 1);
                int customModelData = item.getInt("custom model data", 0);
                list.add(new LootTableItemEntry(actualItem, amount, customModelData, weight));
            }

            LootTableData lootTableData = new LootTableData(list);
            lootTables.put(id, lootTableData);
        }

    }

    public LootTableData getLoot(String id) {
        return lootTables.get(id);
    }

    public Map<String, LootTableData> getLootTables() {
        return lootTables;
    }
}