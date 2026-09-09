package com.holybuckets.enchanting.block;

import com.holybuckets.enchanting.block.be.BlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A copper variant of the vanilla enchanting table.
 * <p>
 * The block entity and menu are delegated to whatever block is registered as
 * minecraft:enchanting_table at runtime. Apotheosis replaces that block with its own, so
 * delegating is what makes the copper table open the Apotheosis menu with the Apotheosis tile
 * rather than the vanilla box. Without Apotheosis the delegate is the vanilla block and the
 * behavior is unchanged.
 */
public class CopperEnchantingTableBlock extends EnchantmentTableBlock {

    public CopperEnchantingTableBlock(Properties properties) {
        super(properties);
        BlockEntityTypes.addValidBlock(BlockEntityType.ENCHANTING_TABLE, this);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        BlockEntityTypes.addValidBlock(BlockEntityType.ENCHANTING_TABLE, this);

        Block delegate = Blocks.ENCHANTING_TABLE;
        if (delegate != this && delegate instanceof EntityBlock entityBlock) {
            return entityBlock.newBlockEntity(pos, state);
        }
        return super.newBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        Block delegate = Blocks.ENCHANTING_TABLE;
        if (delegate != this) {
            return delegate.getMenuProvider(state, level, pos);
        }
        return super.getMenuProvider(state, level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        Block delegate = Blocks.ENCHANTING_TABLE;
        if (delegate != this && state.getBlock() != newState.getBlock()) {
            delegate.onRemove(state, level, pos, newState, isMoving);
            return;
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
