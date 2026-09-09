package com.holybuckets.enchanting.mixin.apotheosis;

import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Apotheosis keeps the gathered stats package private to its own screen; this exposes them.
 */
@Mixin(value = ApothEnchantmentMenu.class, remap = false)
public interface ApothEnchantmentMenuAccessor {

    @Accessor("stats")
    TableStats hbs_enchanting$getStats();
}
