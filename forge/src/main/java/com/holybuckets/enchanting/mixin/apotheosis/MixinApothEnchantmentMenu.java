package com.holybuckets.enchanting.mixin.apotheosis;

import com.holybuckets.enchanting.block.ModBlocks;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.enchanting.config.ModConfig;
import com.holybuckets.enchanting.core.TableCalculator;
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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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

        Map<Block, Integer> configured = TableCalculator.countBlocks(level, pos, radius);
        Map<Block, TableCalculator.Ledger> ledgers = new LinkedHashMap<>();
        float[] totals = TableCalculator.calculate(TableCalculator.contributions(configured), ledgers);
        TableCalculator.setLastLedger(ledgers);

        cir.setReturnValue(new TableStats(
            caps.capEterna(totals[APTH.ETRN.ordinal()]),
            caps.capQuanta(totals[APTH.QNTA.ordinal()]),
            caps.capArcana(totals[APTH.ARCN.ordinal()]),
            totals[APTH.RECT.ordinal()],
            (int) totals[APTH.CLUE.ordinal()],
            Set.of(),
            false));
    }

}
