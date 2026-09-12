package com.holybuckets.enchanting.client;

import com.holybuckets.enchanting.config.ModConfig;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.OPS;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * Replaces the Apotheosis block stat tooltip with this mod's own, so a block reports every
 * operation it applies rather than only its additive value.
 */
public class BlockStatsTooltip {

    private static final String[] STAT_KEYS = { "eterna", "quanta", "arcana", "rectification", "clues" };

    private static final ChatFormatting[] STAT_COLORS = {
        ChatFormatting.GREEN,
        ChatFormatting.RED,
        ChatFormatting.DARK_PURPLE,
        ChatFormatting.YELLOW,
        ChatFormatting.DARK_AQUA
    };

    //Apotheosis stat lines, removed so the two formats are not shown side by side
    private static final List<String> APOTH_KEYS = List.of(
        "info.apotheosis.ench_stats",
        "info.apotheosis.eterna", "info.apotheosis.eterna.p",
        "info.apotheosis.quanta", "info.apotheosis.quanta.p",
        "info.apotheosis.arcana", "info.apotheosis.arcana.p",
        "info.apotheosis.rectification", "info.apotheosis.rectification.p",
        "info.apotheosis.clues", "info.apotheosis.clues.p");

    private BlockStatsTooltip() {}

    public static void append(ItemStack stack, List<Component> tooltip) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) return;

        Block block = blockItem.getBlock();
        ModConfig config = ModConfig.getInstance();
        if (config == null || !config.hasBlockStats(block)) return;

        BlockEnchantingStats stats = config.getBlockStats(block);
        if (stats == null) return;

        List<Component> lines = new ArrayList<>();
        for (APTH stat : APTH.VALUES) {
            Component line = statLine(stats, stat);
            if (line != null) lines.add(line);
        }
        if (lines.isEmpty()) return;

        removeApotheosisStats(tooltip);
        tooltip.add(Component.translatable("gui.hbs_enchanting.stats").withStyle(ChatFormatting.GOLD));
        tooltip.addAll(lines);
    }

    private static Component statLine(BlockEnchantingStats stats, APTH stat) {
        List<String> parts = new ArrayList<>();

        float add = stats.get(stat, OPS.ADD);
        if (add != 0f) parts.add(signed(add));

        float sub = stats.get(stat, OPS.SUB);
        if (sub != 0f) parts.add(signed(-sub));

        if (!stat.isAdditiveOnly()) {
            float mult = stats.get(stat, OPS.MULT);
            if (mult != 1f) parts.add(percent(mult));

            float div = stats.get(stat, OPS.DIV);
            if (div != 1f) parts.add(percent(div));

            float set = stats.get(stat, OPS.SET);
            if (!Float.isNaN(set)) parts.add(text("gui.hbs_enchanting.stat.set", set));

            float floor = stats.get(stat, OPS.FLR);
            if (floor != Float.NEGATIVE_INFINITY) parts.add(text("gui.hbs_enchanting.stat.min", floor));

            float ceil = stats.get(stat, OPS.CEIL);
            if (ceil != Float.POSITIVE_INFINITY) parts.add(text("gui.hbs_enchanting.stat.max", ceil));
        }

        if (parts.isEmpty()) return null;

        float max = stats.getMax(stat);
        if (add != 0f && max != 0f && !Float.isInfinite(max)) {
            parts.add(text("gui.hbs_enchanting.stat.cap", max));
        }

        MutableComponent name = Component.translatable("gui.hbs_enchanting.stat." + STAT_KEYS[stat.ordinal()]);
        return name.append(Component.literal(": " + String.join(" ", parts)))
            .withStyle(STAT_COLORS[stat.ordinal()]);
    }

    private static String text(String key, float value) {
        return Component.translatable(key, format(value)).getString();
    }

    private static String signed(float value) {
        return (value > 0 ? "+" : "") + format(value);
    }

    private static String percent(float factor) {
        return Math.round(factor * 100f) + "%";
    }

    private static String format(float value) {
        if (value == Math.rint(value)) return String.valueOf((int) value);
        return String.format("%.2f", value);
    }

    private static void removeApotheosisStats(List<Component> tooltip) {
        tooltip.removeIf(line -> line.getContents() instanceof TranslatableContents contents
            && APOTH_KEYS.contains(contents.getKey()));
    }
}
