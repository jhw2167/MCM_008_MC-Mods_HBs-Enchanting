package com.holybuckets.enchanting.block;

import com.holybuckets.enchanting.block.be.CopperEnchantingTableBlockEntity;
import com.holybuckets.enchanting.block.be.ModBlockEntities;
import net.minecraft.core.BlockPos;
import com.holybuckets.enchanting.menu.EnchantingTableMenuProvider;
import net.blay09.mods.balm.api.Balm;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.EnchantmentTableBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A copper variant of the vanilla enchanting table; mirrors all vanilla EnchantmentTableBlock behavior.
 */
public class CopperEnchantingTableBlock extends BaseEntityBlock {

    protected static final VoxelShape SHAPE = box(0.0, 0.0, 0.0, 16.0, 12.0, 16.0);

    public CopperEnchantingTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);

        for (BlockPos offset : EnchantmentTableBlock.BOOKSHELF_OFFSETS) {
            if (random.nextInt(16) != 0) continue;
            if (!EnchantmentTableBlock.isValidBookShelf(level, pos, offset)) continue;

            level.addParticle(ParticleTypes.ENCHANT,
                pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5,
                (offset.getX() + random.nextFloat()) - 0.5,
                (offset.getY() - random.nextFloat() - 1.0f),
                (offset.getZ() + random.nextFloat()) - 0.5);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CopperEnchantingTableBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, ModBlockEntities.copperEnchantingTable.get(),
            CopperEnchantingTableBlockEntity::bookAnimationTick) : null;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopperEnchantingTableBlockEntity table) {
            Balm.getNetworking().openMenu(player,
                new EnchantingTableMenuProvider(level, pos, table.getDisplayName(), true));
        }
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof CopperEnchantingTableBlockEntity table)) {
            return null;
        }

        return new EnchantingTableMenuProvider(level, pos, table.getDisplayName(), true);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        if (!stack.hasCustomHoverName()) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof CopperEnchantingTableBlockEntity table) {
            table.setCustomName(stack.getHoverName());
        }
    }

    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }
}
