package com.holybuckets.enchanting.block;

import com.holybuckets.enchanting.block.be.BlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jetbrains.annotations.Nullable;

//Copper enchanting table, extends vanilla enchanting table, has some restrictions
public class CopperEnchantingTableBlock extends EnchantmentTableBlock {

    /** Switches the table's top texture; nothing sets it yet. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CopperEnchantingTableBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LIT, false));
        BlockEntityTypes.addValidBlock(BlockEntityType.ENCHANTING_TABLE, this);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        spawnUseParticles(state, level, pos, random);
    }

    /** Client side only, called on random display ticks while the table is in view. */
    protected void spawnUseParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {

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
