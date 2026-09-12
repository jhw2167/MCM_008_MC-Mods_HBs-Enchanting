package com.holybuckets.enchanting.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.config.json.BlockEnchantingStatsJsonConfig;
import com.holybuckets.enchanting.config.json.EnchantingTableJsonConfig;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.foundation.modelInterface.IStringSerializable;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;
import java.util.Set;

public class ModConfig {

    private static final String CLASS_ID = "012";
    private static ModConfig INSTANCE;

    private final Map<Integer, EnchantingTierCaps> tierCaps = new HashMap<>();
    private final Map<Block, BlockEnchantingStats> blockStats = new HashMap<>();

    /** Enchantments that may never share an item, recorded in both directions. */
    private final Multimap<Enchantment, Enchantment> exclusiveEnchants = HashMultimap.create();

    public static ModConfig getInstance() {
        if (INSTANCE == null) INSTANCE = new ModConfig();
        return INSTANCE;
    }

    private ModConfig() {}

    public static void init(EventRegistrar registrar) {
        INSTANCE = ModConfig.getInstance();
        registrar.registerOnBeforeServerStarted(ModConfig::onBeforeServerStarted, EventPriority.High);
        registrar.registerOnServerStopped(ModConfig::onServerStopped, EventPriority.Low);
    }



    /** Resolved once the block registry is available; keyed by Block so lookups skip the registry. */
    @Nullable
    public BlockEnchantingStats getBlockStats(Block block) {
        return blockStats.get(block);
    }

    public boolean hasBlockStats(Block block) {
        return blockStats.containsKey(block);
    }

    //certain types of blocks have many variants, but only 1 should be allowed, like candles
    public boolean isFirstOfUniqueBlockType(Set<Block> uniqueBlockTypes, Block block) {
        if(!isMutualExclBlock(block)) return true;
        if(uniqueBlockTypes.contains(block)) return false;

        Set<Block> mutexBlocks = BlockEnchantingStats.getMutualExclBlocks(block);
        uniqueBlockTypes.addAll(mutexBlocks);
        return true;
    }

    private static boolean isMutualExclBlock(Block block) {
        return BlockEnchantingStats.isMutualExclBlock(block);
    }

    public EnchantingTierCaps getTierCaps(int tier) {
        return tierCaps.getOrDefault(tier, EnchantingTierCaps.getDefault(tier));
    }



    /** True when the two enchantments are configured as mutually exclusive. */
    public boolean isExclusive(Enchantment a, Enchantment b) {
        return exclusiveEnchants.containsEntry(a, b);
    }

    public Multimap<Enchantment, Enchantment> getExclusiveEnchants() {
        return exclusiveEnchants;
    }

    private void loadExclusiveEnchants(List<String> pairs) {
        exclusiveEnchants.clear();
        if (pairs == null) return;

        for (String pair : pairs) {
            String[] names = pair.split(",");
            if (names.length != 2) {
                LoggerProject.logError(CLASS_ID + "005", "Exclusive enchantment entry needs two ids: " + pair);
                continue;
            }

            Enchantment first = findEnchantment(names[0].trim());
            Enchantment second = findEnchantment(names[1].trim());
            if (first == null || second == null || first == second) continue;

            exclusiveEnchants.put(first, second);
            exclusiveEnchants.put(second, first);
        }

        LoggerProject.logInfo(CLASS_ID + "006",
            "Loaded " + exclusiveEnchants.size() / 2 + " exclusive enchantment pair(s)");
    }

    @Nullable
    private Enchantment findEnchantment(String name) {
        ResourceLocation id = ResourceLocation.tryParse(name);
        Enchantment enchantment = id == null ? null : BuiltInRegistries.ENCHANTMENT.get(id);
        if (enchantment == null) {
            LoggerProject.logError(CLASS_ID + "007", "Unknown enchantment in exclusive config: " + name);
        }
        return enchantment;
    }

