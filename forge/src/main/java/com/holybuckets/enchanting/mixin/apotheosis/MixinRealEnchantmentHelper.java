package com.holybuckets.enchanting.mixin.apotheosis;

import com.holybuckets.enchanting.core.EnchantmentCalculator;
import dev.shadowsoffire.apotheosis.ench.table.RealEnchantmentHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Replaces the level cost Apotheosis offers for each enchanting row, and records the eterna that
 * produced it against the stack being enchanted.
 */
@Mixin(value = RealEnchantmentHelper.class, remap = false)
public class MixinRealEnchantmentHelper {

    @Inject(
        method = "getEnchantmentCost(Lnet/minecraft/util/RandomSource;IFLnet/minecraft/world/item/ItemStack;)I",
        at = @At("HEAD"), cancellable = true, remap = false)
    private static void hbs_enchanting$getEnchantmentCost(RandomSource rand, int num, float eterna, ItemStack stack,
                                                          CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(EnchantmentCalculator.Eterna.getEnchantmentCost(rand, num, eterna, stack));
    }
}
