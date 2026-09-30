package com.holybuckets.enchanting.mixin;

import com.holybuckets.enchanting.block.ModBlocks;
import com.holybuckets.enchanting.client.render.NetheriteTableRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.dragon.DragonHeadModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.EnchantmentTableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin supresses vanilla's animated enchanting book to feature our custom renderer
 * for copper and netherite enchanting tables
 */
@Mixin(EnchantTableRenderer.class)
public class MixinEnchantTableRenderer {

    @Unique
    private static final ResourceLocation DRAGON_MODEL_PATH = new ResourceLocation("textures/entity/enderdragon/dragon.png");

    @Unique
    private NetheriteTableRenderer renderer;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void bakeDragonHead(BlockEntityRendererProvider.Context context, CallbackInfo ci) {
        this.renderer = new NetheriteTableRenderer(new DragonHeadModel(context.getModelSet().bakeLayer(ModelLayers.DRAGON_SKULL)));
    }

    //Replaces book for extra enchanting tables
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void replaceBook(EnchantmentTableBlockEntity blockEntity, float partialTick,
                                            PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, int packedOverlay, CallbackInfo ci) {
        Level level = blockEntity.getLevel();
        if (level == null) return;

        Block block = level.getBlockState(blockEntity.getBlockPos()).getBlock();
        if (block!=ModBlocks.copperEnchantingTable && block!=ModBlocks.netheriteEnchantingTable) return;

        if (block==ModBlocks.netheriteEnchantingTable) {
            renderer.renderDragonHead(blockEntity, partialTick, poseStack, buffer, packedLight);
        }
        ci.cancel();
    }
}
