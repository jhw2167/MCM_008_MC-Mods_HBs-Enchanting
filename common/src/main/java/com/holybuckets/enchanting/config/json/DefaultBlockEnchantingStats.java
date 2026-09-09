package com.holybuckets.enchanting.config.json;

import com.holybuckets.enchanting.config.model.BlockEnchantingStats.APTH;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats.OPS;

import java.util.ArrayList;
import java.util.List;

public class DefaultBlockEnchantingStats {

    private DefaultBlockEnchantingStats() {}

    private static final String[] HOSTILE_HEADS = {
        "minecraft:zombie_head", "minecraft:zombie_wall_head",
        "minecraft:skeleton_skull", "minecraft:skeleton_wall_skull",
        "minecraft:wither_skeleton_skull", "minecraft:wither_skeleton_wall_skull",
        "minecraft:creeper_head", "minecraft:creeper_wall_head",
        "minecraft:piglin_head", "minecraft:piglin_wall_head",
        "minecraft:dragon_head", "minecraft:dragon_wall_head"
    };

    public static List<BlockEnchantingStatsJsonConfig> build() {
        List<BlockEnchantingStatsJsonConfig> entries = new ArrayList<>();

        //Shelves, at a quarter of their Apotheosis values
        entries.add(adder("minecraft:bookshelf", 0.25f, 0f, 0f, 5f, 0f, 0f));
        entries.add(adder("apotheosis:seashelf", 0.5f, 0f, 0.5f, 8f, 0f, 4f));
        entries.add(adder("apotheosis:hellshelf", 0.5f, 0.25f, 0f, 8f, 6f, 0f));
        entries.add(adder("apotheosis:infused_hellshelf", 0.4375f, 0.4375f, 0f, 6.75f, 6f, 0f));
        entries.add(adder("apotheosis:endshelf", 1f, 1.25f, 1.25f, 15f, 10f, 10f));
        entries.add(adder("apotheosis:deepshelf", 1f, 1.25f, 1.25f, 15f, 10f, 10f));
        entries.add(entry("apotheosis:stoneshelf")
            .set(APTH.ETRN, OPS.ADD, -0.375f)
            .set(APTH.ARCN, OPS.ADD, -1.875f));
        entries.add(entry("apotheosis:melonshelf")
            .set(APTH.ETRN, OPS.ADD, -0.25f)
            .set(APTH.QNTA, OPS.ADD, -2.5f));

        //Hostile mob heads, half a quanta each, two heads worth per type
        for (String head : HOSTILE_HEADS) {
            entries.add(entry(head).set(APTH.QNTA, OPS.ADD, 0.5f).max(APTH.QNTA, 1f));
        }

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

        //Resource blocks: raw power at the cost of consistency
        entries.add(resourceBlock("minecraft:copper_block", 0.15f, 1.5f, 0.15f));
        entries.add(resourceBlock("minecraft:iron_block", 0.25f, 2f, 0.25f));
        entries.add(resourceBlock("minecraft:gold_block", 0.5f, 3f, 0.5f));
        entries.add(resourceBlock("minecraft:emerald_block", 0.75f, 4f, 0.75f));
        entries.add(resourceBlock("minecraft:diamond_block", 1f, 5f, 1f));
        entries.add(resourceBlock("minecraft:netherite_block", 2f, 8f, 2f));

        //Multiplier, divisor, floor and ceiling examples
        entries.add(entry("minecraft:beacon").set(APTH.ETRN, OPS.MULT, 1.05f));
        entries.add(entry("minecraft:redstone_block").set(APTH.QNTA, OPS.MULT, 1.05f));
        entries.add(entry("minecraft:cobweb").set(APTH.QNTA, OPS.DIV, 0.9f));
        entries.add(entry("minecraft:respawn_anchor")
            .set(APTH.ETRN, OPS.MULT, 2f)
            .set(APTH.ARCN, OPS.CEIL, 10f));
        entries.add(entry("minecraft:sea_lantern").set(APTH.QNTA, OPS.FLR, 5f));

        return entries;
    }

    private static BlockEnchantingStatsJsonConfig entry(String blockName) {
        return new BlockEnchantingStatsJsonConfig(blockName);
    }

    private static BlockEnchantingStatsJsonConfig adder(String blockName,
                                                        float eterna, float quanta, float arcana,
                                                        float maxEterna, float maxQuanta, float maxArcana) {
        BlockEnchantingStatsJsonConfig config = entry(blockName);
        if (eterna != 0f) config.set(APTH.ETRN, OPS.ADD, eterna);
        if (quanta != 0f) config.set(APTH.QNTA, OPS.ADD, quanta);
        if (arcana != 0f) config.set(APTH.ARCN, OPS.ADD, arcana);
        if (maxEterna > 0f) config.max(APTH.ETRN, maxEterna);
        if (maxQuanta > 0f) config.max(APTH.QNTA, maxQuanta);
        if (maxArcana > 0f) config.max(APTH.ARCN, maxArcana);
        return config;
    }

    private static BlockEnchantingStatsJsonConfig resourceBlock(String blockName, float eterna,
                                                                float maxEterna, float subQuanta) {
        return entry(blockName)
            .set(APTH.ETRN, OPS.ADD, eterna)
            .max(APTH.ETRN, maxEterna)
            .set(APTH.QNTA, OPS.SUB, subQuanta);
    }
}
