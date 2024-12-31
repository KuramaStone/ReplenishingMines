package com.github.kuramastone.utils;

import net.minecraft.block.entity.BrushableBlockEntity;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;

public class BrushableBlockEntityUtils {

    private static Field itemField;
    private static Field lootTableSeedField;

    static {
        try {
            // try standard yarn
            itemField = BrushableBlockEntity.class.getDeclaredField("item");
            itemField.setAccessible(true);

            lootTableSeedField = BrushableBlockEntity.class.getDeclaredField("lootTableSeed");
            lootTableSeedField.setAccessible(true);
        }
        catch (NoSuchFieldException e) {
            try {
                // try obfuscated mappings
                itemField = BrushableBlockEntity.class.getDeclaredField("field_42812");
                itemField.setAccessible(true);

                lootTableSeedField = BrushableBlockEntity.class.getDeclaredField("field_42815");
                lootTableSeedField.setAccessible(true);
            }
            catch (NoSuchFieldException ex) {
                throw new RuntimeException(ex);
            }
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
