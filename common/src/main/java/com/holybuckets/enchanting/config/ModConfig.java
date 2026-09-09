package com.holybuckets.enchanting.config;

import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.config.json.BlockEnchantingStatsJsonConfig;
import com.holybuckets.enchanting.config.json.EnchantingTableJsonConfig;
import com.holybuckets.enchanting.config.model.BlockEnchantingStats;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import net.minecraft.world.level.block.Block;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.util.Collection;

public class ModConfig {

    private static final String CLASS_ID = "012";
    private static ModConfig INSTANCE;

    private EnchantingTableJsonConfig enchantingBlockPower;
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


    public EnchantingTableJsonConfig getEnchantingBlockPower() {
        return enchantingBlockPower;
    }

    public Collection<EnchantingTableJsonConfig.BlockPower> getBlockPowers() {
        return enchantingBlockPower == null ? java.util.List.of() : enchantingBlockPower.getAll().values();
    }

    @Nullable
    public EnchantingTableJsonConfig.BlockPower getBlockPower(String blockId) {
        return enchantingBlockPower == null ? null : enchantingBlockPower.get(blockId);
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
        return enchantingBlockPower == null
            ? EnchantingTierCaps.getDefault(tier) : enchantingBlockPower.getTierCaps(tier);
    }


    private void onBeforeServerStarted() {
        EnchantingConfig activeConfig = Balm.getConfig().getActiveConfig(EnchantingConfig.class);
        String configPath = activeConfig.enchantingBlockPowerConfig;

        File configFile = new File(configPath);
        File defaultConfigFile = new File(EnchantingTableJsonConfig.DEF_CONFIG_FILE_PATH);

        String json = HBUtil.FileIO.loadJsonConfigs(
            configFile,
            defaultConfigFile,
            EnchantingTableJsonConfig.buildDefaultConfig()
        );

        try {
            this.enchantingBlockPower = new EnchantingTableJsonConfig(json);
        } catch (RuntimeException e) {
            String msg = String.format(
                "Failed to parse user enchanting block-power config JSON: %s. Error:\n %s.\n\nDefault configs will be applied",
                configFile.getAbsolutePath(), e.getCause());
            LoggerProject.logError(CLASS_ID + "002", msg);
            this.enchantingBlockPower = new EnchantingTableJsonConfig(
                EnchantingTableJsonConfig.buildDefaultConfig().serialize());
        }

        resolveBlockStats();

        LoggerProject.logInfo(CLASS_ID + "001",
            "Parsed " + enchantingBlockPower.size() + " enchanting block-power entrie(s)");
    }

    /** Turns configured block names into Blocks; the registry is populated by server start. */
    private void resolveBlockStats() {
        blockStats.clear();
        int skipped = 0;

        for (BlockEnchantingStatsJsonConfig config : enchantingBlockPower.getBlockStatConfigs()) {
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
}
