package com.holybuckets.enchanting.core;

import java.util.*;

import com.holybuckets.enchanting.externalapi.EnchantmentPowerInfo;
import com.holybuckets.enchanting.externalapi.IEnchantInfoProvider;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/**
 * Hooks into Apotheosis selectEnchantment to return a set of enchantments
 * based on the current eterna, quanta, and arcana table values:
 *
 * eterna - Enchanting Power, grants access to more enchantments and high level enchantments
 * quanta - Enchantment Permutations, grants access to more permutations of the rolled enchantments
 * arcana - Overlevels enchantments, increases level of enchantments after they are selected, may also add curses
 * rectification - removes curses
 *
 */
public class EnchantmentCalculator {

    static GeneralConfig GENERAL_CONFIG;
    static IEnchantInfoProvider ENCHANT_INFO;
    public static void init(EventRegistrar reg) {
        GENERAL_CONFIG = GeneralConfig.getInstance();
        ENCHANT_INFO = (IEnchantInfoProvider) Balm.platformProxy()
            .withForge("com.holybuckets.enchanting.externalapi.ForgeEnchantInfo")
            .build();
    }

    private EnchantmentCalculator() {}


    //Mixin Enchantment Selector
    //level is eterna which ranges from [0,50], but power goes to 300 so multiply by 4 to get the power range
    public static List<EnchantmentInstance> select(ItemStack stack, int slot, int level,
                                                   float quanta, float arcana, float rectification,
                                                   List<EnchantmentInstance> rolled) {
        RandomSource random = RandomSource.create(seedOf(stack, level));
        List<EnchantmentInstance> selected = Eterna.getValidEnchantments(random, stack, level*4);
        if (selected.isEmpty()) return rolled;
        return selected;
    }

    /** Stable across repeat calls for the same roll; identity hashes are deliberately avoided. */
    private static long seedOf(ItemStack item, int level) {
        long worldSeed = GENERAL_CONFIG.getWorldSeed();
        return item.hashCode() * level * worldSeed;
    }

    //Eterna returns list of valid enchantments
    public static class Eterna
    {
        private static final Map<Enchantment.Rarity, Integer> MAX_LEVELS = new HashMap<>();
        static void onServerStart(ServerStartingEvent event) {
            MAX_LEVELS.put(Enchantment.Rarity.COMMON, 5);
            MAX_LEVELS.put(Enchantment.Rarity.UNCOMMON, 5);
            MAX_LEVELS.put(Enchantment.Rarity.RARE, 3);
            MAX_LEVELS.put(Enchantment.Rarity.VERY_RARE, 1);
        }

        public static List<EnchantmentInstance> getValidEnchantments(RandomSource random, ItemStack stack, int power) {
            Map<Enchantment, EnchantmentPowerInfo> availableEnchants = ENCHANT_INFO.getAllValidFor(stack);
            List<EnchantmentInstance> valid = new ArrayList<>();

            for (Map.Entry<Enchantment, EnchantmentPowerInfo> entry : availableEnchants.entrySet()) {
                Enchantment enchantment = entry.getKey();
                EnchantmentPowerInfo info = entry.getValue();
                int maxEnchantLevel = info.getMaxLevel();
                int minPower = info.getMinPower(0);
                int maxPower = info.getMaxPower(maxEnchantLevel);
                int stdDev = (maxPower - minPower) / 6;
                int rg = stdDev*3;
                for(int i=1 ; i <= info.getHighestLevelAt(Math.min(power+rg, IEnchantInfoProvider.MAX_POWER)); i++)
                {
                    if(info.getMinPower(i) < Math.max(0, power-rg)) continue;
                    int levelRoll = gaussEnchantLevel(random, power, info, info.getMaxLevel());
                    if(levelRoll <=0 ) continue;
                    valid.add(new EnchantmentInstance(enchantment, levelRoll));
                    if(levelRoll >= maxEnchantLevel) break;
                }

            }
            return valid;
        }

         private static int gaussEnchantLevel(RandomSource source, int curr, EnchantmentPowerInfo info, int maxEnchantLevel) {
            double mean = curr;
            double range = info.getMaxPower(maxEnchantLevel) - info.getMinPower(0);
            double step = range / maxEnchantLevel;
            double stddev = range / 6.0; // 99.7% of values will fall within ±3 standard deviations
            double gaussian = source.nextGaussian() * stddev + mean;
            for(int i=0; i < maxEnchantLevel; i++) {
                if(gaussian < info.getMinPower(i)) return i-1;
            }
            return maxEnchantLevel;
        }
    }

    public static class Quanta {

        public static final float QUANTA_REROLL_COST = 5f;

        public static final int MAX_PERMUTATION_INPUT = 12;

        private Quanta() {}

        //Number of enchantment permutations offered at quanta
        public static int getOptionCount(float quanta) {
            return Math.max(1, 1 + (int) Math.floor(Math.max(0f, quanta) / QUANTA_REROLL_COST));
        }

        //Every non empty subset of the rolled enchantments, largest first.
        //Returns an empty list when the input is empty.
        public static List<List<EnchantmentInstance>> getPermutations(List<EnchantmentInstance> enchantments) {
            List<List<EnchantmentInstance>> permutations = new ArrayList<>();
            if (enchantments.isEmpty()) return permutations;

            int size = Math.min(enchantments.size(), MAX_PERMUTATION_INPUT);
            int total = 1 << size;

            for (int mask = 1; mask < total; mask++) {
                List<EnchantmentInstance> subset = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    if ((mask & (1 << i)) != 0) subset.add(enchantments.get(i));
                }
                permutations.add(subset);
            }

            permutations.sort(Comparator.comparingInt((List<EnchantmentInstance> l) -> l.size()).reversed());
            return permutations;
        }

        //The options a player should see: the full rolled set first, then a random selection of the
        //remaining permutations up to the count allowed by quanta.
        public static List<List<EnchantmentInstance>> getOptions(RandomSource random, float quanta,
                                                                List<EnchantmentInstance> enchantments) {
            List<List<EnchantmentInstance>> permutations = getPermutations(enchantments);
            if (permutations.isEmpty()) return permutations;

            int count = Math.min(getOptionCount(quanta), permutations.size());
            List<List<EnchantmentInstance>> options = new ArrayList<>(count);
            options.add(permutations.remove(0));

            while (options.size() < count && !permutations.isEmpty()) {
                options.add(permutations.remove(random.nextInt(permutations.size())));
            }

            return options;
        }
    }

    //Overlevels enchantments, adds curses
    public static class Arcana {

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

        private Arcana() {}

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
}
