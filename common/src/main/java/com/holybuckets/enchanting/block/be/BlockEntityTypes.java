package com.holybuckets.enchanting.block.be;

import com.holybuckets.enchanting.mixin.BlockEntityTypeAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.HashSet;
import java.util.Set;

public class BlockEntityTypes {

    private BlockEntityTypes() {}

    /**
     * Lets a mod block use a vanilla block entity type, so every vanilla and third party hook
     * keyed on that type applies to it unchanged.
     */
    public static void addValidBlock(BlockEntityType<?> type, Block block) {
        if (type == null || block == null) return;
        BlockEntityTypeAccessor accessor = (BlockEntityTypeAccessor) type;
        Set<Block> validBlocks = accessor.getValidBlocks();
        if (validBlocks.contains(block)) return;

        Set<Block> updated = new HashSet<>(validBlocks);
        updated.add(block);
        accessor.setValidBlocks(updated);
    }
}
