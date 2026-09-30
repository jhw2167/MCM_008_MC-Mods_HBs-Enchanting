package com.holybuckets.enchanting.networking;

import com.holybuckets.enchanting.Constants;
import net.blay09.mods.balm.api.network.BalmNetworking;
import net.minecraft.resources.ResourceLocation;

public class ModNetworking {

    public static void init(BalmNetworking networking) {
        Handlers.init();
        networking.registerClientboundPacket(id(BlockStatsSyncMessage.LOCATION), BlockStatsSyncMessage.class,
            Codecs::encodeBlockStatsSync, Codecs::decodeBlockStatsSync, Handlers::handleBlockStatsSync);
    }

    private static ResourceLocation id(String name) {
        return new ResourceLocation(Constants.MOD_ID, name);
    }
}
