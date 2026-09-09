package com.holybuckets.enchanting.mixin;

import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The enchanting screen greys out and prices every row against this count. Reporting a full stack
 * keeps the rows selectable now that lapis is not a material cost.
 */
@Mixin(EnchantmentMenu.class)
public class MixinEnchantmentMenu {

    @Inject(method = "getGoldCount", at = @At("HEAD"), cancellable = true)
    private void hbs_enchanting$noLapisCost(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(64);
    }
}
