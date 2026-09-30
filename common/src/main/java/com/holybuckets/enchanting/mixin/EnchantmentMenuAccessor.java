package com.holybuckets.enchanting.mixin;

import net.minecraft.world.inventory.EnchantmentMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the vanilla cost array so injectors in mixins that disable remapping can still reach it. */
@Mixin(EnchantmentMenu.class)
public interface EnchantmentMenuAccessor {

    @Accessor("costs")
    int[] hbs_enchanting$getCosts();
}
