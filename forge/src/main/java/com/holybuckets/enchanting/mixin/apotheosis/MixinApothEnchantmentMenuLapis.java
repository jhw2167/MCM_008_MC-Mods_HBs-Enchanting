package com.holybuckets.enchanting.mixin.apotheosis;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Removes lapis as a material cost.
 * <p>
 * clickMenuButton reads the lapis stack into a local, refuses the enchant when it holds less than
 * the row cost, then shrinks it. Swapping the local for a throwaway stack satisfies the check and
 * sends the shrink to the temporary copy, so the player's own slot is never touched. The local is
 * the second ItemStack in the method, after the item being enchanted.
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public class MixinApothEnchantmentMenuLapis {

    @ModifyVariable(method = "clickMenuButton", at = @At("STORE"), ordinal = 1, remap = false)
    private ItemStack hbs_enchanting$noLapisCost(ItemStack lapis) {
        return new ItemStack(Items.LAPIS_LAZULI, 64);
    }
}
