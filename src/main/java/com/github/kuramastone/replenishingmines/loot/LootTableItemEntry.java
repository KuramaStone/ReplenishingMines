package com.github.kuramastone.replenishingmines.loot;

import net.minecraft.item.Item;

import java.util.List;

public record LootTableItemEntry(Item item, int amount, int custommodeldata, int weight) {
}