    private void onBeforeServerStarted() {
        EnchantingConfig activeConfig = Balm.getConfig().getActiveConfig(EnchantingConfig.class);
        String configPath = activeConfig.enchantingBlockPowerConfig;

        File configFile = new File(configPath);
        File defaultConfigFile = new File(EnchantingTableJsonConfig.DEF_CONFIG_FILE_PATH);

        String json = HBUtil.FileIO.loadJsonConfigs(configFile, defaultConfigFile, defaultJsonConfig());

        try {
            load(JsonParser.parseString(json).getAsJsonObject());
        } catch (RuntimeException e) {
            String msg = String.format(
                "Failed to parse enchanting config JSON: %s. Error:\n %s.\n\nDefault configs will be applied",
                configFile.getAbsolutePath(), e.getCause());
            LoggerProject.logError(CLASS_ID + "002", msg);
            load(JsonParser.parseString(defaultJsonConfig().serialize()).getAsJsonObject());
        }

        LoggerProject.logInfo(CLASS_ID + "001",
            "Loaded " + tierCaps.size() + " enchanting table tier(s)");

        loadExclusiveEnchants(activeConfig.exclusivePairs);

        BlockEnchantingStats.onServerStarted();
    }

    private void load(JsonObject root) {
        List<EnchantingTableJsonConfig> tables = EnchantingTableJsonConfig.parse(root);
        if (tables.isEmpty()) {
            LoggerProject.logWarning(CLASS_ID + "004", "Config has no '"
                + EnchantingTableJsonConfig.ROOT_KEY + "' entries; applying tier defaults");
            tables = EnchantingTableJsonConfig.defaultConfig();
        }

        tierCaps.clear();
        for (EnchantingTableJsonConfig entry : tables) {
            tierCaps.put(entry.getTier(), entry.getTierCaps());
        }

        List<BlockEnchantingStatsJsonConfig> blocks = BlockEnchantingStatsJsonConfig.parse(root);
        if (blocks.isEmpty()) {
            LoggerProject.logWarning(CLASS_ID + "005", "Config has no '"
                + BlockEnchantingStatsJsonConfig.ROOT_KEY + "' entries; applying block defaults");
            blocks = BlockEnchantingStatsJsonConfig.defaultConfig();
        }

        deserializeBlockStats(blocks);
    }

    /** Turns configured block names into Blocks; the registry is populated by server start. */
    private void deserializeBlockStats(List<BlockEnchantingStatsJsonConfig> configs) {
        blockStats.clear();
        int skipped = 0;

        for (BlockEnchantingStatsJsonConfig config : configs) {
            BlockEnchantingStats stats = BlockEnchantingStatsJsonConfig.deserialize(config);
            if (stats == null) {
                skipped++;
                continue;
            }
            blockStats.put(stats.getBlock(), stats);
        }

        LoggerProject.logInfo(CLASS_ID + "003",
            "Resolved " + blockStats.size() + " block enchanting stat entrie(s), skipped " + skipped);
    }

    private void onServerStopped() {
        INSTANCE = null;
    }


    private static void onBeforeServerStarted(ServerStartingEvent event) {
        getInstance().onBeforeServerStarted();
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        getInstance().onServerStopped();
    }


    private static IStringSerializable defaultJsonConfig() {
        return new IStringSerializable() {
            @Override
            public String serialize() {
                JsonObject root = new JsonObject();
                root.add(EnchantingTableJsonConfig.ROOT_KEY,
                    EnchantingTableJsonConfig.toJsonArray(EnchantingTableJsonConfig.defaultConfig()));
                root.add(BlockEnchantingStatsJsonConfig.ROOT_KEY,
                    BlockEnchantingStatsJsonConfig.toJsonArray(BlockEnchantingStatsJsonConfig.defaultConfig()));

                return new GsonBuilder().setPrettyPrinting().create().toJson(root);
            }

            @Override
            public void deserialize(String jsonString) {
                // No-op for default config
            }
        };
    }

}
