package com.holybuckets.enchanting.config.model;

import net.minecraft.world.level.block.Block;

import java.util.Collection;

/**
 * Per block type contribution to a table's enchanting stats, held as a flat [stat][operation]
 * table so every calculation is a loop rather than a chain of field tests.
 * <p>
 * Each operation's unset value is its own identity, so an unconfigured slot is a no-op with no
 * branch: ADD and SUB are 0, MULT and DIV are 1, SET is NaN, FLR is -infinity, CEIL is +infinity.
 */
public class BlockEnchantingStats {

    /** The three Apotheosis stats, in array order. */
    public enum APTH {
        ETRN, QNTA, ARCN;

        public static final APTH[] VALUES = values();
    }

    /** Operations applied in declaration order. */
    public enum OPS {
        ADD, SUB, MULT, DIV, SET, FLR, CEIL;

        public static final OPS[] VALUES = values();
    }

    public static final float[] IDENTITY = {
        0f,                             //ADD
        0f,                             //SUB
        1f,                             //MULT
        1f,                             //DIV
        Float.NaN,                      //SET
        Float.NEGATIVE_INFINITY,        //FLR
        Float.POSITIVE_INFINITY         //CEIL
    };

    private final Block block;
    private final float[][] ops;

    /** Ceiling on what this block type may contribute through ADD, per stat. */
    private final float maxEterna;
    private final float maxQuanta;
    private final float maxArcana;

    public BlockEnchantingStats(Block block, float[][] ops, float maxEterna, float maxQuanta, float maxArcana) {
        this.block = block;
        this.ops = ops;
        this.maxEterna = maxEterna;
        this.maxQuanta = maxQuanta;
        this.maxArcana = maxArcana;
    }

    public Block getBlock() { return block; }

    public float get(APTH stat, OPS op) {
        return ops[stat.ordinal()][op.ordinal()];
    }

    public float getMaxEterna() { return maxEterna; }
    public float getMaxQuanta() { return maxQuanta; }
    public float getMaxArcana() { return maxArcana; }

    public float getMax(APTH stat) {
        return switch (stat) {
            case ETRN -> maxEterna;
            case QNTA -> maxQuanta;
            case ARCN -> maxArcana;
        };
    }

    /** A fresh identity table, ready to be filled in by the json config. */
    public static float[][] identityOps() {
        float[][] ops = new float[APTH.VALUES.length][OPS.VALUES.length];
        for (float[] row : ops) {
            System.arraycopy(IDENTITY, 0, row, 0, IDENTITY.length);
        }
        return ops;
    }

    /**
     * Applies every contributing block type to the given base totals, one stat at a time and one
     * operation at a time, in OPS declaration order.
     *
     * @param totals base eterna, quanta and arcana, indexed by APTH; modified in place
     */
    public static void apply(Collection<Contribution> contributions, float[] totals) {
        for (APTH stat : APTH.VALUES) {
            int s = stat.ordinal();

            for (Contribution c : contributions) {
                float added = c.stats.get(stat, OPS.ADD) * c.count;
                float max = c.stats.getMax(stat);
                totals[s] += added >= 0 ? Math.min(added, max) : Math.max(added, -max);
            }

            for (Contribution c : contributions) {
                totals[s] -= c.stats.get(stat, OPS.SUB) * c.count;
            }

            for (Contribution c : contributions) {
                totals[s] *= (float) Math.pow(c.stats.get(stat, OPS.MULT), c.count);
            }

            for (Contribution c : contributions) {
                totals[s] *= (float) Math.pow(c.stats.get(stat, OPS.DIV), c.count);
            }

            for (Contribution c : contributions) {
                float set = c.stats.get(stat, OPS.SET);
                totals[s] = Float.isNaN(set) ? totals[s] : set;
            }

            for (Contribution c : contributions) {
                totals[s] = Math.max(totals[s], c.stats.get(stat, OPS.FLR));
            }

            for (Contribution c : contributions) {
                totals[s] = Math.min(totals[s], c.stats.get(stat, OPS.CEIL));
            }

            totals[s] = Math.max(totals[s], 0f);
        }
    }

    /** One block type and how many of it are in range. */
    public static class Contribution {
        public final BlockEnchantingStats stats;
        public final int count;

        public Contribution(BlockEnchantingStats stats, int count) {
            this.stats = stats;
            this.count = count;
        }
    }
}
