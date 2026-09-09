package com.holybuckets.enchanting;


import com.holybuckets.foundation.event.EventRegistrar;
import com.holybuckets.enchanting.config.EnchantingConfig;
import com.holybuckets.enchanting.block.ModBlocks;
import com.holybuckets.enchanting.block.be.BlockEntityTypes;
import com.holybuckets.enchanting.config.ModConfig;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.blay09.mods.balm.api.event.server.ServerStartingEvent;

/**
 * Main instance of the mod, initialize this class statically via commonClass
 * This class will init all major Manager instances and events for the mod
 */
public class EnchantingMain {
    private static boolean DEV_MODE = false;;
    private static EnchantingConfig CONFIG;
    public static EnchantingMain INSTANCE;

    public EnchantingMain()
    {
        super();
        INSTANCE = this;
        init();
        // LoggerProject.logInit( "001000", this.getClass().getName() ); // Uncomment if you have a logging system in place
    }

    private void init()
    {

        /*
        Proxy for external APIs which are platform dependent
        this.portalApi = (PortalApi) Balm.platformProxy()
            .withFabric("com.holybuckets.challengetemple.externalapi.FabricPortalApi")
            .withForge("com.holybuckets.challengetemple.externalapi.ForgePortalApi")
            .build();
            */

        //Events
        EventRegistrar registrar = EventRegistrar.getInstance();
        //ChallengeBlockBehavior.init(registrar);

        ModConfig.init(registrar);

        //Apotheosis resets the enchanting table block entity type's valid blocks during its own
        //setup, which drops the copper table; put it back once every level is loading.
        registrar.registerOnLevelLoad(e -> BlockEntityTypes.addValidBlock(
            BlockEntityType.ENCHANTING_TABLE, ModBlocks.copperEnchantingTable));

        //register local events
        registrar.registerOnBeforeServerStarted(this::onServerStarting);

    }

    private void onServerStarting(ServerStartingEvent e) {
        //CONFIG = Balm.getConfig().getActiveConfig(TemplateConfig.class);
        //this.DEV_MODE = CONFIG.devMode;
        this.DEV_MODE = false;
    }


}
