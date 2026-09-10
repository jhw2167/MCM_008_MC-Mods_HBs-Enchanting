package com.holybuckets.enchanting.config.json;

import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.OPS;

import java.util.*;

public class DefaultBlockEnchantingStats {

    private DefaultBlockEnchantingStats() {}

    private static final String[] HOSTILE_HEADS = {
        "minecraft:zombie_head",
        "minecraft:skeleton_skull",
        "minecraft:wither_skeleton_skull",
        "minecraft:creeper_head",
        "minecraft:piglin_head",
        "minecraft:dragon_head"
    };

    //Candles trade one stat for a clue; grouped by colour family
    private static final String[] CANDLES_ETERNA = {
        "minecraft:candle", "minecraft:white_candle", "minecraft:light_gray_candle",
        "minecraft:gray_candle", "minecraft:black_candle"
    };
    private static final String[] CANDLES_ARCANA = {
        "minecraft:red_candle", "minecraft:orange_candle", "minecraft:yellow_candle"
    };
    private static final String[] CANDLES_QUANTA = {
        "minecraft:lime_candle", "minecraft:green_candle",
        "minecraft:cyan_candle", "minecraft:light_blue_candle"
    };
    private static final String[] CANDLES_RECT = {
        "minecraft:blue_candle", "minecraft:purple_candle", "minecraft:magenta_candle",
        "minecraft:pink_candle", "minecraft:brown_candle"
    };
    public static final Set<String> CANDLES = new HashSet<>();

    static {
        Collections.addAll(CANDLES, CANDLES_ETERNA);
        Collections.addAll(CANDLES, CANDLES_ARCANA);
        Collections.addAll(CANDLES, CANDLES_QUANTA);
        Collections.addAll(CANDLES, CANDLES_RECT);
    }

