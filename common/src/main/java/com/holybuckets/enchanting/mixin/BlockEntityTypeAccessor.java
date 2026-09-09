package com.holybuckets.enchanting.mixin;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Set;

/**
 * Opens up the immutable valid block set so a mod block can share a vanilla block entity type.
 */
@Mixin(BlockEntityType.class)
public interface BlockEntityTypeAccessor {

    @Accessor("validBlocks")
    Set<Block> getValidBlocks();

    @Mutable
    @Accessor("validBlocks")
    void setValidBlocks(Set<Block> validBlocks);
}
