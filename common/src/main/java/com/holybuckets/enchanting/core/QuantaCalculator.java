package com.holybuckets.enchanting.core;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Quanta widens the enchantment choice: instead of a single rolled outcome, the permutations of
 * the rolled enchantment set are offered to the player as separate options. Higher quanta surfaces
 * more of those permutations.
 */
public class QuantaCalculator {

    /** Option count is 1 at zero quanta and grows by one per this many quanta. */
    public static final float QUANTA_PER_OPTION = 10f;
    /** Guard against combinatorial explosion; 2^12 subsets is already far more than is usable. */
    public static final int MAX_PERMUTATION_INPUT = 12;

    private QuantaCalculator() {}

    /** How many permutations should be offered at the given quanta. */
    public static int getOptionCount(float quanta) {
        return Math.max(1, 1 + (int) Math.floor(Math.max(0f, quanta) / QUANTA_PER_OPTION));
    }

    /**
     * Every non empty subset of the rolled enchantments, largest first.
     * Returns an empty list when the input is empty.
     */
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

    /**
     * The options a player should see: the full rolled set first, then a random selection of the
     * remaining permutations up to the count allowed by quanta.
     */
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
