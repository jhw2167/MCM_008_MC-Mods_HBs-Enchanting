package com.holybuckets.enchanting.client.render;

import com.holybuckets.enchanting.block.be.CopperEnchantingTableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.util.Mth;

/**
 * Copy of the vanilla EnchantTableRenderer, bound to the copper enchanting table block entity.
 */
public class CopperEnchantingTableRenderer implements BlockEntityRenderer<CopperEnchantingTableBlockEntity> {

    private final BookModel bookModel;

    public CopperEnchantingTableRenderer(BlockEntityRendererProvider.Context context) {
        this.bookModel = new BookModel(context.bakeLayer(ModelLayers.BOOK));
    }

    @Override
    public void render(CopperEnchantingTableBlockEntity be, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int light, int overlay) {
        poseStack.pushPose();
        poseStack.translate(0.5f, 0.75f, 0.5f);
        float time = be.time + partialTick;
        poseStack.translate(0.0f, 0.1f + Mth.sin(time * 0.1f) * 0.01f, 0.0f);

        float delta = be.rot - be.oRot;
        while (delta >= (float) Math.PI) delta -= (float) (Math.PI * 2);
        while (delta < -(float) Math.PI) delta += (float) (Math.PI * 2);

        poseStack.mulPose(Axis.YP.rotation(-(be.oRot + delta * partialTick)));
        poseStack.mulPose(Axis.ZP.rotationDegrees(80.0f));

        float flip = Mth.lerp(partialTick, be.oFlip, be.flip);
        float leftPage = Mth.frac(flip + 0.25f) * 1.6f - 0.3f;
        float rightPage = Mth.frac(flip + 0.75f) * 1.6f - 0.3f;
        float open = Mth.lerp(partialTick, be.oOpen, be.open);

        this.bookModel.setupAnim(0.0f, Mth.clamp(leftPage, 0.0f, 1.0f), Mth.clamp(rightPage, 0.0f, 1.0f), open);
        VertexConsumer consumer = EnchantTableRenderer.BOOK_LOCATION.buffer(buffer, RenderType::entitySolid);
        this.bookModel.render(poseStack, consumer, light, overlay, 1.0f, 1.0f, 1.0f, 1.0f);
        poseStack.popPose();
    }
}
