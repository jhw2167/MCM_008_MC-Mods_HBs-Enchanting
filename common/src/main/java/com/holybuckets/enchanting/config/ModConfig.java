package com.holybuckets.enchanting.config;

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

import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.File;

public class ModConfig {

    private static final String CLASS_ID = "012";
    private static ModConfig INSTANCE;

    private final Map<Integer, EnchantingTierCaps> tierCaps = new HashMap<>();
    private final Map<Block, BlockEnchantingStats> blockStats = new HashMap<>();

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

    public EnchantingTierCaps getTierCaps(int tier) {
        return tierCaps.getOrDefault(tier, EnchantingTierCaps.getDefault(tier));
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
    }

    /** Reads both arrays out of the config root and resolves what needs the registry. */
    private void load(JsonObject root) {
        tierCaps.clear();
        for (EnchantingTableJsonConfig entry : EnchantingTableJsonConfig.parse(root)) {
            tierCaps.put(entry.getTier(), entry.getTierCaps());
        }

        resolveBlockStats(BlockEnchantingStatsJsonConfig.parse(root));
    }

    /** Turns configured block names into Blocks; the registry is populated by server start. */
    private void resolveBlockStats(List<BlockEnchantingStatsJsonConfig> configs) {
        blockStats.clear();
        int skipped = 0;

        for (BlockEnchantingStatsJsonConfig config : configs) {
            BlockEnchantingStats stats = config.resolve();
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
