package com.holybuckets.enchanting.core;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

/**
 * Arcana overlevels enchantments and, in exchange, risks applying curses.
 * <p>
 * Arcana is divided into "overlevel segments". Segment 1 (+1 level) unlocks at arcana 5; every
 * further segment unlocks at a 20 arcana interval, so +2 at 20, +3 at 40, +4 at 60 and +5 at 80.
 * Each unlocked segment is rolled independently and the successful ones stack, so a high arcana
 * item can pick up +1 and +2 and +3 on the same enchantment.
 * <p>
 * At a segment's unlock point it has a 20% chance to fire and a successful roll has an 80% chance
 * to attach a curse. Every point of arcana above the unlock point adds 1% to the overlevel chance
 * and removes 2% from the curse chance, so a +4 overlevel at arcana 100 fires 60% of the time and
 * is never cursed.
 */
public class ArcanaCalculator {

    /** Arcana at which the first (+1) overlevel segment becomes possible. */
    public static final int MIN_ARCANA = 5;
    /** Arcana between each subsequent overlevel segment. */
    public static final int SEGMENT_INTERVAL = 20;
    /** Highest overlevel segment, i.e. the largest single bonus a segment can grant. */
    public static final int MAX_SEGMENT = 5;

    public static final float BASE_OVERLEVEL_CHANCE = 0.20f;
    public static final float OVERLEVEL_CHANCE_PER_ARCANA = 0.01f;
    public static final float BASE_CURSE_CHANCE = 0.80f;
    public static final float CURSE_CHANCE_PER_ARCANA = 0.02f;

    private ArcanaCalculator() {}

    /** Arcana required before the given overlevel segment can fire. */
    public static int getSegmentThreshold(int segment) {
        if (segment <= 1) return MIN_ARCANA;
        return (segment - 1) * SEGMENT_INTERVAL;
    }

    /** Highest overlevel segment unlocked at the given arcana; 0 when none are. */
    public static int getMaxSegment(float arcana) {
        int max = 0;
        for (int segment = 1; segment <= MAX_SEGMENT; segment++) {
            if (arcana >= getSegmentThreshold(segment)) max = segment;
        }
        return max;
    }

    /** Chance that the given overlevel segment fires. */
    public static float getOverlevelChance(int segment, float arcana) {
        int threshold = getSegmentThreshold(segment);
        if (segment < 1 || segment > MAX_SEGMENT || arcana < threshold) return 0f;
        return clamp01(BASE_OVERLEVEL_CHANCE + OVERLEVEL_CHANCE_PER_ARCANA * (arcana - threshold));
    }

    /** Chance that a successful overlevel of the given segment also applies a curse. */
    public static float getCurseChance(int segment, float arcana) {
        int threshold = getSegmentThreshold(segment);
        if (segment < 1 || segment > MAX_SEGMENT || arcana < threshold) return 0f;
        return clamp01(BASE_CURSE_CHANCE - CURSE_CHANCE_PER_ARCANA * (arcana - threshold));
    }

    /** Average bonus levels a single enchantment gains at the given arcana. */
    public static float getExpectedBonusLevels(float arcana) {
        float expected = 0f;
        for (int segment = 1; segment <= getMaxSegment(arcana); segment++) {
            expected += segment * getOverlevelChance(segment, arcana);
        }
        return expected;
    }

    /** Average number of curses a single enchantment attracts at the given arcana. */
    public static float getExpectedCurses(float arcana) {
        float expected = 0f;
        for (int segment = 1; segment <= getMaxSegment(arcana); segment++) {
            expected += getOverlevelChance(segment, arcana) * getCurseChance(segment, arcana);
        }
        return expected;
    }

    /** Rolls every unlocked segment once for a single enchantment. */
    public static Roll roll(RandomSource random, float arcana) {
        int bonusLevels = 0;
        int curses = 0;

        for (int segment = 1; segment <= getMaxSegment(arcana); segment++) {
            if (random.nextFloat() >= getOverlevelChance(segment, arcana)) continue;
            bonusLevels += segment;
            if (random.nextFloat() < getCurseChance(segment, arcana)) curses++;
        }

        return new Roll(bonusLevels, curses);
    }

    /**
     * Rolls arcana for each enchantment, returning the overleveled results.
     * Curses are accumulated across all enchantments and returned on the result.
     */
    public static Result apply(RandomSource random, float arcana, List<EnchantmentInstance> enchantments) {
        List<EnchantmentInstance> result = new ArrayList<>(enchantments.size());
        int curses = 0;

        for (EnchantmentInstance instance : enchantments) {
            Roll roll = roll(random, arcana);
            curses += roll.curses();
            int level = instance.level + roll.bonusLevels();
            result.add(new EnchantmentInstance(instance.enchantment, Math.max(1, level)));
        }

        return new Result(result, curses);
    }

    /**
     * Rectification removes pending curses; each curse survives with probability
     * (1 - rectification / 100).
     */
    public static int applyRectification(RandomSource random, int curses, float rectification) {
        if (curses <= 0) return 0;
        float removalChance = clamp01(rectification / EnchantingStats.MAX_RECTIFICATION);

        int remaining = 0;
        for (int i = 0; i < curses; i++) {
            if (random.nextFloat() >= removalChance) remaining++;
        }
        return remaining;
    }

    /** Every curse enchantment that may be applied to the given stack. */
    public static List<Enchantment> getApplicableCurses(ItemStack stack) {
        List<Enchantment> curses = new ArrayList<>();
        for (Enchantment enchantment : BuiltInRegistries.ENCHANTMENT) {
            if (!enchantment.isCurse()) continue;
            boolean book = stack.is(Items.BOOK) || stack.is(Items.ENCHANTED_BOOK);
            if (!book && !enchantment.canEnchant(stack)) continue;
            curses.add(enchantment);
        }
        return curses;
    }

    /** Picks distinct curses for the given stack, up to the requested count. */
    public static List<EnchantmentInstance> pickCurses(RandomSource random, ItemStack stack, int count) {
        List<Enchantment> available = getApplicableCurses(stack);
        List<EnchantmentInstance> picked = new ArrayList<>();

        while (!available.isEmpty() && picked.size() < count) {
            Enchantment enchantment = available.remove(random.nextInt(available.size()));
            picked.add(new EnchantmentInstance(enchantment, enchantment.getMaxLevel()));
        }

        return picked;
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    /** Result of rolling arcana for one enchantment. */
    public record Roll(int bonusLevels, int curses) {}

    /** Overleveled enchantments plus the number of curses they earned. */
    public record Result(List<EnchantmentInstance> enchantments, int curses) {}
}
