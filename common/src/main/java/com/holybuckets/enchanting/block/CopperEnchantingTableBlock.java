package com.holybuckets.enchanting.block;

import com.holybuckets.enchanting.block.be.BlockEntityTypes;
import com.holybuckets.foundation.core.WoolColorHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
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

    private static final double PARTICLE_RANGE = 8.0d;
    private static final double PARTICLE_SPREAD = 1.1d;
    private static final double PARTICLE_MIN_HEIGHT = 0.75d;
    private static final double PARTICLE_MAX_HEIGHT = 2.5d;
    private static final int PARTICLE_MAX_COUNT = 4;

    protected void spawnUseParticles(BlockState state, Level level, BlockPos pos, RandomSource random) {
        double centerX = pos.getX() + 0.5d;
        double centerY = pos.getY();
        double centerZ = pos.getZ() + 0.5d;

        Player player = level.getNearestPlayer(centerX, centerY, centerZ, PARTICLE_RANGE, false);
        if (player == null) return;

        double distance = Math.sqrt(player.distanceToSqr(centerX, centerY, centerZ));
        double nearness = 1.0d - Math.min(distance / PARTICLE_RANGE, 1.0d);

        int count = (int) Math.ceil(PARTICLE_MAX_COUNT * nearness * nearness);
        for (int i = 0; i < count; i++) {
            double x = centerX + (random.nextDouble() - 0.5d) * 2.0d * PARTICLE_SPREAD;
            double z = centerZ + (random.nextDouble() - 0.5d) * 2.0d * PARTICLE_SPREAD;
            double y = centerY + PARTICLE_MIN_HEIGHT
                + random.nextDouble() * (PARTICLE_MAX_HEIGHT - PARTICLE_MIN_HEIGHT);

            level.addParticle(ParticleTypes.WITCH, x, y, z, 0.0d, -0.04d - random.nextDouble() * 0.03d, 0.0d);
        }

        //If distance is <4, add some particles emiting from the center of block like redstone dust,

        if(distance < 4.0d)
        {
            double x = centerX;
            double z = centerZ;
            double y = centerY + 1.1d;

            for(int i = 0; i < 3; i++) {
                double xVel = random.nextGaussian()*10;
                double zVel = random.nextGaussian()*10;
                double yVel = random.nextGaussian()+2;
                level.addParticle(WoolColorHelper.getDust(8), x+(xVel/10), y, z+(zVel/10),
                    xVel, 5.0d-yVel, zVel); //velocity
            }

            if(random.nextDouble()<0.1d)
                level.addParticle(WoolColorHelper.getDust(11), x, y, z,
                 -2.0d, 1.0d, -5.0d);
        }

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
