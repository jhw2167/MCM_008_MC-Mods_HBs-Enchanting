package com.holybuckets.enchanting.block;

import com.holybuckets.enchanting.Constants;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.block.BalmBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ModBlocks {

    public static Block templateBlock;
    public static Block copperEnchantingTable;
    public static Block netheriteEnchantingTable;

    public static void initialize(BalmBlocks blocks) {
        blocks.register(() -> templateBlock = new EmptyBlock(defaultProperties()), () -> itemBlock(templateBlock), id("template_block"));

        blocks.register(() -> copperEnchantingTable = new CopperEnchantingTableBlock(enchantingTableProperties()),
            () -> itemBlock(copperEnchantingTable), id("copper_enchanting_table"));

        blocks.register(() -> netheriteEnchantingTable = new CopperEnchantingTableBlock(enchantingTableProperties()),
            () -> itemBlock(netheriteEnchantingTable), id("netherite_enchanting_table"));

        /*
        DyeColor[] colors = DyeColor.values();
        for (DyeColor color : colors) {
            blocks.register(() -> scopedSharestones[color.ordinal()] = new SharestoneBlock(defaultProperties(), color), () -> itemBlock(scopedSharestones[color.ordinal()]), id(color.getSerializedName() + "_sharestone"));
        }
        */

    }

    public static int getTableTier(BlockState state) {
        if (state == null) return EnchantingTierCaps.TIER_NORMAL;
        Block block = state.getBlock();
        if (block == copperEnchantingTable) return EnchantingTierCaps.TIER_COPPER;
        if (block == netheriteEnchantingTable) return EnchantingTierCaps.TIER_NETHERITE;
        return EnchantingTierCaps.TIER_NORMAL;
    }

    private static BlockItem itemBlock(Block block) {
        return new BlockItem(block, Balm.getItems().itemProperties());
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }

    private static BlockBehaviour.Properties enchantingTableProperties() {
        return BlockBehaviour.Properties.copy(Blocks.ENCHANTING_TABLE);
    }

    private static BlockBehaviour.Properties defaultProperties() {
        return Balm.getBlocks().blockProperties().sound(SoundType.STONE).strength(5f, 2000f);
    }
}
