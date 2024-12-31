package com.github.kuramastone.replenishingmines.loot;

import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.loot.LootPool;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public record LootTableData(List<LootTableItemEntry> items) {

    public ItemStack getLoot(long seed) {
        Random random = new Random(seed);

        int sum = items.stream().flatMapToInt(l-> IntStream.of(l.weight())).sum();
        int rnd = random.nextInt(sum);

        int running = 0;
        for (LootTableItemEntry item : items) {
            if(rnd <= (running += item.weight())) {
                ItemStack itemstack = item.item().getDefaultStack();
                itemstack.setCount(item.amount());
                itemstack.set(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent(item.custommodeldata()));

                return itemstack;
            }
        }

        return ItemStack.EMPTY;
    }

}
