package com.holybuckets.enchanting.client;

import com.holybuckets.enchanting.Constants;
import com.holybuckets.enchanting.block.be.ModBlockEntities;
import com.holybuckets.enchanting.client.render.CopperEnchantingTableRenderer;
import net.blay09.mods.balm.api.client.rendering.BalmRenderers;
import net.minecraft.resources.ResourceLocation;

public class ModRenderers {

    //public static ModelLayerLocation someModel;

    public static void clientInitialize(BalmRenderers renderers) {
        renderers.registerBlockEntityRenderer( id("copper_enchanting_table"),
            ModBlockEntities.copperEnchantingTable::get, CopperEnchantingTableRenderer::new );

        //waystoneModel = renderers.registerModel(new ResourceLocation(Waystones.MOD_ID, "waystone"), () -> WaystoneModel.createLayer(CubeDeformation.NONE));
        //renderers.setBlockRenderType(() -> ModBlocks.stoneBrickBlockEntity, RenderType.cutout());
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

}
