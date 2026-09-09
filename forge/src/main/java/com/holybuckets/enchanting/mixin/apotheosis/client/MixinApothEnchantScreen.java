package com.holybuckets.enchanting.mixin.apotheosis.client;

import com.holybuckets.enchanting.client.EnchantingTierClient;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.enchanting.core.ArcanaCalculator;
import com.holybuckets.enchanting.core.QuantaCalculator;
import com.holybuckets.enchanting.mixin.apotheosis.ApothEnchantmentMenuAccessor;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.apotheosis.util.DrawsOnLeft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Replaces the Apotheosis stat bar hints with the HB wording, adds the left hand popups for
 * quanta and arcana, removes the info button, and draws the tier cap markers.
 * <p>
 * Apotheosis builds all three hints inline inside render, so the only seam is the isHovering call
 * that guards each block: redirecting those to false suppresses its tooltips, and ours are drawn
 * at the tail of render instead.
 */
@Mixin(ApothEnchantScreen.class)
public abstract class MixinApothEnchantScreen {

    private static final int BAR_X = 59;
    private static final int BAR_WIDTH = 110;
    private static final int ETERNA_Y = 75;
    private static final int QUANTA_Y = 85;
    private static final int ARCANA_Y = 95;

    private static final int HOVER_X = 60;
    private static final int HOVER_ETERNA_Y = 76;
    private static final int HOVER_QUANTA_Y = 86;
    private static final int HOVER_ARCANA_Y = 96;
    private static final int HOVER_WIDTH = 110;
    private static final int HOVER_HEIGHT = 5;

    private static final int INFO_BUTTON_X = 145;
    private static final int INFO_BUTTON_Y = -15;
    private static final int INFO_BUTTON_WIDTH = 27;
    private static final int INFO_BUTTON_HEIGHT = 15;

    private static final int MARKER_TOP_OFFSET = -1;
    private static final int MARKER_HEIGHT = 7;
    private static final int MARKER_EDGE = 0xFF3F3F3F;
    private static final int MARKER_CORE = 0xFFFFFFFF;

