package com.holybuckets.enchanting.config.model;

import com.holybuckets.enchanting.config.json.DefaultBlockEnchantingStats;
import com.holybuckets.foundation.HBUtil;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Per block type contribution to a table's enchanting stats, held as a flat [stat][operation]
 */
public class BlockEnchantingStats {

    public enum APTH {
        ETRN, QNTA, ARCN, RECT, CLUE;
        public static final APTH[] VALUES = values();

        /** Rectification and clues accumulate only; mult, div, set, floor and ceiling are ignored. */
        public boolean isAdditiveOnly() {
            return this == RECT || this == CLUE;
        }
    }

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
    private final float[] max;

    /** Mutually exclusive blocks: if one is present, the others are ignored. */
    private static final Map<Block, Set<Block>> MUTEXCL_BLOCKS = new HashMap<>();

    public static void onServerStarted() {
        //load mutually exclusive blocks from config, if any
        Set<Block> candles = DefaultBlockEnchantingStats.CANDLES.stream()
            .map(HBUtil.BlockUtil::blockNameToBlock).collect(Collectors.toSet());
        for(Block candle : candles) {
            MUTEXCL_BLOCKS.put(candle, candles);
        }
    }

    public BlockEnchantingStats(Block block, float[][] ops, float[] max) {
        this.block = block;
        this.ops = ops;
        this.max = max;
    }

    public Block getBlock() { return block; }

    public float get(APTH stat, OPS op) {
        return ops[stat.ordinal()][op.ordinal()];
    }

    public float getMaxEterna() { return max[APTH.ETRN.ordinal()]; }
    public float getMaxQuanta() { return max[APTH.QNTA.ordinal()]; }
    public float getMaxArcana() { return max[APTH.ARCN.ordinal()]; }

    public float getMax(APTH stat) {
        return max[stat.ordinal()];
    }

    /** A fresh identity table, ready to be filled in by the json config. */
    public static float[][] identityOps() {
        float[][] ops = new float[APTH.VALUES.length][OPS.VALUES.length];
        for (float[] row : ops) {
            System.arraycopy(IDENTITY, 0, row, 0, IDENTITY.length);
        }
        return ops;
    }


    //** STATICS

    public static boolean isMutualExclBlock(Block block) {
        return MUTEXCL_BLOCKS.containsKey(block);
    }

    public static Set<Block> getMutualExclBlocks(Block block) {
        return MUTEXCL_BLOCKS.getOrDefault(block, Set.of());
    }

}
