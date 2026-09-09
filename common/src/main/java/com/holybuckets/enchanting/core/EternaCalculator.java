package com.holybuckets.enchanting.core;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * Eterna determines the maximum enchanting level available, and therefore the level cost shown
 * in each of the three enchanting rows.
 */
public class EternaCalculator {

    private EternaCalculator() {}

    public static int getMaxLevel(float eterna) {
        return Math.max(0, (int) Math.floor(eterna));
    }


    public static int getRowCost(RandomSource random, int row, float eterna, ItemStack stack) {
        return EnchantmentHelper.getEnchantmentCost(random, row, getMaxLevel(eterna), stack);
    }

    public static float getProgress(float eterna) {
        return Math.max(0f, Math.min(1f, eterna / EnchantingStats.MAX_ETERNA));
    }
}
