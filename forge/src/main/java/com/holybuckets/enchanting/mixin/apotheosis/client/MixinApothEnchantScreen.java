package com.holybuckets.enchanting.mixin.apotheosis.client;

import com.holybuckets.enchanting.client.EnchantingTierClient;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.enchanting.core.EnchantmentCalculator;
import com.holybuckets.enchanting.mixin.apotheosis.ApothEnchantmentMenuAccessor;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantScreen.SuperRender;
import dev.shadowsoffire.apotheosis.ench.table.ApothEnchantmentMenu.TableStats;
import dev.shadowsoffire.apotheosis.ench.table.EnchantingStatRegistry;
import dev.shadowsoffire.apotheosis.util.DrawsOnLeft;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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

    private static final int NUMERAL_Y = 15;
    private static final int ROW_HEIGHT = 19;

    private static final int LEDGER_GAP = 4;
    private static final int LEDGER_PAD = 8;
    private static final int TOOLTIP_OFFSET = 12;
    private static final int LEDGER_TOP = 24;
    private static final int LEDGER_MAX_ROWS = 2;

    private static final String[] STAT_KEYS = { "eterna", "quanta", "arcana", "rectification", "clues" };
    private static final ChatFormatting[] STAT_COLORS = {
        ChatFormatting.GREEN, ChatFormatting.RED, ChatFormatting.DARK_PURPLE,
        ChatFormatting.YELLOW, ChatFormatting.DARK_AQUA
    };

    private static final int OFF_SCREEN = -9999;

    private static final int REGION_NONE = -1;
    private static final int REGION_ETERNA = 0;
    private static final int REGION_QUANTA = 1;
    private static final int REGION_ARCANA = 2;
    private static final int REGION_INFO_BUTTON = 3;

    private static final int MARKER_TOP_OFFSET = -1;
    private static final int MARKER_HEIGHT = 7;
    private static final int MARKER_EDGE = 0xFF3F3F3F;
    private static final int MARKER_CORE = 0xFFFFFFFF;

    /**
     * Takes over rendering while the cursor sits on a stat bar or the info button.
     * <p>
     * Apotheosis builds all of its hint tooltips inline in render, guarded by isHovering calls
     * that cannot be targeted individually. Instead this reproduces the two lines of render that
     * draw the screen itself, draws the HB tooltip, and cancels the rest. Everything visual still
     * happens because renderBg runs inside apoth_superRender; only the tooltip logic is skipped,
     * and only over regions where Apotheosis would have drawn a hint of its own.
     */
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void hbs_enchanting$renderStatHints(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks,
                                                CallbackInfo ci) {
        ApothEnchantScreen screen = (ApothEnchantScreen) (Object) this;

        int region = regionAt(screen, mouseX, mouseY);
        if (region == REGION_NONE) return;

        screen.renderBackground(gfx);
        ((SuperRender) screen).apoth_superRender(gfx, mouseX, mouseY, screen.getMinecraft().getFrameTime());

        if (region != REGION_INFO_BUTTON) {
            TableStats stats = ((ApothEnchantmentMenuAccessor) screen.getMenu()).hbs_enchanting$getStats();
            Font font = screen.getMinecraft().font;

            EnchantingTierCaps caps = EnchantingTierClient.getCaps();

            if (region == REGION_ETERNA) {
                gfx.renderComponentTooltip(font,
                    hint("eterna", ChatFormatting.GREEN, stats.eterna(), caps.getEternaMax()), mouseX, mouseY);
            }
            else if (region == REGION_QUANTA) {
                gfx.renderComponentTooltip(font,
                    hint("quanta", ChatFormatting.RED, stats.quanta(), caps.getQuantaMax()), mouseX, mouseY);
                drawOnLeft(screen, gfx, quantaPopup(stats.quanta()));
            }
            else {
                gfx.renderComponentTooltip(font,
                    hint("arcana", ChatFormatting.DARK_PURPLE, stats.arcana(), caps.getArcanaMax()), mouseX, mouseY);
                drawOnLeft(screen, gfx, arcanaPopup(stats.arcana()));
            }
        }

        if (region == REGION_INFO_BUTTON) {
            gfx.renderComponentTooltip(screen.getMinecraft().font, ledgerHint(), mouseX, mouseY);
        }

        drawLedger(screen, gfx);
        ci.cancel();
    }

    /** The ledger panel also draws on the normal path, where render is not cancelled. */
    @Inject(method = "render", at = @At("TAIL"))
    private void hbs_enchanting$renderLedger(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks,
                                             CallbackInfo ci) {
        drawLedger((ApothEnchantScreen) (Object) this, gfx);
    }

    private static List<Component> ledgerHint() {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting.ledger").withStyle(ChatFormatting.GOLD));
        list.add(Component.translatable("gui.hbs_enchanting.ledger.toggle").withStyle(ChatFormatting.GRAY));
        return list;
    }

    /**
     * Lists which blocks moved each stat, on the right of the menu. Values are the net effect the
     * block had, so a divisor like the bee shelf shows the amount it removed rather than its factor.
     */
    private void drawLedger(ApothEnchantScreen screen, GuiGraphics gfx) {
        if (!this.hbs_enchanting$ledgerOpen) return;

        List<Component> lines = new ArrayList<>();

        for (APTH stat : APTH.VALUES) {
            List<EnchantingTierClient.LedgerEntry> entries = EnchantingTierClient.getLedger(stat.ordinal());
            if (entries.isEmpty()) continue;

            lines.add(Component.translatable("gui.hbs_enchanting." + STAT_KEYS[stat.ordinal()])
                .withStyle(STAT_COLORS[stat.ordinal()], ChatFormatting.UNDERLINE));

            //Additions first, then subtractions, largest effect first
            appendGroup(lines, entries, true);
            appendGroup(lines, entries, false);
        }

        if (lines.isEmpty()) return;

        Font font = screen.getMinecraft().font;
        int width = 0;
        for (Component line : lines) width = Math.max(width, font.width(line));

        //renderComponentTooltip offsets by 12 internally, so back that out to sit flush, then keep
        //the panel on screen: to the left of the menu when there is no room on the right
        int x = screen.getGuiLeft() + screen.getXSize() + LEDGER_GAP - TOOLTIP_OFFSET;
        if (x + TOOLTIP_OFFSET + width + LEDGER_PAD > screen.width) {
            x = screen.getGuiLeft() - width - LEDGER_PAD - TOOLTIP_OFFSET - LEDGER_GAP;
        }

        gfx.renderComponentTooltip(font, lines, Math.max(0, x), screen.getGuiTop() + LEDGER_TOP);
    }

    /** Lists the two largest entries then rolls whatever is left into a single Other line. */
    private static void appendGroup(List<Component> lines, List<EnchantingTierClient.LedgerEntry> entries,
                                    boolean positive) {
        List<EnchantingTierClient.LedgerEntry> group = new ArrayList<>();
        for (EnchantingTierClient.LedgerEntry entry : entries) {
            if (positive == entry.value() > 0) group.add(entry);
        }
        group.sort((a, b) -> Float.compare(Math.abs(b.value()), Math.abs(a.value())));

        for (int i = 0; i < Math.min(LEDGER_MAX_ROWS, group.size()); i++) {
            lines.add(ledgerLine(group.get(i)));
        }

        float rest = 0f;
        for (int i = LEDGER_MAX_ROWS; i < group.size(); i++) rest += group.get(i).value();
        if (rest != 0f) {
            String amount = (rest > 0 ? "+" : "") + format(rest);
            lines.add(Component.literal(" " + amount + " ")
                .append(Component.translatable("gui.hbs_enchanting.ledger.other"))
                .withStyle(rest > 0 ? ChatFormatting.WHITE : ChatFormatting.GOLD));
        }
    }

    private static Component ledgerLine(EnchantingTierClient.LedgerEntry entry) {
        boolean positive = entry.value() > 0;
        String amount = (positive ? "+" : "") + format(entry.value());
        return Component.literal(" " + amount + " ").append(entry.block().getName())
            .withStyle(positive ? ChatFormatting.WHITE : ChatFormatting.GOLD);
    }

    @Unique
    private boolean hbs_enchanting$ledgerOpen = false;

    /** The info button toggles the ledger rather than opening the Apotheosis info screen. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void hbs_enchanting$toggleLedger(double mouseX, double mouseY, int button,
                                             CallbackInfoReturnable<Boolean> cir) {
        ApothEnchantScreen screen = (ApothEnchantScreen) (Object) this;
        if (!isOver(screen, INFO_BUTTON_X, INFO_BUTTON_Y, INFO_BUTTON_WIDTH, INFO_BUTTON_HEIGHT, mouseX, mouseY)) {
            return;
        }

        this.hbs_enchanting$ledgerOpen = !this.hbs_enchanting$ledgerOpen;
        screen.getMinecraft().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
        cir.setReturnValue(true);
    }

    private static int regionAt(ApothEnchantScreen screen, int mouseX, int mouseY) {
        if (isOver(screen, HOVER_X, HOVER_ETERNA_Y, HOVER_WIDTH, HOVER_HEIGHT, mouseX, mouseY)) return REGION_ETERNA;
        if (isOver(screen, HOVER_X, HOVER_QUANTA_Y, HOVER_WIDTH, HOVER_HEIGHT, mouseX, mouseY)) return REGION_QUANTA;
        if (isOver(screen, HOVER_X, HOVER_ARCANA_Y, HOVER_WIDTH, HOVER_HEIGHT, mouseX, mouseY)) return REGION_ARCANA;
        if (isOver(screen, INFO_BUTTON_X, INFO_BUTTON_Y, INFO_BUTTON_WIDTH, INFO_BUTTON_HEIGHT, mouseX, mouseY)) {
            return REGION_INFO_BUTTON;
        }
        return REGION_NONE;
    }

    private static boolean isOver(ApothEnchantScreen screen, int x, int y, int width, int height,
                                  double mouseX, double mouseY) {
        double relX = mouseX - screen.getGuiLeft();
        double relY = mouseY - screen.getGuiTop();
        return relX >= x - 1 && relX < x + width + 1 && relY >= y - 1 && relY < y + height + 1;
    }

    /**
     * Hides the info button graphic by pushing its y off screen.
     * <p>
     * ModifyArgs would be the natural fit but its synthetic Args class cannot be loaded under
     * ModLauncher, so this modifies the single y argument instead. Every other blit in renderBg
     * draws at guiTop or below; the info button is the only one above the panel, which makes its
     * y value a reliable discriminator without depending on a call ordinal.
     */
    @ModifyArg(
        method = "renderBg",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),
        index = 2)
    private int hbs_enchanting$hideNumerals(int y) {
        ApothEnchantScreen screen = (ApothEnchantScreen) (Object) this;
        int guiTop = screen.getGuiTop();

        //The 1/2/3 numeral icons sit one pixel below their row background, which is the only
        //thing distinguishing them; the rows themselves stay visible. The info button is left
        //alone because it now toggles the ledger.
        for (int row = 0; row < 3; row++) {
            if (y == guiTop + NUMERAL_Y + ROW_HEIGHT * row) return OFF_SCREEN;
        }
        return y;
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

    /** Title, description, then the live value in green and the tier ceiling in red. */
    private static List<Component> hint(String stat, ChatFormatting titleColor, float current, int max) {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting." + stat).withStyle(titleColor));
        list.add(Component.translatable("gui.hbs_enchanting." + stat + ".desc").withStyle(ChatFormatting.GRAY));
        list.add(Component.literal(I18n.get("gui.hbs_enchanting.current", format(current)))
            .withStyle(ChatFormatting.GREEN));
        list.add(Component.literal(I18n.get("gui.hbs_enchanting.max", format(max)))
            .withStyle(ChatFormatting.RED));
        return list;
    }

    private static String format(float value) {
        return value == Math.round(value) ? String.valueOf(Math.round(value)) : String.format("%.1f", value);
    }

    private static List<Component> quantaPopup(float quanta) {
        List<Component> list = new ArrayList<>();
        list.add(Component.literal(I18n.get("gui.hbs_enchanting.quanta.rerolls",
            EnchantingTierClient.getRerollsRemaining())).withStyle(ChatFormatting.BLUE));

        String page = EnchantingTierClient.getPage();
        if (!page.isEmpty()) {
            list.add(Component.literal(I18n.get("gui.hbs_enchanting.quanta.page", page))
                .withStyle(ChatFormatting.GOLD));
        }
        return list;
    }

    private static List<Component> arcanaPopup(float arcana) {
        List<Component> list = new ArrayList<>();
        list.add(Component.translatable("gui.hbs_enchanting.arcana.bonus")
            .withStyle(ChatFormatting.UNDERLINE, ChatFormatting.DARK_PURPLE));

        int maxSegment = EnchantmentCalculator.Arcana.getMaxSegment(arcana);
        if (maxSegment < 1) {
            list.add(Component.translatable("gui.hbs_enchanting.arcana.none").withStyle(ChatFormatting.GRAY));
            return list;
        }

        for (int segment = 1; segment <= maxSegment; segment++) {
            list.add(Component.literal(I18n.get("gui.hbs_enchanting.arcana.segment",
                segment, percent(EnchantmentCalculator.Arcana.getOverlevelChance(segment, arcana)))).withStyle(ChatFormatting.BLUE));
            list.add(Component.literal(I18n.get("gui.hbs_enchanting.arcana.curse",
                percent(EnchantmentCalculator.Arcana.getCurseChance(segment, arcana)))).withStyle(ChatFormatting.DARK_RED));
        }
        return list;
    }

    private static String percent(float chance) {
        return String.valueOf(Math.round(chance * 100f));
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
