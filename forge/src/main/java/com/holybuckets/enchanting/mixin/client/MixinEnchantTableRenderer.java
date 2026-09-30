package com.holybuckets.enchanting.mixin.client;

import com.holybuckets.enchanting.block.ModBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The mod's tables share the vanilla block entity type, so one renderer serves all three. This
 * suppresses the book on the copper and netherite tables and puts a dragon head on the netherite one.
 */
@Mixin(EnchantTableRenderer.class)
public class MixinEnchantTableRenderer {

    @Unique
    private static final ResourceLocation HBS_ENCHANTING$DRAGON =
        new ResourceLocation("textures/entity/enderdragon/dragon.png");

    @Unique
    private static final float HBS_ENCHANTING$SPIN_DEGREES_PER_TICK = 2.5f;

    @Unique
    private static final float HBS_ENCHANTING$HEAD_SCALE = 0.5f;

    @Unique
    private static final float HBS_ENCHANTING$HEAD_HEIGHT = 1.25f;

    @Unique
    private static final float HBS_ENCHANTING$TILT_DEGREES = 30f;

    @Unique
    private static final double HBS_ENCHANTING$TRACK_RANGE = 8.0d;

    @Unique
    private static final float HBS_ENCHANTING$CHOMP_PERIOD_TICKS = 40f;

    @Unique
    private SkullModelBase hbs_enchanting$dragonHead;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void hbs_enchanting$bakeDragonHead(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        this.hbs_enchanting$dragonHead =
            new DragonHeadModel(context.getModelSet().bakeLayer(ModelLayers.DRAGON_SKULL));
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hbs_enchanting$replaceBook(EnchantmentTableBlockEntity blockEntity, float partialTick,
                                            PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, int packedOverlay, CallbackInfo ci) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        Block block = level.getBlockState(blockEntity.getBlockPos()).getBlock();
        if (block != ModBlocks.copperEnchantingTable && block != ModBlocks.netheriteEnchantingTable) return;

        if (block==ModBlocks.netheriteEnchantingTable) {
            hbs_enchanting$renderDragonHead(blockEntity, partialTick, poseStack, buffer, packedLight);
        }
        ci.cancel();
    }

    @Unique
    private void hbs_enchanting$renderDragonHead(EnchantmentTableBlockEntity blockEntity, float partialTick,
                                                 PoseStack poseStack, MultiBufferSource buffer, int packedLight) {
        if (this.hbs_enchanting$dragonHead == null) return;

        float time = blockEntity.time + partialTick;
        float yaw = hbs_enchanting$trackPlayer(blockEntity, time);
        float mouth = 0f;

        poseStack.pushPose();
        poseStack.translate(0.5f, HBS_ENCHANTING$HEAD_HEIGHT, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(HBS_ENCHANTING$TILT_DEGREES));
        poseStack.scale(HBS_ENCHANTING$HEAD_SCALE, HBS_ENCHANTING$HEAD_SCALE, HBS_ENCHANTING$HEAD_SCALE);
        poseStack.scale(-1f, -1f, 1f);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(HBS_ENCHANTING$DRAGON));
        this.hbs_enchanting$dragonHead.setupAnim(mouth, 0f, 0f);
        this.hbs_enchanting$dragonHead.renderToBuffer(poseStack, consumer, packedLight,
            OverlayTexture.NO_OVERLAY, 1f, 1f, 1f, 1f);

        poseStack.popPose();
    }

    /** Jaw opening amount, 0 shut to 1 wide, on a repeating cycle. */
    @Unique
    private float hbs_enchanting$chomp(float time) {
        float phase = (time % HBS_ENCHANTING$CHOMP_PERIOD_TICKS) / HBS_ENCHANTING$CHOMP_PERIOD_TICKS;
        return Math.max(0f, Mth.sin(phase * Mth.TWO_PI));
    }

    /** Yaw facing the nearest player, falling back to the idle spin when nobody is close. */
    @Unique
    private float hbs_enchanting$trackPlayer(EnchantmentTableBlockEntity blockEntity, float time) {
        float spin = time * HBS_ENCHANTING$SPIN_DEGREES_PER_TICK;

        Level level = blockEntity.getLevel();
        if (level == null) return spin;

        BlockPos pos = blockEntity.getBlockPos();
        Player player = level.getNearestPlayer(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d,
            HBS_ENCHANTING$TRACK_RANGE, false);
        if (player == null) return spin;

        double dx = player.getX() - (pos.getX() + 0.5d);
        double dz = player.getZ() - (pos.getZ() + 0.5d);
        return (float) (Mth.atan2(dz, dx) * (180d / Math.PI)) - 90f;
    }

    /** Emits dragon breath from in front of the head. */
    @Unique
    private void hbs_enchanting$breathFire(EnchantmentTableBlockEntity blockEntity, float yaw) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        BlockPos pos = blockEntity.getBlockPos();
        double radians = Math.toRadians(yaw + 90f);
        double x = pos.getX() + 0.5d + Math.cos(radians) * 0.6d;
        double y = pos.getY() + HBS_ENCHANTING$HEAD_HEIGHT;
        double z = pos.getZ() + 0.5d + Math.sin(radians) * 0.6d;

        level.addParticle(ParticleTypes.DRAGON_BREATH, x, y, z,
            Math.cos(radians) * 0.1d, 0.0d, Math.sin(radians) * 0.1d);
    }
}