    public static List<BlockEnchantingStatsJsonConfig> build() {
        List<BlockEnchantingStatsJsonConfig> entries = new ArrayList<>();

        //Vanilla baseline
        entries.add(entry("minecraft:bookshelf")
            .set(APTH.ETRN, OPS.ADD, 0.25f).max(APTH.ETRN, 5f));

        //Seashelves: eterna and quanta
        entries.add(entry("apotheosis:seashelf")
            .set(APTH.ETRN, OPS.ADD, 0.375f).max(APTH.ETRN, 6f)
            .set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 8f));
        entries.add(entry("apotheosis:crystal_seashelf")
            .set(APTH.ETRN, OPS.ADD, 0.2f).max(APTH.ETRN, 2f)
            .set(APTH.ARCN, OPS.SUB, 2f)
            .set(APTH.QNTA, OPS.ADD, 1f).max(APTH.QNTA, 6f));
        entries.add(entry("apotheosis:heart_seashelf")
            .set(APTH.ETRN, OPS.ADD, 0.4f).max(APTH.ETRN, 2f)
            .set(APTH.ARCN, OPS.SUB, 2f)
            .set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 6f));
        //Infused: more of both, but drains arcana at twice the quanta rate
        entries.add(entry("apotheosis:infused_seashelf")
            .set(APTH.ETRN, OPS.ADD, 0.5f).max(APTH.ETRN, 8f)
            .set(APTH.QNTA, OPS.ADD, 0.75f).max(APTH.QNTA, 7.5f)
            .set(APTH.ARCN, OPS.SUB, 1f));

        //Hellshelves: eterna and arcana
        entries.add(entry("apotheosis:hellshelf")
            .set(APTH.ETRN, OPS.ADD, 0.375f).max(APTH.ETRN, 6f)
            .set(APTH.ARCN, OPS.ADD, 0.5f).max(APTH.ARCN, 8f));
        entries.add(entry("apotheosis:glowing_hellshelf")
            .set(APTH.ETRN, OPS.ADD, 0.2f).max(APTH.ETRN, 2f)
            .set(APTH.QNTA, OPS.SUB, 2f)
            .set(APTH.ARCN, OPS.ADD, 1f).max(APTH.ARCN, 6f));
        entries.add(entry("apotheosis:blazing_hellshelf")
            .set(APTH.ETRN, OPS.ADD, 0.4f).max(APTH.ETRN, 2f)
            .set(APTH.QNTA, OPS.SUB, 2f)
            .set(APTH.ARCN, OPS.ADD, 0.5f).max(APTH.ARCN, 6f));
        entries.add(entry("apotheosis:infused_hellshelf")
            .set(APTH.ETRN, OPS.ADD, 0.5f).max(APTH.ETRN, 8f)
            .set(APTH.ARCN, OPS.ADD, 0.75f).max(APTH.ARCN, 7.5f)
            .set(APTH.QNTA, OPS.SUB, 1f));

        //Endshelves: all three, upgraded variants cost a clue each
        entries.add(entry("apotheosis:endshelf")
            .set(APTH.ETRN, OPS.ADD, 0.625f).max(APTH.ETRN, 11.25f));
        entries.add(entry("apotheosis:pearl_endshelf")
            .set(APTH.ETRN, OPS.ADD, 0.3f).max(APTH.ETRN, 6f)
            .set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 10f)
            .set(APTH.CLUE, OPS.SUB, 0.5f));
        entries.add(entry("apotheosis:draconic_endshelf")
            .set(APTH.ETRN, OPS.ADD, 0.3f).max(APTH.ETRN, 6f)
            .set(APTH.ARCN, OPS.ADD, 0.5f).max(APTH.ARCN, 10f)
            .set(APTH.CLUE, OPS.SUB, 0.5f));

        //Deep and echo shelves: additive, no drawbacks
        entries.add(entry("apotheosis:dormant_deepshelf")
            .set(APTH.ETRN, OPS.ADD, 0.25f).max(APTH.ETRN, 4f));
        entries.add(entry("apotheosis:deepshelf")
            .set(APTH.ETRN, OPS.ADD, 0.75f).max(APTH.ETRN, 15f)
            .set(APTH.QNTA, OPS.ADD, 1.25f).max(APTH.QNTA, 15f)
            .set(APTH.ARCN, OPS.ADD, 1.25f).max(APTH.ARCN, 15f));
        entries.add(entry("apotheosis:echoing_deepshelf")
            .set(APTH.ETRN, OPS.ADD, 0.5f).max(APTH.ETRN, 4f)
            .set(APTH.QNTA, OPS.ADD, 3f).max(APTH.QNTA, 12f));

        entries.add(entry("apotheosis:soul_touched_deepshelf")
            .set(APTH.ETRN, OPS.ADD, 0.5f).max(APTH.ETRN, 4f)
            .set(APTH.ARCN, OPS.ADD, 3f).max(APTH.ARCN, 12f));

        entries.add(entry("apotheosis:echoing_sculkshelf")
            .set(APTH.ETRN, OPS.ADD, 1.25f).max(APTH.ETRN, 10f)
            .set(APTH.QNTA, OPS.ADD, 1.25f).max(APTH.QNTA, 15f)
            .set(APTH.ARCN, OPS.ADD, 3.75f).max(APTH.ARCN, 30f)
            .set(APTH.CLUE, OPS.ADD, 1f).max(APTH.CLUE, 2f));
        entries.add(entry("apotheosis:soul_touched_sculkshelf")
            .set(APTH.ETRN, OPS.ADD, 1.25f).max(APTH.ETRN, 10f)
            .set(APTH.QNTA, OPS.ADD, 3.75f).max(APTH.QNTA, 30f)
            .set(APTH.ARCN, OPS.ADD, 1.25f).max(APTH.ARCN, 15f)
            .set(APTH.RECT, OPS.ADD, 1.25f).max(APTH.RECT, 10f));

        //Novelty shelves: quanta in bulk, eterna and arcana divided down
        entries.add(entry("apotheosis:beeshelf")
            .set(APTH.QNTA, OPS.ADD, 5f).max(APTH.QNTA, 10f)
            .set(APTH.ETRN, OPS.DIV, 0.8f));
        entries.add(entry("apotheosis:melonshelf")
            .set(APTH.QNTA, OPS.ADD, 5f).max(APTH.QNTA, 10f)
            .set(APTH.ETRN, OPS.DIV, 0.8f));
        entries.add(entry("apotheosis:stoneshelf")
            .set(APTH.ETRN, OPS.ADD, 0.5f).max(APTH.ETRN, 10f)
            .set(APTH.QNTA, OPS.SUB, 1f)
            .set(APTH.ARCN, OPS.SUB, 1f));

        //Rectifiers, sight shelves and amethyst
        entries.add(entry("apotheosis:rectifier").set(APTH.RECT, OPS.ADD, 2.5f).max(APTH.RECT, 10f));
        entries.add(entry("apotheosis:rectifier_t2").set(APTH.RECT, OPS.ADD, 3.75f).max(APTH.RECT, 15f));
        entries.add(entry("apotheosis:rectifier_t3").set(APTH.RECT, OPS.ADD, 6.25f).max(APTH.RECT, 25f));
        entries.add(entry("apotheosis:sightshelf").set(APTH.CLUE, OPS.ADD, 1f).max(APTH.CLUE, 2f));
        entries.add(entry("apotheosis:sightshelf_t2").set(APTH.CLUE, OPS.ADD, 1f).max(APTH.CLUE, 3f));
        entries.add(entry("minecraft:amethyst_cluster").set(APTH.RECT, OPS.ADD, 0.375f).max(APTH.RECT, 6f));

        //Candles: a clue each, paid for out of one stat
        for (String candle : CANDLES_ETERNA) entries.add(candle(candle, APTH.ETRN, 5f));
        for (String candle : CANDLES_ARCANA) entries.add(candle(candle, APTH.ARCN, 5f));
        for (String candle : CANDLES_QUANTA) entries.add(candle(candle, APTH.QNTA, 5f));
        for (String candle : CANDLES_RECT) entries.add(candle(candle, APTH.RECT, 5f));

        //Hostile mob heads, half a quanta each, two heads worth per type
        for (String head : HOSTILE_HEADS) {
            entries.add(entry(head).set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 1f));
        }

        //Solid resource blocks buy rectification with eterna
        entries.add(resourceBlock("minecraft:copper_block", 0.5f, 4f, 0.15f));
        entries.add(resourceBlock("minecraft:iron_block", 1f, 6f, 0.25f));
        entries.add(resourceBlock("minecraft:gold_block", 1.5f, 8f, 0.5f));
        entries.add(resourceBlock("minecraft:emerald_block", 2f, 10f, 0.75f));
        entries.add(resourceBlock("minecraft:diamond_block", 3f, 12f, 1f));
        entries.add(resourceBlock("minecraft:netherite_block", 5f, 20f, 2f));

        //Ambience
        entries.add(entry("minecraft:obsidian").set(APTH.ETRN, OPS.ADD, 0.25f).max(APTH.ETRN, 2f));
        entries.add(entry("minecraft:nether_bricks").set(APTH.ARCN, OPS.ADD, 0.25f).max(APTH.ARCN, 2f));
        entries.add(entry("minecraft:lava").set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 2f));
        entries.add(entry("minecraft:water").set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 2f));
        entries.add(entry("minecraft:crying_obsidian").set(APTH.ARCN, OPS.ADD, 0.5f).max(APTH.ARCN, 3f));
        entries.add(entry("minecraft:sculk_catalyst").set(APTH.ARCN, OPS.ADD, 0.5f).max(APTH.ARCN, 2f));
        entries.add(entry("minecraft:soul_sand").set(APTH.ARCN, OPS.ADD, 0.25f).max(APTH.ARCN, 1.5f));
        entries.add(entry("minecraft:soul_soil").set(APTH.ARCN, OPS.ADD, 0.25f).max(APTH.ARCN, 1.5f));
        entries.add(entry("minecraft:amethyst_block").set(APTH.QNTA, OPS.ADD, 0.25f).max(APTH.QNTA, 2f));
        entries.add(entry("minecraft:lectern").set(APTH.QNTA, OPS.ADD, 0.25f).max(APTH.QNTA, 2f));
        entries.add(entry("minecraft:end_stone_bricks").set(APTH.ETRN, OPS.ADD, 0.2f).max(APTH.ETRN, 2f));

        //Multiplier, divisor, floor and ceiling examples
        entries.add(entry("minecraft:beacon").set(APTH.ETRN, OPS.MULT, 1.05f));
        entries.add(entry("minecraft:redstone_block").set(APTH.QNTA, OPS.MULT, 1.05f));
        entries.add(entry("minecraft:cobweb").set(APTH.QNTA, OPS.DIV, 0.9f));
        entries.add(entry("minecraft:sea_lantern").set(APTH.QNTA, OPS.FLR, 5f));

        return entries;
    }

    private static BlockEnchantingStatsJsonConfig entry(String blockName) {
        return new BlockEnchantingStatsJsonConfig(blockName);
    }

    private static BlockEnchantingStatsJsonConfig candle(String blockName, APTH cost, float amount) {
        return entry(blockName)
            .set(APTH.CLUE, OPS.ADD, 1f).max(APTH.CLUE, 1f)
            .set(cost, OPS.SUB, amount);
    }

    private static BlockEnchantingStatsJsonConfig resourceBlock(String blockName, float rectification,
                                                                float maxRectification, float subEterna) {
        return entry(blockName)
            .set(APTH.RECT, OPS.ADD, rectification).max(APTH.RECT, maxRectification)
            .set(APTH.ETRN, OPS.SUB, subEterna);
    }
}
