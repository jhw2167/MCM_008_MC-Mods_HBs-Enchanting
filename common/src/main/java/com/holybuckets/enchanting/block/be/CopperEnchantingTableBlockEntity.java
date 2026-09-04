package com.holybuckets.enchanting.block.be;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Nameable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Mirrors vanilla EnchantmentTableBlockEntity, including the book animation state.
 */
public class CopperEnchantingTableBlockEntity extends BlockEntity implements Nameable {

    private static final RandomSource RANDOM = RandomSource.create();

    public int time;
    public float flip;
    public float oFlip;
    public float flipT;
    public float flipA;
    public float open;
    public float oOpen;
    public float rot;
    public float oRot;
    public float tRot;

    @Nullable
    private Component name;

    public CopperEnchantingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.copperEnchantingTable.get(), pos, state);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (this.hasCustomName()) {
            tag.putString("CustomName", Component.Serializer.toJson(this.name));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        if (tag.contains("CustomName", 8)) {
            this.name = Component.Serializer.fromJson(tag.getString("CustomName"));
        }
    }

    public static void bookAnimationTick(Level level, BlockPos pos, BlockState state, CopperEnchantingTableBlockEntity be) {
        be.oOpen = be.open;
        be.oRot = be.rot;
        Player player = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3.0, false);

        if (player != null) {
            double dx = player.getX() - (pos.getX() + 0.5);
            double dz = player.getZ() - (pos.getZ() + 0.5);
            be.tRot = (float) Mth.atan2(dz, dx);
            be.open += 0.1f;
            if (be.open < 0.5f || RANDOM.nextInt(40) == 0) {
                float previous = be.flipT;
                do {
                    be.flipT += RANDOM.nextInt(4) - 1;
                } while (previous == be.flipT);
            }
        } else {
            be.tRot += 0.02f;
            be.open -= 0.1f;
        }

        while (be.rot >= (float) Math.PI) be.rot -= (float) (Math.PI * 2);
        while (be.rot < -(float) Math.PI) be.rot += (float) (Math.PI * 2);
        while (be.tRot >= (float) Math.PI) be.tRot -= (float) (Math.PI * 2);
        while (be.tRot < -(float) Math.PI) be.tRot += (float) (Math.PI * 2);

        float delta = be.tRot - be.rot;
        while (delta >= (float) Math.PI) delta -= (float) (Math.PI * 2);
        while (delta < -(float) Math.PI) delta += (float) (Math.PI * 2);

        be.rot += delta * 0.4f;
        be.open = Mth.clamp(be.open, 0.0f, 1.0f);
        be.time++;
        be.oFlip = be.flip;

        float flipDelta = Mth.clamp((be.flipT - be.flip) * 0.4f, -0.2f, 0.2f);
        be.flipA += (flipDelta - be.flipA) * 0.9f;
        be.flip += be.flipA;
    }

    @Override
    public Component getName() {
        return this.name != null ? this.name : Component.translatable("container.enchant");
    }

    public void setCustomName(@Nullable Component name) {
        this.name = name;
    }

    @Nullable
    @Override
    public Component getCustomName() {
        return this.name;
    }
}
