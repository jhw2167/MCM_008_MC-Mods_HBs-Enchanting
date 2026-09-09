package com.holybuckets.enchanting.config.json;

import com.google.gson.JsonObject;
import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.OPS;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;

/**
 * Json shape for one blockEnchantingStats entry: owns the property names, the parsing, and the
 * registry lookup that turns a block name into a live Block.
 * <p>
 * Property names are built from the operation and the stat, so ADD on ETRN is "addEterna" and
 * CEIL on ARCN is "ceilArcana". Only values that differ from the operation's identity are written
 * back out, which keeps the generated file small.
 */
public class BlockEnchantingStatsJsonConfig {

    private static final String CLASS_ID = "017";

    public static final String BLOCK_NAME_KEY = "blockName";

    private static final String[] OP_PREFIX = { "add", "sub", "mult", "div", "set", "flr", "ceil" };
    private static final String[] STAT_SUFFIX = { "Eterna", "Quanta", "Arcana" };
    private static final String[] MAX_KEY = { "maxEterna", "maxQuanta", "maxArcana" };

    private final String blockName;
    private final float[][] ops;
    private final float[] max;

    public BlockEnchantingStatsJsonConfig(String blockName) {
        this.blockName = blockName == null ? "" : blockName;
        this.ops = BlockEnchantingStats.identityOps();
        this.max = new float[] {
            Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY };
    }

    public String getBlockName() {
        return blockName;
    }

    public BlockEnchantingStatsJsonConfig set(APTH stat, OPS op, float value) {
        ops[stat.ordinal()][op.ordinal()] = value;
        return this;
    }

    public BlockEnchantingStatsJsonConfig max(APTH stat, float value) {
        max[stat.ordinal()] = value;
        return this;
    }

    public static String key(OPS op, APTH stat) {
        return OP_PREFIX[op.ordinal()] + STAT_SUFFIX[stat.ordinal()];
    }

    public static String maxKey(APTH stat) {
        return MAX_KEY[stat.ordinal()];
    }

    /**
     * Resolves the configured block name against the block registry.
     *
     * @return the usable stats, or null when the block is not registered
     */
    @Nullable
    public BlockEnchantingStats resolve() {
        if (blockName.isEmpty()) {
            LoggerProject.logError(CLASS_ID + "001", "blockEnchantingStats entry is missing a blockName; skipping");
            return null;
        }

        ResourceLocation id = ResourceLocation.tryParse(blockName);
        Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
        if (block == null) {
            LoggerProject.logError(CLASS_ID + "002",
                "blockEnchantingStats references a block not in the registry: " + blockName + "; skipping");
            return null;
        }

        return new BlockEnchantingStats(block, ops,
            max[APTH.ETRN.ordinal()], max[APTH.QNTA.ordinal()], max[APTH.ARCN.ordinal()]);
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty(BLOCK_NAME_KEY, blockName);

        for (OPS op : OPS.VALUES) {
            for (APTH stat : APTH.VALUES) {
                float value = ops[stat.ordinal()][op.ordinal()];
                if (isIdentity(op, value)) continue;
                obj.addProperty(key(op, stat), value);
            }
        }

        for (APTH stat : APTH.VALUES) {
            float value = max[stat.ordinal()];
            if (Float.isInfinite(value)) continue;
            obj.addProperty(maxKey(stat), value);
        }

        return obj;
    }

    public static BlockEnchantingStatsJsonConfig deserialize(JsonObject obj) {
        String name = obj.has(BLOCK_NAME_KEY) ? obj.get(BLOCK_NAME_KEY).getAsString() : "";
        BlockEnchantingStatsJsonConfig config = new BlockEnchantingStatsJsonConfig(name);

        for (OPS op : OPS.VALUES) {
            for (APTH stat : APTH.VALUES) {
                Float value = read(obj, key(op, stat));
                if (value != null) config.set(stat, op, value);
            }
        }

        for (APTH stat : APTH.VALUES) {
            Float value = read(obj, maxKey(stat));
            if (value != null) config.max(stat, value);
        }

        return config;
    }

    private static boolean isIdentity(OPS op, float value) {
        float identity = BlockEnchantingStats.IDENTITY[op.ordinal()];
        return Float.isNaN(identity) ? Float.isNaN(value) : identity == value;
    }

    @Nullable
    private static Float read(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsFloat() : null;
    }
}
