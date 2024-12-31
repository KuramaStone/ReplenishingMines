package com.github.kuramastone.utils;

import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;

public class BrushableBlockEntityUtils {

    private static final Field itemField;
    private static final Field lootTableSeedField;

    static {
        try {
            itemField = BrushableBlockEntity.class.getDeclaredField("item");
            itemField.setAccessible(true);

            lootTableSeedField = BrushableBlockEntity.class.getDeclaredField("lootTableSeed");
            lootTableSeedField.setAccessible(true);
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setItem(BrushableBlockEntity brushableBlockEntity, ItemStack item) {
        try {
            itemField.set(brushableBlockEntity, item);
        }
        catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

    }

    public static void setSeed(BrushableBlockEntity blockEntity, long seed) {
    }
}
