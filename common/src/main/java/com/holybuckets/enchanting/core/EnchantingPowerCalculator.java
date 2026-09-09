package com.holybuckets.enchanting.core;

import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.config.json.EnchantingTableJsonConfig;
import com.holybuckets.enchanting.config.ModConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Computes enchanting power for a table by scanning a configurable radius and applying
 * the per block type caps declared in the enchanting block power config.
 */
public class EnchantingPowerCalculator {

    private static final String CLASS_ID = "013";

    private EnchantingPowerCalculator() {}

    /**
     * @param maxPower hard cap on the returned power; 0 or less means uncapped
     */
    public static int getPower(Level level, BlockPos tablePos, int radius, int maxPower) {
        Map<String, Integer> counts = countPowerBlocks(level, tablePos, radius);

        float power = 0f;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            EnchantingTableJsonConfig.BlockPower blockPower = ModConfig.getInstance().getBlockPower(entry.getKey());
            if (blockPower == null) continue;
            power += blockPower.contribution(entry.getValue());
        }

        int total = (int) Math.floor(power);
        if (maxPower > 0) total = Math.min(total, maxPower);
        total = Math.max(total, 0);

        LoggerProject.logDebug(CLASS_ID + "001", String.format(
            "Enchanting power at %s: %d (cap %d, radius %d, configured blocks %d) from %s",
            tablePos, total, maxPower, radius,
            ModConfig.getInstance().getBlockPowers().size(), counts));

        return total;
    }

    /** Counts every configured power providing block within radius of the table. */
    public static Map<String, Integer> countPowerBlocks(Level level, BlockPos tablePos, int radius) {
        Map<String, Integer> counts = new HashMap<>();
        if (radius <= 0) return counts;

        int minY = Math.max(level.getMinBuildHeight(), tablePos.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, tablePos.getY() + radius);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            for (int x = tablePos.getX() - radius; x <= tablePos.getX() + radius; x++) {
                for (int z = tablePos.getZ() - radius; z <= tablePos.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    if (!level.isLoaded(cursor)) continue;

                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) continue;

                    String id = blockId(state.getBlock());
                    if (!isPowerBlock(id)) continue;
                    counts.merge(id, 1, Integer::sum);
                }
            }
        }

        return counts;
    }

    private static boolean isPowerBlock(String blockId) {
        return ModConfig.getInstance().getBlockPower(blockId) != null;
    }

    private static String blockId(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return key == null ? "" : key.toString();
    }
}
