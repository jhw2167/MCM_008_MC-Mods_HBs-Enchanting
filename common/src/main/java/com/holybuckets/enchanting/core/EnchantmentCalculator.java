package com.holybuckets.enchanting.core;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import com.holybuckets.enchanting.CommonClass;
import com.holybuckets.enchanting.EnchantingMain;
import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.externalapi.EnchantmentPowerInfo;
import com.holybuckets.enchanting.externalapi.IEnchantInfoProvider;
import com.holybuckets.foundation.GeneralConfig;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import static com.google.common.collect.Sets.combinations;

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

        reg.registerOnBeforeServerStarted(Quanta::onServerStart);
        reg.registerOnBeforeServerStarted(Eterna::onServerStart);
    }

    private EnchantmentCalculator() {}

    private static boolean applying = false;
    public static void markApplying(boolean value) {
        applying = value;
    }

    public static boolean consumeApplying() {
        boolean value = applying;
        applying = false;
        return value;
    }


    //Mixin Enchantment Selector
    //cost is 2*eterna value with some randomness. Eterna maxes out at 50, cost 100, max enchant levels can
    //go up to 200
    public static final int ROWS = 3;

    public static List<EnchantmentInstance> select(ItemStack stack, UUID playerId, int slot, int cost,
                                                   float quanta, float arcana, float rectification,
                                                   List<EnchantmentInstance> rolled)
    {
        float eterna = Eterna.getEterna(stack);

        //Only the click that enchants ends the session; the clue passes must not
        boolean apply = consumeApplying();

        Quanta.Key key = new Quanta.Key(playerId, stack.getItem());
        Quanta.Session session = Quanta.SESSIONS.get(key);
        if (session == null) {
            RandomSource random = RandomSource.create(seedOf(key, eterna));
            List<EnchantmentInstance> selected = Eterna.getValidEnchantments(random, stack, cost);
            if (selected.isEmpty()) {
                LoggerProject.logDebug("020002", "getValidEnchantments returned nothing at cost " + cost);
                return Collections.emptyList();
            }
            Quanta.buildOptions(random, key, stack, quanta, eterna, selected);
            session = Quanta.SESSIONS.get(key);
        }

        List<EnchantmentInstance> option = Quanta.getCachedOptions(key, slot);

        if(EnchantingMain.DEV_MODE) {
            LoggerProject.logDebug("020001", String.format(
                "select slot=%d cost=%d apply=%s eterna=%.1f page=%d/%d pool=%d option=%d",
                slot, cost, apply, eterna, Quanta.getPage(key), Quanta.getTotalPages(key),
                session == null ? -1 : session.pool.size(), option.size()));
        }

        if (apply) {
            //The item is enchanted; the session is spent and a later insert starts fresh
            RandomSource random = RandomSource.create(seedOf(key, eterna) + slot);
            Quanta.clear(key);
            return Arcana.apply(random, stack, arcana, option);
        }
        return option;
    }

    /** Stable across repeat calls for the same roll; identity hashes are deliberately avoided. */
    //Stable for the life of a table session. ItemStack has no value based hashCode, so hashing
    //the stack gave a different seed on every insert and a completely different pool each time.
    private static long seedOf(Quanta.Key key, float eterna) {
        long worldSeed = GENERAL_CONFIG.getWorldSeed();
        return worldSeed * 31L + key.hashCode() * 31L + (long)(eterna * 1000);
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

        //Eterna used to price each stack, so later stages can read the table's power
        private static final Map<ItemStack, Float> ETERNA_BY_STACK = new WeakHashMap<>();

        public static float getEterna(ItemStack stack) {
            return ETERNA_BY_STACK.getOrDefault(stack, 0f);
        }

        //Mixin cost hook: normal distribution, mean = eterna * 2, stdDev = mean / 6
        public static int getEnchantmentCost(RandomSource random, int slot, float eterna, ItemStack stack) {
            ETERNA_BY_STACK.put(stack, eterna);

            float mean = eterna * 2f;
            float stdDev = mean / 6f;
            //Seeded from the table state only, so the cost is stable while the item sits there
            long seed = Float.floatToIntBits(eterna) * 31L + slot;
            float gaussian = mean + (float) new Random(seed).nextGaussian() * stdDev;
            return Math.max(1, Math.round(gaussian));
        }

        public static List<EnchantmentInstance> getValidEnchantments(RandomSource random, ItemStack stack, int power) {
            Map<Enchantment, EnchantmentPowerInfo> availableEnchants = ENCHANT_INFO.getAllValidFor(stack);
            List<EnchantmentInstance> valid = new ArrayList<>();

            for (Map.Entry<Enchantment, EnchantmentPowerInfo> entry : availableEnchants.entrySet()) {
                Enchantment enchantment = entry.getKey();
                if(enchantment.isCurse()) continue;
                EnchantmentPowerInfo info = entry.getValue();
                int maxEnchantLevel = info.getMaxLevel();
                int minPower = info.getMinPower(0);
                if(info.getMinPower(0)>power) continue;
                float stdDev = (power - minPower) / 6f;
                float rg = stdDev*3;
                for(int i=1 ; i <= info.getHighestLevelAt(Math.min(power+rg, IEnchantInfoProvider.MAX_POWER)); i++)
                {
                    if(info.getMaxPower(i) < power) continue;
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
            int currHighLevel = info.getHighestLevelAt(curr);
            if(currHighLevel <= 0) return 0;
            double range = info.getMinPower(currHighLevel) - info.getMinPower(currHighLevel-1);
            double stddev = range / 6.0; // 99.7% of values will fall within ±3 standard deviations
            double gaussian = source.nextGaussian() * stddev + mean;
            //Levels start at 1; getMinPower clamps index 0 to level 1, so starting the scan at 0
            //tested level 1 twice and made level 1 unreachable for most enchantments
            for(int i = 1; i <= maxEnchantLevel; i++) {
                if(gaussian < info.getMinPower(i)) return i - 1;
            }
            return maxEnchantLevel;
        }
    }

    public static class Quanta {

        //One open table per player, so the session survives the item leaving and returning
        //Keyed by player and item type. Never pruned by design; the map is small and cheap.
        public static final Map<Key, Session> SESSIONS = new HashMap<>();

        public record Key(UUID playerId, Item item) {}

        public static class Session {
            public final List<Set<EnchantmentInstance>> pool;
            public final AtomicInteger page = new AtomicInteger(0);

            Session(List<Set<EnchantmentInstance>> pool) {
                this.pool = pool;
            }
        }
        public static final float QUANTA_REROLL_COST = 5f;
        public static final int MAX_PERMUTATION_INPUT = 12;
        private static final float enchantsPerEterna = 0.25f; // 10 enchants per 50 eterna
        private static final float enchantsPerQuanta = 0.125f; // 10 enchants per 100 eternaa
        private static final float levelCombinationCount = 0.5f; //an additional enchanting level counts as half a new enchantment
        private static final Map<Enchantment.Rarity, Float> rarityCombinationCounts = new HashMap<>();

        static void onServerStart(ServerStartingEvent event) {
            SESSIONS.clear();
            rarityCombinationCounts.put(Enchantment.Rarity.COMMON, 0.5f);
            rarityCombinationCounts.put(Enchantment.Rarity.UNCOMMON, 1f);
            rarityCombinationCounts.put(Enchantment.Rarity.RARE, 2f);
            rarityCombinationCounts.put(Enchantment.Rarity.VERY_RARE, 4f);
        }

        private Quanta() {}

        //Total number of distinct enchantments permitted per item
        public static final float BASE_QUANTA=3f;
        public static float getMaxCombinationsCount(float quanta, float eterna) {
            float maxByEterna = enchantsPerEterna * eterna;
            float maxByQuanta = enchantsPerQuanta * quanta;
            return Math.max(0f, maxByEterna+maxByQuanta)+BASE_QUANTA;
        }

        //Every non empty subset of the rolled enchantments, largest first.
        //Returns an empty list when the input is empty.
        private static List<Set<EnchantmentInstance>> getCombinations(RandomSource random,
                                                                      List<EnchantmentInstance> enchantments,
                                                                      float maxEnchantsPerCombination)
        {
            List<Set<EnchantmentInstance>> permutations = new ArrayList<>();
            if (enchantments.isEmpty()) return permutations;

            //One randomly chosen level per enchantment, so sharpness appears once at 1, 2 or 3
            List<EnchantmentInstance> shuffled = new ArrayList<>(enchantments);
            Collections.shuffle(shuffled, new Random(random.nextLong()));

            Set<Enchantment> seen = new HashSet<>();
            Set<EnchantmentInstance> set = new HashSet<>();
            for (EnchantmentInstance ei : shuffled) {
                if (seen.add(ei.enchantment)) set.add(ei);
            }

            int maxPerSet = Math.min((int) maxEnchantsPerCombination, set.size());
            Set<Set<EnchantmentInstance>> combinations = new HashSet<>();
            for (int i = 1; i <= maxPerSet; i++) {
                combinations.addAll(combinations(set, i));
            }

            //Groupings light enough to be paired with another later
            List<Set<EnchantmentInstance>> halfPermutations = new ArrayList<>();
            float half = maxEnchantsPerCombination / 2f;

            for (Set<EnchantmentInstance> combination : combinations) {
                float weightedCount = weigh(combination);
                if (weightedCount > maxEnchantsPerCombination) continue;
                if (weightedCount <= half) halfPermutations.add(combination);
                else permutations.add(combination);
            }

            //Pair the lightest grouping with the heaviest that still fits, so merged sets land near the cap
            halfPermutations.sort(Comparator.comparingDouble(EnchantmentCalculator.Quanta::weigh));
            int low = 0;
            int high = halfPermutations.size() - 1;
            while (low < high) {
                Set<EnchantmentInstance> a = halfPermutations.get(low);
                Set<EnchantmentInstance> b = halfPermutations.get(high);

                Set<EnchantmentInstance> merged = new HashSet<>(a);
                merged.addAll(b);
                if (weigh(merged) <= maxEnchantsPerCombination && merged.size() == a.size() + b.size()) {
                    permutations.add(merged);
                }
                low++;
                high--;
            }

            permutations.addAll(halfPermutations);
            return permutations;
        }

        static float weigh(Set<EnchantmentInstance> combination) {
            float weightedCount = 0;
            for (EnchantmentInstance ei : combination) {
                weightedCount += rarityCombinationCounts.getOrDefault(ei.enchantment.getRarity(), 1f);
                weightedCount += ei.level * levelCombinationCount;
            }
            return weightedCount;
        }


        public static void buildOptions(RandomSource random, Key key, ItemStack stack,
                                        float quanta, float eterna, List<EnchantmentInstance> enchantments)
        {
            float count = Math.min(getMaxCombinationsCount(quanta, eterna), enchantments.size());
            List<Set<EnchantmentInstance>> combinations = getCombinations(random, enchantments, count);
            if (combinations.isEmpty()) return;

            Collections.shuffle(combinations, new Random(random.nextLong()));

            int poolSize = Math.min(rerollsAtQuanta(quanta) * ROWS, combinations.size());
            List<Set<EnchantmentInstance>> pool = new ArrayList<>(combinations.subList(0, poolSize));
            SESSIONS.put(key, new Session(pool));
        }

        public static Integer rerollsAtQuanta(float quanta) {
            return ((int) (quanta / QUANTA_REROLL_COST))+1;
        }

        //Roll r shows pool entries [r*ROWS + slot]; past the end the row has no option and is hidden
        public static List<EnchantmentInstance> getCachedOptions(Key key, int slot) {
            Session session = SESSIONS.get(key);
            if (session == null) return Collections.emptyList();

            int idx = session.page.intValue() * ROWS + slot;
            if (idx >= session.pool.size()) return Collections.emptyList();
            return new ArrayList<>(session.pool.get(idx));
        }

        /** One based page for display. */
        public static int getPage(Key key) {
            Session session = SESSIONS.get(key);
            return session == null ? 0 : session.page.intValue() + 1;
        }

        public static int getTotalPages(Key key) {
            Session session = SESSIONS.get(key);
            if (session == null) return 0;
            return (session.pool.size() + ROWS - 1) / ROWS;
        }

        /** Rerolls left after the current page. */
        public static int getRemainingRerolls(Key key) {
            return Math.max(0, getTotalPages(key) - getPage(key));
        }

        //Advances to the next window of ROWS options
        public static void reRoll(Key key) {
            Session session = SESSIONS.get(key);
            if (session == null) return;
            if (session.page.intValue() + 1 < getTotalPages(key)) session.page.getAndIncrement();
        }

        //Taking the item out and putting it back is the reroll. The first insert has no pool yet
        //and select builds one at window zero; every later insert advances the window.
        public static void onInsert(UUID playerId, ItemStack stack) {
            Key key = new Key(playerId, stack.getItem());
            if (SESSIONS.containsKey(key)) reRoll(key);
        }

        public static Key keyOf(UUID playerId, ItemStack stack) {
            return new Key(playerId, stack.getItem());
        }

        public static void clear(Key key) {
            SESSIONS.remove(key);
        }

    }

    //Overlevels enchantments, adds curses
    public static class Arcana {

        //min arcana to start applying curses
        public static final int MIN_ARCANA = 5;

        //steps up arcana overlevel segments, i.e. the arcana required to unlock each segment
        public static final int SEGMENT_INTERVAL = 20;

        // No more than 5 segments of overleveling: 5-25 (1), 25-45 (2), 45-65 (3), 65-85 (4), 85+ (5)
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

        // Chance that the given overlevel segment adds a curse. This is only checked if the segment fires.
        public static float getCurseChance(int segment, float arcana) {
            int threshold = getSegmentThreshold(segment);
            if (segment < 1 || segment > MAX_SEGMENT || arcana < threshold) return 0f;
            return clamp01(BASE_CURSE_CHANCE - CURSE_CHANCE_PER_ARCANA * (arcana - threshold));
        }

        // Average bonus levels a single enchantment gains at the given arcana.
        public static float getExpectedBonusLevels(float arcana) {
            float expected = 0f;
            for (int segment = 1; segment <= getMaxSegment(arcana); segment++) {
                expected += segment * getOverlevelChance(segment, arcana);
            }
            return expected;
        }

        // Average number of curses a single enchantment attracts at the given arcana.
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
        public static List<EnchantmentInstance> apply(RandomSource random, ItemStack stack, float arcana, List<EnchantmentInstance> enchantments)
        {
            List<EnchantmentInstance> result = new ArrayList<>(enchantments.size());
            int curses = 0;

            int i;
            for(i=0; i<enchantments.size(); i++)
            {
                EnchantmentInstance instance = enchantments.get(i);
                int maxLevel = instance.enchantment.getMaxLevel();
                if(instance.level >= maxLevel) {
                    result.add(instance);
                    continue;
                }

                Roll roll = roll(random, arcana);
                int bonusLevels = Math.min(roll.bonusLevels(), maxLevel - instance.level);
                result.add(new EnchantmentInstance(instance.enchantment, instance.level+bonusLevels));
                if(roll.curses() > 1) {
                    List<EnchantmentInstance> cursesList = pickCurses(random, stack, bonusLevels);
                    result.addAll(cursesList);
                    break;
                }
            }
            if(i<enchantments.size()-1)
                result.addAll(enchantments.subList(i+1, enchantments.size()));
            return result;
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
        public static List<EnchantmentInstance> pickCurses(RandomSource random, ItemStack stack, int curseLevel) {
            List<Enchantment> available = getApplicableCurses(stack);
            List<EnchantmentInstance> picked = new ArrayList<>();

            while (!available.isEmpty()) {
                Enchantment enchantment = available.remove(random.nextInt(available.size()));
                int lvl = Math.min(curseLevel, enchantment.getMaxLevel());
                picked.add(new EnchantmentInstance(enchantment, lvl));
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
