package com.holybuckets.enchanting.core;

/**
 * The five enchanting statistics used by the overhaul, mirroring the Apotheosis vocabulary.
 * <p>
 * eterna - maximum level that may be enchanted.
 * quanta - how many alternative enchantment permutations are offered.
 * arcana - overlevels enchantments, at the risk of applying curses.
 * rectification - removes curses that arcana would otherwise apply.
 * clues - how many of the resulting enchantments are revealed before enchanting.
 */
public class EnchantingStats {

    public static final float MAX_ETERNA = 100f;
    public static final float MAX_QUANTA = 100f;
    public static final float MAX_ARCANA = 100f;
    public static final float MAX_RECTIFICATION = 100f;

    private final float eterna;
    private final float quanta;
    private final float arcana;
    private final float rectification;
    private final int clues;

    public EnchantingStats(float eterna, float quanta, float arcana, float rectification, int clues) {
        this.eterna = clamp(eterna, MAX_ETERNA);
        this.quanta = clamp(quanta, MAX_QUANTA);
        this.arcana = clamp(arcana, MAX_ARCANA);
        this.rectification = clamp(rectification, MAX_RECTIFICATION);
        this.clues = Math.max(clues, 0);
    }

    public static EnchantingStats ofEterna(float eterna) {
        return new EnchantingStats(eterna, 0f, 0f, 0f, 0);
    }

    public float getEterna() { return eterna; }
    public float getQuanta() { return quanta; }
    public float getArcana() { return arcana; }
    public float getRectification() { return rectification; }
    public int getClues() { return clues; }

    public EnchantingStats withEterna(float value) {
        return new EnchantingStats(value, quanta, arcana, rectification, clues);
    }

    public EnchantingStats withQuanta(float value) {
        return new EnchantingStats(eterna, value, arcana, rectification, clues);
    }

    public EnchantingStats withArcana(float value) {
        return new EnchantingStats(eterna, quanta, value, rectification, clues);
    }

    public EnchantingStats withRectification(float value) {
        return new EnchantingStats(eterna, quanta, arcana, value, clues);
    }

    public EnchantingStats withClues(int value) {
        return new EnchantingStats(eterna, quanta, arcana, rectification, value);
    }

    /** Sums two stat blocks, e.g. table base stats plus surrounding block contributions. */
    public EnchantingStats plus(EnchantingStats other) {
        return new EnchantingStats(eterna + other.eterna, quanta + other.quanta,
            arcana + other.arcana, rectification + other.rectification, clues + other.clues);
    }

    private static float clamp(float value, float max) {
        if (value < 0f) return 0f;
        return Math.min(value, max);
    }

    @Override
    public String toString() {
        return String.format("EnchantingStats[eterna=%.1f, quanta=%.1f, arcana=%.1f, rectification=%.1f, clues=%d]",
            eterna, quanta, arcana, rectification, clues);
    }
}
