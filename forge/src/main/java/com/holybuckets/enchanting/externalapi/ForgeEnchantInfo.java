package com.holybuckets.enchanting.externalapi;

import com.holybuckets.foundation.GeneralConfig;
import dev.shadowsoffire.apotheosis.ench.EnchModule;
import dev.shadowsoffire.apotheosis.ench.EnchantmentInfo;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.HashMap;
import java.util.Map;


public class ForgeEnchantInfo implements IEnchantInfoProvider {

    @Override
    public EnchantmentPowerInfo get(Enchantment enchantment) {
        EnchantmentInfo info = EnchModule.getEnchInfo(enchantment);

        int maxLevel = info.getMaxLevel();
        int[] min = new int[maxLevel + 1];
        int[] max = new int[maxLevel + 1];

        for (int level = 1; level <= maxLevel; level++) {
            min[level] = info.getMinPower(level);
            max[level] = info.getMaxPower(level);
        }

        return new EnchantmentPowerInfo(enchantment, maxLevel, info.getMaxLootLevel(),
            info.isTreasure(), info.isDiscoverable(), info.isLootable(), info.isTradeable(),
            min, max);
    }

    @Override
    public Map<Enchantment, EnchantmentPowerInfo> getAll() {
        Registry<Enchantment> registry = GeneralConfig.getInstance().getServer()
            .registryAccess().registryOrThrow(Registries.ENCHANTMENT);

        Map<Enchantment, EnchantmentPowerInfo> all = new HashMap<>();
        for (Enchantment enchantment : registry) {
            all.put(enchantment, get(enchantment));
        }
        return all;
    }

    @Override
    public Map<Enchantment, EnchantmentPowerInfo> getAllValidFor(ItemStack stack) {
        Map<Enchantment, EnchantmentPowerInfo> valid = new HashMap<>();
        for (Map.Entry<Enchantment, EnchantmentPowerInfo> entry : getAll().entrySet()) {
            if (entry.getKey().canEnchant(stack)) valid.put(entry.getKey(), entry.getValue());
        }
        return valid;
    }
}
