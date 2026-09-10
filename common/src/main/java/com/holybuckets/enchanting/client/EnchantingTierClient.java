package com.holybuckets.enchanting.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.enchanting.LoggerProject;
import com.holybuckets.enchanting.config.json.EnchantingTableJsonConfig;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;
import com.holybuckets.foundation.client.ClientEventRegistrar;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.holybuckets.foundation.event.custom.SimpleMessageEvent;

/**
 * Client side cache of the tier settings for the enchanting table currently open.
 * <p>
 * The client menu is built with ContainerLevelAccess.NULL and never learns the block position, so
 * the server sends the tier's caps as JSON when the table is opened.
 */
public class EnchantingTierClient {

    public static final String MESSAGE_ID = "hbs_enchanting_tier";
    public static final String PAGE_MESSAGE_ID = "hbs_enchanting_page";
    public static final String LEDGER_MESSAGE_ID = "hbs_enchanting_ledger";
    private static final String CLASS_ID = "015";

    private static EnchantingTierCaps caps = EnchantingTierCaps.getDefault(EnchantingTierCaps.TIER_NORMAL);

    private EnchantingTierClient() {}

    public static void init(ClientEventRegistrar registrar) {
        registrar.registerOnSimpleMessage(MESSAGE_ID, EnchantingTierClient::onTierMessage);
        registrar.registerOnSimpleMessage(PAGE_MESSAGE_ID, EnchantingTierClient::onPageMessage);
        registrar.registerOnSimpleMessage(LEDGER_MESSAGE_ID, EnchantingTierClient::onLedgerMessage);
    }

    private static void onTierMessage(SimpleMessageEvent event) {
        try {
            caps = EnchantingTableJsonConfig.fromJson(event.getContent());
        } catch (RuntimeException e) {
            LoggerProject.logError(CLASS_ID + "001",
                "Could not read enchanting tier payload: " + event.getContent());
        }
    }

    private static String page = "";

    private static int rerollsRemaining = 0;

    //Payload is "page/total/remaining"
    private static void onPageMessage(SimpleMessageEvent event) {
        String[] parts = event.getContent().split("/");
        page = parts.length >= 2 ? parts[0] + "/" + parts[1] : "";
        rerollsRemaining = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
    }

    public static int getRerollsRemaining() {
        return rerollsRemaining;
    }

    //Net contribution per stat, in the order the server sent them
    private static final Map<Integer, List<LedgerEntry>> ledger = new LinkedHashMap<>();

    public record LedgerEntry(Block block, float value) {}

    private static void onLedgerMessage(SimpleMessageEvent event) {
        ledger.clear();
        for (JsonElement element : JsonParser.parseString(event.getContent()).getAsJsonArray()) {
            JsonObject obj = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.tryParse(obj.get("b").getAsString());
            Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
            if (block == null) continue;

            ledger.computeIfAbsent(obj.get("s").getAsInt(), k -> new ArrayList<>())
                .add(new LedgerEntry(block, obj.get("v").getAsFloat()));
        }
    }

    /** Which blocks moved the given stat, and by how much. */
    public static List<LedgerEntry> getLedger(int stat) {
        return ledger.getOrDefault(stat, List.of());
    }

    /** Reroll page as "n/total"; empty when there is no active session. */
    public static String getPage() {
        return page;
    }

    public static EnchantingTierCaps getCaps() {
        return caps;
    }
}