    /**
     * Suppresses the Apotheosis hover blocks for the three stat bars and for the info button,
     * and answers every other hover test exactly as the vanilla method would.
     */
    @Redirect(
        method = { "render", "renderBg", "mouseClicked" },
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;isHovering(IIIIDD)Z"))
    private boolean hbs_enchanting$suppressHovers(ApothEnchantScreen screen, int x, int y, int width, int height,
                                                  double mouseX, double mouseY) {
        if (width == HOVER_WIDTH && height == HOVER_HEIGHT) return false;
        if (x == INFO_BUTTON_X && y == INFO_BUTTON_Y) return false;

        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= x - 1 && relX < x + width + 1 && relY >= y - 1 && relY < y + height + 1;
    }

    /** Drops the info button graphic; it is the only 27x15 blit in the background. */
    @Redirect(
        method = "renderBg",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"))
    private void hbs_enchanting$hideInfoButton(GuiGraphics gfx, ResourceLocation texture, int x, int y,
                                               int u, int v, int width, int height) {
        if (width == INFO_BUTTON_WIDTH && height == INFO_BUTTON_HEIGHT) return;
        gfx.blit(texture, x, y, u, v, width, height);
    }

    @Inject(method = "renderBg", at = @At("TAIL"))
    private void hbs_enchanting$renderStatCaps(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        ApothEnchantScreen screen = (ApothEnchantScreen) (Object) this;
        int xCenter = screen.getGuiLeft();
        int yCenter = screen.getGuiTop();

        EnchantingTierCaps caps = EnchantingTierClient.getCaps();
        drawCap(gfx, xCenter, yCenter + ETERNA_Y, caps.getEternaMax(), EnchantingStatRegistry.getAbsoluteMaxEterna());
        drawCap(gfx, xCenter, yCenter + QUANTA_Y, caps.getQuantaMax(), 100f);
        drawCap(gfx, xCenter, yCenter + ARCANA_Y, caps.getArcanaMax(), 100f);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void hbs_enchanting$renderStatHints(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        ApothEnchantScreen screen = (ApothEnchantScreen) (Object) this;
        TableStats stats = ((ApothEnchantmentMenuAccessor) screen.getMenu()).hbs_enchanting$getStats();

        if (isOver(screen, HOVER_ETERNA_Y, mouseX, mouseY)) {
            gfx.renderComponentTooltip(screen.getMinecraft().font, eternaHint(), mouseX, mouseY);
        }
        else if (isOver(screen, HOVER_QUANTA_Y, mouseX, mouseY)) {
            gfx.renderComponentTooltip(screen.getMinecraft().font, quantaHint(), mouseX, mouseY);
            drawOnLeft(screen, gfx, quantaPopup(stats.quanta()));
        }
        else if (isOver(screen, HOVER_ARCANA_Y, mouseX, mouseY)) {
            gfx.renderComponentTooltip(screen.getMinecraft().font, arcanaHint(), mouseX, mouseY);
            drawOnLeft(screen, gfx, arcanaPopup(stats.arcana()));
        }
    }

    private static List<Component> eternaHint() {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting.eterna").withStyle(ChatFormatting.GREEN));
        list.add(Component.translatable("gui.hbs_enchanting.eterna.desc").withStyle(ChatFormatting.GRAY));
        return list;
    }

    private static List<Component> quantaHint() {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting.quanta").withStyle(ChatFormatting.RED));
        list.add(Component.translatable("gui.hbs_enchanting.quanta.desc").withStyle(ChatFormatting.GRAY));
        return list;
    }

    private static List<Component> arcanaHint() {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting.arcana").withStyle(ChatFormatting.DARK_PURPLE));
        list.add(Component.translatable("gui.hbs_enchanting.arcana.desc").withStyle(ChatFormatting.GRAY));
        return list;
    }

    private static List<Component> quantaPopup(float quanta) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(I18n.get("gui.hbs_enchanting.quanta.rerolls",
            QuantaCalculator.getOptionCount(quanta))).withStyle(ChatFormatting.BLUE));
        return list;
    }

    private static List<Component> arcanaPopup(float arcana) {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting.arcana.bonus")
            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.DARK_PURPLE));

        int maxSegment = ArcanaCalculator.getMaxSegment(arcana);
        if (maxSegment < 1) {
            list.add(Component.translatable("gui.hbs_enchanting.arcana.none").withStyle(ChatFormatting.GRAY));
            return list;
        }

        for (int segment = 1; segment <= maxSegment; segment++) {
            list.add(Component.literal(I18n.get("gui.hbs_enchanting.arcana.segment",
                segment, percent(ArcanaCalculator.getOverlevelChance(segment, arcana)))).withStyle(ChatFormatting.BLUE));
            list.add(Component.literal(I18n.get("gui.hbs_enchanting.arcana.curse",
                percent(ArcanaCalculator.getCurseChance(segment, arcana)))).withStyle(ChatFormatting.DARK_RED));
        }
        return list;
    }

    private static String percent(float chance) {
        return String.valueOf(Math.round(chance * 100f));
    }

    private static boolean isOver(ApothEnchantScreen screen, int y, int mouseX, int mouseY) {
        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= HOVER_X - 1 && relX < HOVER_X + HOVER_WIDTH + 1
            && relY >= y - 1 && relY < y + HOVER_HEIGHT + 1;
    }

    private static void drawOnLeft(ApothEnchantScreen screen, GuiGraphics gfx, List<Component> list) {
        ((DrawsOnLeft) screen).drawOnLeft(gfx, list, screen.getGuiTop() + 29);
    }

    private static void drawCap(GuiGraphics gfx, int xCenter, int barY, float cap, float scale) {
        if (cap <= 0f || scale <= 0f) return;

        int offset = Math.min(Math.round(cap / scale * BAR_WIDTH), BAR_WIDTH);
        int x = xCenter + BAR_X + offset;
        int top = barY + MARKER_TOP_OFFSET;
        int bottom = top + MARKER_HEIGHT;

        gfx.fill(x - 1, top, x + 2, bottom, MARKER_EDGE);
        gfx.fill(x, top, x + 1, bottom, MARKER_CORE);
    }
}
