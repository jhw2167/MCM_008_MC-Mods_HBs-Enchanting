package com.holybuckets.enchanting.client.screen;

import com.holybuckets.enchanting.Constants;
import com.holybuckets.enchanting.menu.ModEnchantmentMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;

/**
 * Vanilla enchanting screen with the lapis slot repurposed: an Enchanted Essence ghost outline on
 * the standard table, and nothing at all on the copper table.
 */
public class ModEnchantmentScreen extends EnchantmentScreen {

    private static final ResourceLocation ESSENCE_ICON =
        new ResourceLocation("hbs_foundation", "textures/item/enchanted_essence.png");

    private static final ResourceLocation TEXTURE =
        new ResourceLocation(Constants.MOD_ID, "textures/gui/copper_enchanting_table.png");

    private static final int SLOT_X = 35;
    private static final int SLOT_Y = 47;
    private static final int GUI_BACKGROUND = 0xFFC6C6C6;

    private final boolean copper;

    public ModEnchantmentScreen(EnchantmentMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.copper = menu instanceof ModEnchantmentMenu modMenu && modMenu.isCopper();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        if (copper) {
            graphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
            graphics.fill(x + SLOT_X - 1, y + SLOT_Y - 1, x + SLOT_X + 17, y + SLOT_Y + 17, GUI_BACKGROUND);
            return;
        }

        if (this.menu.getSlot(ModEnchantmentMenu.ESSENCE_SLOT).hasItem()) return;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.35f);
        graphics.blit(ESSENCE_ICON, x + SLOT_X, y + SLOT_Y, 0, 0, 16, 16, 16, 16);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }
}
