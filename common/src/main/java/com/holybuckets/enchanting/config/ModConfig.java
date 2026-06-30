package com.holybuckets.enchanting.config;

import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.foundation.HBUtil;
import com.holybuckets.foundation.event.EventRegistrar;
import net.blay09.mods.balm.api.Balm;
import net.blay09.mods.balm.api.event.EventPriority;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;
import net.blay09.mods.balm.api.event.server.ServerStoppedEvent;

import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;

public class ModConfig {

    private static final String CLASS_ID = "012";
    private static ModConfig INSTANCE;

    private EnchantingBlockPower enchantingBlockPower;

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


    public EnchantingBlockPower getEnchantingBlockPower() {
        return enchantingBlockPower;
    }

    public Collection<EnchantingBlockPower.BlockPower> getBlockPowers() {
        return enchantingBlockPower.getAll().values();
    }

    @Nullable
    public EnchantingBlockPower.BlockPower getBlockPower(String blockId) {
        return enchantingBlockPower.get(blockId);
    }


    private void onBeforeServerStarted() {
        EnchantingConfig activeConfig = Balm.getConfig().getActiveConfig(EnchantingConfig.class);
        String configPath = activeConfig.enchantingBlockPowerConfig;

        File configFile = new File(configPath);
        File defaultConfigFile = new File(EnchantingBlockPower.DEF_CONFIG_FILE_PATH);

        String json = HBUtil.FileIO.loadJsonConfigs(
            configFile,
            defaultConfigFile,
            EnchantingBlockPower.buildDefaultConfig()
        );

        try {
            this.enchantingBlockPower = new EnchantingBlockPower(json);
        } catch (RuntimeException e) {
            String msg = String.format(
                "Failed to parse user enchanting block-power config JSON: %s. Error:\n %s.\n\nDefault configs will be applied",
                configFile.getAbsolutePath(), e.getCause());
            LoggerProject.logError(CLASS_ID + "002", msg);
            this.enchantingBlockPower = new EnchantingBlockPower(
                EnchantingBlockPower.buildDefaultConfig().serialize());
        }

        LoggerProject.logInfo(CLASS_ID + "001",
            "Parsed " + enchantingBlockPower.size() + " enchanting block-power entrie(s)");
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
