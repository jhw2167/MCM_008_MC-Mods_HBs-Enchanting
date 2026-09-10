package com.holybuckets.enchanting.mixin.apotheosis;

import com.holybuckets.enchanting.config.ModConfig;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.OPS;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reports this mod's block stats wherever Apotheosis asks the registry for a block's contribution.
 * <p>
 * Every stat display Apotheosis has reads these six getters: the item tooltip in EnchModuleClient
 * and the block tooltips through CommonTooltipUtil for Jade and TOP. Overriding here keeps a
 * single source of truth instead of replacing each display, and makes blocks that Apotheosis has
 * no stat file for, such as obsidian or mob heads, report their configured values too.
 * <p>
 * Only the ADD operation and the per stat max are expressible through this interface; multiply,
 * divide, set, floor and ceiling have no equivalent and are not shown.
 */
@Mixin(value = EnchantingStatRegistry.class, remap = false)
public class MixinEnchantingStatRegistry {

    @Inject(method = "getEterna", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getEterna(BlockState state, Level world, BlockPos pos,
                                                 CallbackInfoReturnable<Float> cir) {
        add(state, APTH.ETRN, cir);
    }

    @Inject(method = "getMaxEterna", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getMaxEterna(BlockState state, Level world, BlockPos pos,
                                                    CallbackInfoReturnable<Float> cir) {
        BlockEnchantingStats stats = statsFor(state);
        if (stats == null) return;
        float max = stats.getMax(APTH.ETRN);
        cir.setReturnValue(Float.isInfinite(max) ? 0f : max);
    }

    @Inject(method = "getQuanta", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getQuanta(BlockState state, Level world, BlockPos pos,
                                                 CallbackInfoReturnable<Float> cir) {
        add(state, APTH.QNTA, cir);
    }

    @Inject(method = "getArcana", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getArcana(BlockState state, Level world, BlockPos pos,
                                                 CallbackInfoReturnable<Float> cir) {
        add(state, APTH.ARCN, cir);
    }

    @Inject(method = "getQuantaRectification", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getRectification(BlockState state, Level world, BlockPos pos,
                                                        CallbackInfoReturnable<Float> cir) {
        add(state, APTH.RECT, cir);
    }

    @Inject(method = "getBonusClues", at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getClues(BlockState state, Level world, BlockPos pos,
                                                CallbackInfoReturnable<Integer> cir) {
        BlockEnchantingStats stats = statsFor(state);
        if (stats == null) return;
        cir.setReturnValue((int) stats.get(APTH.CLUE, OPS.ADD));
    }

    private static void add(BlockState state, APTH stat, CallbackInfoReturnable<Float> cir) {
        BlockEnchantingStats stats = statsFor(state);
        if (stats == null) return;
        cir.setReturnValue(stats.get(stat, OPS.ADD));
    }

    /** Null for a block this mod does not configure, which reports zero rather than the Apotheosis value. */
    private static BlockEnchantingStats statsFor(BlockState state) {
        return ModConfig.getInstance().getBlockStats(state.getBlock());
    }
}
