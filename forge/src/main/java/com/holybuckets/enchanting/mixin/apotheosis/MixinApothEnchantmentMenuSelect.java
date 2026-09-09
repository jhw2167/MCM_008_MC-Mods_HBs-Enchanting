package com.holybuckets.enchanting.mixin.apotheosis;

import com.holybuckets.enchanting.core.EnchantmentCalculator;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Hijacks apotheosis enchanting calculator
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public abstract class MixinApothEnchantmentMenuSelect {

    @Shadow
    protected TableStats stats;

    /** Raised for the duration of a click so the enchant call can be told from the clue calls. */
    @Inject(method = "clickMenuButton", at = @At("HEAD"), remap = false)
    private void hbs_enchanting$markApplying(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentCalculator.markApplying(true);
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"), remap = false)
    private void hbs_enchanting$clearApplying(Player player, int id, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentCalculator.markApplying(false);
    }

    @Inject(
        method = "getEnchantmentList(Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;",
        at = @At("RETURN"), cancellable = true, remap = false)
    private void hbs_enchanting$select(ItemStack stack, int enchantSlot, int level,
                                       CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        cir.setReturnValue(EnchantmentCalculator.select(stack, enchantSlot, level,
            this.stats.quanta(), this.stats.arcana(), this.stats.rectification(),
            cir.getReturnValue()));
    }
}
