package com.holybuckets.enchanting.config;

import com.holybuckets.enchanting.Constants;
import net.blay09.mods.balm.api.config.reflection.Comment;
import net.blay09.mods.balm.api.config.reflection.Config;
import net.blay09.mods.balm.api.config.reflection.NestedType;

import java.util.Arrays;
import java.util.List;


@Config(Constants.MOD_ID)
public class EnchantingConfig {

    @Comment("devMode==true disables portal spawns so the player can build and save new challenges")
    public boolean devMode = false;
    @Comment("Where the loot rules json configuration can be found. This file determines what loot is available in each level of pool")
    public String lootRulesConfig = "config/challengeTempleslootRules.json";

    @Comment("File path to the enchanting block-power config. Defines which blocks contribute to enchantment power, their max stack count per altar, and the max power each contributes.")
    public String enchantingBlockPowerConfig = "config/HBsEnchantingOverhaulConfig.json";


    public static class SatelliteBlockConfig {

        @Comment("Satellite will not operate below this y level")
        public int minSatelliteWorkingHeight = 256;
    }


    public static class EnchantmentVarietyConfig {

        @Comment("Grants an additional Enchantment table reroll for an item for this many quanta in the table. default 15")
        public float quantaRerollRate = 15f;

        @Comment("Generally a fractional value that increases the number of enchantments and total enchantment levels that can appear on an item per each Eterna added to the table. Can be negative")
        public float enchantmentsPerItemEternaScaler = 0.25f;

        @Comment("Generally a fractional value that increases the number of enchantments and total enchantment levels that can appear on an item per each Quanta added to the table. Can be negative")
        public float enchantmentsPerItemQuantaScaler = 0.125f;

        @Comment("Generally a fractional value that increases the number of enchantments and total enchantment levels that can appear on an item per each Arcana added to the table. Can be negative")
        public float enchantmentsPerItemArcanaScaler = 0f;

        @Comment("Fractional value that treats an enchantment level as if it were a new enchantment in terms of enchantment combination weight. E.g. makes a Sharpness 5 tool less likely to include multiple enchantments on the weapon than Sharpness 2")
        public float enchantmentsPerLevelWeight = 0.5f;

        @NestedType(Float.class)
        @Comment("Combination weight charged for one enchantment of each rarity. Higher rates make it less likely to receive two rare enchantments at the same time. [common, uncommon, rare, very rare]")
        public List<Float> enchantmentRarityComboRates = Arrays.asList(1f, 2f, 4f, 6f);
    }




    @NestedType(String.class)
    @Comment("Pairs of enchantments that may never appear on the same item. Each entry is two enchantment ids separated by a comma, e.g. minecraft:sharpness,minecraft:smite")
    public List<String> exclusivePairs = Arrays.asList(
        "minecraft:silk_touch,minecraft:fortune",
        "minecraft:infinity,minecraft:mending"
    );

    public EnchantmentVarietyConfig enchantmentVarietyConfig = new EnchantmentVarietyConfig();
}