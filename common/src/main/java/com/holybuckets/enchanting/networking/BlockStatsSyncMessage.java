package com.holybuckets.enchanting.networking;

import com.holybuckets.enchanting.config.json.BlockEnchantingStatsJsonConfig;
import com.holybuckets.foundation.HBUtil;
import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Sends the server's resolved block enchanting stats to a client on login, so tooltips and the
 * table screen show the configured values rather than the ones Apotheosis ships with.
 */
public class BlockStatsSyncMessage {

    public static final String LOCATION = "block_stats_sync";

    //Entries per packet, kept well under the 32KB packet ceiling
    private static final int MAX_ENTRIES = 16;

    final boolean replace;
    final String json;

    BlockStatsSyncMessage(boolean replace, String json) {
        this.replace = replace;
        this.json = json;
    }

    public static void createAndFire(Player player, List<BlockEnchantingStatsJsonConfig> entries) {
        if (player == null || entries.isEmpty()) return;

        for (int start = 0; start < entries.size(); start += MAX_ENTRIES) {
            int end = Math.min(start + MAX_ENTRIES, entries.size());

            JsonObject root = new JsonObject();
            root.add(BlockEnchantingStatsJsonConfig.ROOT_KEY,
                BlockEnchantingStatsJsonConfig.toJsonArray(new ArrayList<>(entries.subList(start, end))));

            HBUtil.NetworkUtil.serverSendToPlayer(player, new BlockStatsSyncMessage(start == 0, root.toString()));
        }
    }
}
