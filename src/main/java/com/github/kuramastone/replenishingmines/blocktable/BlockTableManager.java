package com.github.kuramastone.replenishingmines.blocktable;

import com.github.kuramastone.replenishingmines.ReplenishingMines;
import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BlockTableManager {

    private Map<String, BlockTableData> blockTables;

    public void load() throws IOException {
        blockTables = new HashMap<>();

        YamlDocument document = YamlDocument.create(
                new File(new File(FabricLoader.getInstance().getConfigDir().toFile(), ReplenishingMines.MODID), "block tables.yml"),
                ReplenishingMines.class.getResourceAsStream("/replenishingmines.configs/block tables.yml")
        );

        for (Object idObj : document.getKeys()) {
            String id = idObj.toString();
            List<Map<String, ?>> rawList = (List<Map<String, ?>>) document.getList(id);
            for (Map<String, ?> rawMap : rawList) {
                Section section = mapToSection(document.createSection(UUID.randomUUID().toString()), rawMap);

                if (section != null) {
                    try {
                        blockTables.computeIfAbsent(id, it -> new BlockTableData(new ArrayList<>())).list().add(BlockTableDataEntry.load(id, section));
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public BlockTableData getTable(String id) {
        return blockTables.get(id);
    }

    public Map<String, BlockTableData> getBlockTables() {
        return blockTables;
    }

    private Section mapToSection(Section parent, Map<?, ?> map) {
        map.forEach((key, value) -> {
            if(value instanceof Map<?, ?> submap) {
                mapToSection(parent.createSection(key.toString()), submap);
            }
            else {
                parent.set(key.toString(), value);
            }
        });

        return parent;
    }

}