package com.holybuckets.enchanting.mixin.apotheosis;

import com.holybuckets.enchanting.block.ModBlocks;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.enchanting.config.ModConfig;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces Apotheosis's stat gathering with a tier aware version.
 * <p>
 * Vanilla and Apotheosis both read stats from the fixed BOOKSHELF_OFFSETS ring. This scans a cube
 * of the tier's configured radius, applies the blockEnchantingStats config to every block listed
 * there, falls back to Apotheosis's own gathering for blocks the config does not mention, and
 * finally clamps the totals to the tier's ceilings.
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public class MixinApothEnchantmentMenu {

    @Inject(
        method = "gatherStats(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;I)Ldev/shadowsoffire/apotheosis/ench/table/ApothEnchantmentMenu$TableStats;",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$gatherStatsInRadius(Level level, BlockPos pos, int itemEnch,
                                                           CallbackInfoReturnable<TableStats> cir) {
        int tier = ModBlocks.getTableTier(level.getBlockState(pos));
        EnchantingTierCaps caps = ModConfig.getInstance().getTierCaps(tier);
        int radius = caps.getRadius();

        TableStats.Builder builder = new TableStats.Builder(itemEnch);
        Map<Block, Integer> configured = new HashMap<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int minY = Math.max(level.getMinBuildHeight(), pos.getY() - radius);
        int maxY = Math.min(level.getMaxBuildHeight() - 1, pos.getY() + radius);

        for (int y = minY; y <= maxY; y++) {
            for (int x = pos.getX() - radius; x <= pos.getX() + radius; x++) {
                for (int z = pos.getZ() - radius; z <= pos.getZ() + radius; z++) {
                    cursor.set(x, y, z);
                    if (cursor.equals(pos)) continue;
                    if (!level.isLoaded(cursor)) continue;

                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) continue;

                    Block block = state.getBlock();
                    if (ModConfig.getInstance().hasBlockStats(block)) {
                        configured.merge(block, 1, Integer::sum);
                    } else {
                        ApothEnchantmentMenu.gatherStats(builder, level, cursor.immutable());
                    }
                }
            }
        }

        TableStats base = builder.build();
        float[] totals = { base.eterna(), base.quanta(), base.arcana() };
        BlockEnchantingStats.apply(contributions(configured), totals);

        cir.setReturnValue(new TableStats(
            caps.capEterna(totals[APTH.ETRN.ordinal()]),
            caps.capQuanta(totals[APTH.QNTA.ordinal()]),
            caps.capArcana(totals[APTH.ARCN.ordinal()]),
            base.rectification(),
            base.clues(),
            base.blacklist(),
            base.treasure()));
    }

    private static List<BlockEnchantingStats.Contribution> contributions(Map<Block, Integer> counts) {
        List<BlockEnchantingStats.Contribution> list = new ArrayList<>(counts.size());
        for (Map.Entry<Block, Integer> entry : counts.entrySet()) {
            BlockEnchantingStats stats = ModConfig.getInstance().getBlockStats(entry.getKey());
            if (stats != null) list.add(new BlockEnchantingStats.Contribution(stats, entry.getValue()));
        }
        return list;
    }
}
