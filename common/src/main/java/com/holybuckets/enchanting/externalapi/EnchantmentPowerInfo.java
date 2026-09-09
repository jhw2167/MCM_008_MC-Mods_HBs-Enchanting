package com.holybuckets.enchanting.externalapi;

import net.minecraft.world.item.enchantment.Enchantment;

/**
 * A loader independent snapshot of everything the enchanting overhaul needs to know about one
 * enchantment. On Forge with Apotheosis installed these values come from
 * config/apotheosis/enchantments.cfg; otherwise they are the vanilla values.
 * <p>
 * Min and max power are captured per level rather than as a function, so the values can be read
 * from common code without touching Apotheosis's PowerFunc.
 */
public class EnchantmentPowerInfo {

    private final Enchantment enchantment;
    private final int maxLevel;
    private final int maxLootLevel;
    private final boolean treasure;
    private final boolean discoverable;
    private final boolean lootable;
    private final boolean tradeable;

    /** Indexed by level; index 0 is unused so getMinPower(1) reads minPower[1]. */
    private final int[] minPower;
    private final int[] maxPower;

    public EnchantmentPowerInfo(Enchantment enchantment, int maxLevel, int maxLootLevel,
                                boolean treasure, boolean discoverable, boolean lootable, boolean tradeable,
                                int[] minPower, int[] maxPower) {
        this.enchantment = enchantment;
        this.maxLevel = maxLevel;
        this.maxLootLevel = maxLootLevel;
        this.treasure = treasure;
        this.discoverable = discoverable;
        this.lootable = lootable;
        this.tradeable = tradeable;
        this.minPower = minPower;
        this.maxPower = maxPower;
    }

    public Enchantment getEnchantment() { return enchantment; }
    public int getMaxLevel() { return maxLevel; }
    public int getMaxLootLevel() { return maxLootLevel; }
    public boolean isTreasure() { return treasure; }
    public boolean isDiscoverable() { return discoverable; }
    public boolean isLootable() { return lootable; }
    public boolean isTradeable() { return tradeable; }

    /** Lowest enchanting power at which this level can appear. */
    public int getMinPower(int level) {
        return read(minPower, level);
    }

    /** Highest enchanting power at which this level can appear. */
    public int getMaxPower(int level) {
        return read(maxPower, level);
    }

    /** True when the given power sits inside this level's window. */
    public boolean isInPowerRange(int level, float power) {
        return power >= getMinPower(level) && power <= getMaxPower(level);
    }

    /** Highest level this enchantment can reach at the given enchanting power; 0 when none. */
    public int getHighestLevelAt(float power) {
        for (int level = maxLevel; level >= 1; level--) {
            if (isInPowerRange(level, power)) return level;
        }
        return 0;
    }

    private static int read(int[] values, int level) {
        if (values.length == 0) return 0;
        int index = Math.max(1, Math.min(level, values.length - 1));
        return values[index];
    }
}
