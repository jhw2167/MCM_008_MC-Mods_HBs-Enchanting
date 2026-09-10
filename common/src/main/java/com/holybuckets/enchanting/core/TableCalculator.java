package com.holybuckets.enchanting.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.holybuckets.enchanting.config.ModConfig;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

//Calculates enchanting table stats from blocks present
public class TableCalculator {

    public static Map<Block, Integer> countBlocks(Level level, BlockPos pos, int radius)
    {
        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - 1);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + 4);
        Map<Block, Integer> configured = new HashMap<>();
        Set<Block> uniqueBlockTypes = new HashSet<>();
        ModConfig config = ModConfig.getInstance();

        //One mutable position and one loaded check per column, rather than per block
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
            for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                if (!level.isLoaded(cursor.set(x, minY, z))) continue;

                for (int y = minY; y <= maxY; y++) {
                    cursor.set(x, y, z);
                    if (cursor.equals(pos)) continue;

                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    if (config.hasBlockStats(block) && config.isFirstOfUniqueBlockType(uniqueBlockTypes, block)) {
                        configured.merge(block, 1, Integer::sum);
                    }
                }
            }
        }
        return  configured;
    }


    public static List<Contribution> contributions(Map<Block, Integer> counts) {
        List<Contribution> list = new ArrayList<>(counts.size());
        for (Map.Entry<Block, Integer> entry : counts.entrySet()) {
            BlockEnchantingStats stats = ModConfig.getInstance().getBlockStats(entry.getKey());
            if (stats != null) list.add(new Contribution(stats, entry.getValue()));
        }
        return list;
    }


    /**
     * @param ledgers filled in with the net contribution each block made to each stat, including
     *                the amount a multiply or divide moved the running total by
     */
    public static float[] calculate(Collection<Contribution> contributions, Map<Block, Ledger> ledgers)
    {
        for (Contribution c : contributions) {
            ledgers.computeIfAbsent(c.stats.getBlock(), Ledger::new).count = c.count;
        }

        float[] totals = new float[BlockEnchantingStats.APTH.VALUES.length];
        for (BlockEnchantingStats.APTH stat : BlockEnchantingStats.APTH.VALUES) {
            int s = stat.ordinal();

            for (Contribution c : contributions) {
                float added = c.stats.get(stat, BlockEnchantingStats.OPS.ADD) * c.count;
                float max = c.stats.getMax(stat);
                added = added >= 0 ? Math.min(added, max) : Math.max(added, -max);
                totals[s] += added;
                record(ledgers, c, stat, added);
            }

            for (Contribution c : contributions) {
                float subbed = c.stats.get(stat, BlockEnchantingStats.OPS.SUB) * c.count;
                totals[s] -= subbed;
                record(ledgers, c, stat, -subbed);
            }

            //Rectification and clues accumulate only
            if (stat.isAdditiveOnly()) {
                totals[s] = Math.max(totals[s], 0f);
                continue;
            }

            //From here the operations act on the running total, so the ledger records the amount
            //each block moved it by rather than the factor itself
            for (Contribution c : contributions) {
                float before = totals[s];
                totals[s] *= (float) Math.pow(c.stats.get(stat, BlockEnchantingStats.OPS.MULT), c.count);
                record(ledgers, c, stat, totals[s] - before);
            }

            for (Contribution c : contributions) {
                float before = totals[s];
                totals[s] *= (float) Math.pow(c.stats.get(stat, BlockEnchantingStats.OPS.DIV), c.count);
                record(ledgers, c, stat, totals[s] - before);
            }

            for (Contribution c : contributions) {
                float set = c.stats.get(stat, BlockEnchantingStats.OPS.SET);
                if (Float.isNaN(set)) continue;
                float before = totals[s];
                totals[s] = set;
                record(ledgers, c, stat, totals[s] - before);
            }

            for (Contribution c : contributions) {
                float before = totals[s];
                totals[s] = Math.max(totals[s], c.stats.get(stat, BlockEnchantingStats.OPS.FLR));
                record(ledgers, c, stat, totals[s] - before);
            }

            for (Contribution c : contributions) {
                float before = totals[s];
                totals[s] = Math.min(totals[s], c.stats.get(stat, BlockEnchantingStats.OPS.CEIL));
                record(ledgers, c, stat, totals[s] - before);
            }

            totals[s] = Math.max(totals[s], 0f);
        }
        return totals;
    }

    private static void record(Map<Block, Ledger> ledgers, Contribution c,
                               BlockEnchantingStats.APTH stat, float delta) {
        if (delta == 0f) return;
        Ledger ledger = ledgers.computeIfAbsent(c.stats.getBlock(), Ledger::new);
        if (delta > 0) ledger.add(stat, delta);
        else ledger.sub(stat, -delta);
    }

    private static String lastLedgerJson = "[]";
    public static String getLastLedgerJson() {
        return lastLedgerJson;
    }
    public static void setLastLedger(Map<Block, Ledger> ledgers) {
        JsonArray array = new JsonArray();
        for (Ledger ledger : ledgers.values()) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(ledger.block);
            if (id == null) continue;

            for (BlockEnchantingStats.APTH stat : BlockEnchantingStats.APTH.VALUES) {
                float net = ledger.net(stat);
                if (net == 0f) continue;

                JsonObject obj = new JsonObject();
                obj.addProperty("s", stat.ordinal());
                obj.addProperty("b", id.toString());
                obj.addProperty("v", net);
                array.add(obj);
            }
        }
        lastLedgerJson = array.toString();
    }

    public static class Contribution {
        public final BlockEnchantingStats stats;
        public final int count;

        public Contribution(BlockEnchantingStats stats, int count) {
            this.stats = stats;
            this.count = count;
        }
    }

    public static class Ledger {
        public final Block block;
        public int count;
        final float[] adds;
        final float[] subs;

        public Ledger(Block block) {
            this.block = block;
            this.adds = new float[BlockEnchantingStats.APTH.VALUES.length];
            this.subs = new float[BlockEnchantingStats.APTH.VALUES.length];
        }

        public void add(BlockEnchantingStats.APTH stat, float value) {
            adds[stat.ordinal()] += value;
        }

        public void sub(BlockEnchantingStats.APTH stat, float value) {
            subs[stat.ordinal()] += value;
        }

        /** What this block was worth to the stat overall, negative when it cost more than it gave. */
        public float net(BlockEnchantingStats.APTH stat) {
            return adds[stat.ordinal()] - subs[stat.ordinal()];
        }
    }
}
