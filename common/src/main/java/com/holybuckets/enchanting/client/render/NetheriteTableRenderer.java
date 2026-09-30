package com.holybuckets.enchanting.client.render;

import com.google.common.util.concurrent.AtomicDouble;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;

/**
 * The mod's tables share the vanilla block entity type, so one renderer serves all three. This
 * suppresses the book on the copper and netherite tables and puts a dragon head on the netherite one.
 */

public class NetheriteTableRenderer {

    private static final ResourceLocation DRAGON = new ResourceLocation("textures/entity/enderdragon/dragon.png");

    private static final float SPIN_DEGREES_PER_TICK = 2.5f;
    private static final float HEAD_SCALE = 0.5f;
    private static final float HEAD_HEIGHT = 1.25f;
    private static final float TILT_DEGREES = 45f;
    private static final double TRACK_RANGE = 6.0d;
    private static final float CHOMP_PERIOD_TICKS = 40f;

    private static final float HEAD_FOLLOW_ADJ = 0f;

    private static final float BREATHE_FIRE_INTERVAL = 1000;

    private SkullModelBase dragonHead;

    public NetheriteTableRenderer(SkullModelBase dragonHead) {
        this.dragonHead = dragonHead;
    }
    
    public void renderDragonHead(EnchantmentTableBlockEntity be, float partialTick,
                                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (this.dragonHead == null) return;

        float time = be.time + partialTick;
        Level level = be.getLevel();
        BlockPos pos = be.getBlockPos();
        boolean useTrackPlayer = level.getNearestPlayer(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d,
            TRACK_RANGE, false) != null;
            //set yaw to be a function of partialtick and time
        AtomicDouble yaw = new AtomicDouble((time*SPIN_DEGREES_PER_TICK) % 360f);
        AtomicDouble tilt = new AtomicDouble(TILT_DEGREES);
        if(useTrackPlayer)
        {
            float playerYaw = trackPlayer(be, time);
            if( trackingTransitionFrames < TOTAL_TRACK_TRANSITION_FRAMES) {
                trackingTransitionFrames++;
                transitionToPlayer(yaw, tilt, playerYaw, 0, trackingTransitionFrames++);
            } else {
                yaw.set(playerYaw);
                tilt.set(0);
            }

        } else if( trackingTransitionFrames > 0) {
            float spinYaw = (float) yaw.get();
            yaw.set(trackPlayer(be, time));
            tilt.set(0);
            int frames = TOTAL_TRACK_TRANSITION_FRAMES - trackingTransitionFrames--;
            transitionToPlayer(yaw, tilt, spinYaw, TILT_DEGREES, frames );

        } else if(be.time% BREATHE_FIRE_INTERVAL == 0) {
            breathFire(be, (float) yaw.get());
        }

        float mouth = chomp(time);

        poseStack.pushPose();
        poseStack.translate(0.5f, HEAD_HEIGHT, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) yaw.get()));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) tilt.get()));
        poseStack.scale(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
        poseStack.scale(-1f, -1f, 1f);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(DRAGON));
        this.dragonHead.setupAnim(mouth, 0f, 0f);
        this.dragonHead.renderToBuffer(poseStack, consumer, packedLight,
            OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        poseStack.popPose();
    }

    int trackingTransitionFrames=0;
    final static int TOTAL_TRACK_TRANSITION_FRAMES = 60;
    private void transitionToPlayer(AtomicDouble currentYaw, AtomicDouble currentTilt,
    float targetYaw, float targetTilt, int frames)
    {
        float progress = (float) frames / TOTAL_TRACK_TRANSITION_FRAMES;

        progress = progress * progress * (3.0f - 2.0f * progress);
        float yawDelta = Mth.wrapDegrees((float)(targetYaw-currentYaw.get()));

        double yaw = currentYaw.get() + yawDelta * progress;
        double tilt = Mth.lerp(progress, currentTilt.get(), (float)targetTilt);

        currentYaw.set(yaw);
        currentTilt.set(tilt);
    }


    private float trackPlayer(EnchantmentTableBlockEntity blockEntity, float time) {
        float spin = time * SPIN_DEGREES_PER_TICK;

        Level level = blockEntity.getLevel();
        if (level == null) return spin;

        BlockPos pos = blockEntity.getBlockPos();

        Player player = level.getNearestPlayer(
            pos.getX() + 0.5d,
            pos.getY() + 0.5d,
            pos.getZ() + 0.5d,
            TRACK_RANGE*2,
            false
        );

        if (player == null) return spin;

        double dx = player.getX() - (pos.getX() + 0.5d);
        double dz = player.getZ() - (pos.getZ() + 0.5d);

        return (float) Math.toDegrees(Math.atan2(-dx, -dz));
    }


    private float chomp(float time) {
        float phase = (time % CHOMP_PERIOD_TICKS) / CHOMP_PERIOD_TICKS;
        return Math.max(0f, Mth.sin(phase * Mth.TWO_PI));
    }

    /** Spins faster, tilts all the way to face up and lets out a fiery breath. */
    public void spinToTop(float time, AtomicDouble yaw, AtomicDouble tilt, float spin) {
        yaw.set(spin);
    }

    private int fireBreathAnimationFrames=0;
    public void animateFireBreath(float time, AtomicDouble yaw, AtomicDouble tilt) {
        if(fireBreathAnimationFrames < 0) return;
        float progress = (float) fireBreathAnimationFrames / TOTAL_TRACK_TRANSITION_FRAMES;

        progress = progress * progress * (3.0f - 2.0f * progress);
        double tiltTarget = Mth.lerp(progress, 0f, -TILT_DEGREES);

        tilt.set(tiltTarget);

        if(fireBreathAnimationFrames++ > TOTAL_TRACK_TRANSITION_FRAMES)
            fireBreathAnimationFrames = -1;
    }

    /** Emits dragon breath from in front of the head. */
    public void breathFire(EnchantmentTableBlockEntity blockEntity, float yaw) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        double radians = Math.toRadians(yaw + 90f);
        double x = pos.getX() + 0.5d + Math.cos(radians) * 0.6d;
        double y = pos.getY() + HEAD_HEIGHT;
        double z = pos.getZ() + 0.5d + Math.sin(radians) * 0.6d;

        level.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z,
            Math.cos(radians) * 0.1d, 0.0d, Math.sin(radians) * 0.1d);
    }
}
