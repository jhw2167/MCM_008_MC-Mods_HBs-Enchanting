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

    /** Highest enchanting level the given eterna can produce. */
    public static int getMaxLevel(float eterna) {
        return Math.max(0, (int) Math.floor(eterna));
    }

    /** Applies a hard ceiling, e.g. the copper table's maximum power. */
    public static float cap(float eterna, float maxEterna) {
        if (maxEterna <= 0f) return Math.max(eterna, 0f);
        return Math.max(0f, Math.min(eterna, maxEterna));
    }

    /**
     * Level cost for one of the three enchanting rows, using the vanilla curve with eterna
     * standing in for bookshelf power.
     */
    public static int getRowCost(RandomSource random, int row, float eterna, ItemStack stack) {
        return EnchantmentHelper.getEnchantmentCost(random, row, getMaxLevel(eterna), stack);
    }

    /**
     * Fraction of the maximum eterna reached; useful for scaling other stats or rendering a bar.
     */
    public static float getProgress(float eterna) {
        return Math.max(0f, Math.min(1f, eterna / EnchantingStats.MAX_ETERNA));
    }
}
