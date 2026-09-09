package com.holybuckets.enchanting.externalapi;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Map;


public interface IEnchantInfoProvider {

    int MAX_POWER = 200;

    EnchantmentPowerInfo get(Enchantment enchantment);

    /** Every registered enchantment, keyed by enchantment. */
    Map<Enchantment, EnchantmentPowerInfo> getAll();

    /** Only the enchantments that may be applied to the given stack. */
    Map<Enchantment, EnchantmentPowerInfo> getAllValidFor(ItemStack stack);

}
