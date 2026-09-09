package com.holybuckets.enchanting.config.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.enchanting.config.model.EnchantingTierCaps;

import java.util.ArrayList;
import java.util.List;

/**
 * Json for a single enchanting table config - includes its radius, eterna, and quanta maxes
 */
public class EnchantingTableJsonConfig {

    public static final String DEF_CONFIG_FILE_PATH = "config/HBsEnchantingOverhaulConfig.json";
    public static final String ROOT_KEY = "enchantingTables";

    public static final String TIER_KEY = "tier";
    public static final String RADIUS_KEY = "radius";
    public static final String ETERNA_MAX_KEY = "eternaMax";
    public static final String QUANTA_MAX_KEY = "quantaMax";
    public static final String ARCANA_MAX_KEY = "arcanaMax";

    private final EnchantingTierCaps tierCaps;

    public EnchantingTableJsonConfig(EnchantingTierCaps tierCaps) {
        this.tierCaps = tierCaps;
    }

    public EnchantingTableJsonConfig(JsonObject obj) {
        this.tierCaps = new EnchantingTierCaps(
            asInt(obj, TIER_KEY),
            asInt(obj, RADIUS_KEY),
            asInt(obj, ETERNA_MAX_KEY),
            asInt(obj, QUANTA_MAX_KEY),
            asInt(obj, ARCANA_MAX_KEY));
    }

    public EnchantingTierCaps getTierCaps() {
        return tierCaps;
    }

    public int getTier() {
        return tierCaps.getTier();
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty(TIER_KEY, tierCaps.getTier());
        obj.addProperty(RADIUS_KEY, tierCaps.getRadius());
        obj.addProperty(ETERNA_MAX_KEY, tierCaps.getEternaMax());
        obj.addProperty(QUANTA_MAX_KEY, tierCaps.getQuantaMax());
        obj.addProperty(ARCANA_MAX_KEY, tierCaps.getArcanaMax());
        return obj;
    }

    /** One entry as a compact json string; this is what gets sent to the client. */
    public String toJson() {
        return serialize().toString();
    }

    public static EnchantingTierCaps fromJson(String json) {
        if (json == null || json.isBlank()) {
            return EnchantingTierCaps.getDefault(EnchantingTierCaps.TIER_NORMAL);
        }
        return new EnchantingTableJsonConfig(JsonParser.parseString(json).getAsJsonObject()).getTierCaps();
    }

    public static List<EnchantingTableJsonConfig> parse(JsonObject root) {
        List<EnchantingTableJsonConfig> entries = new ArrayList<>();
        if (!root.has(ROOT_KEY) || !root.get(ROOT_KEY).isJsonArray()) return entries;

        for (JsonElement element : root.getAsJsonArray(ROOT_KEY)) {
            if (!element.isJsonObject()) continue;
            entries.add(new EnchantingTableJsonConfig(element.getAsJsonObject()));
        }
        return entries;
    }

    public static JsonArray toJsonArray(List<EnchantingTableJsonConfig> entries) {
        JsonArray array = new JsonArray();
        for (EnchantingTableJsonConfig entry : entries) {
            array.add(entry.serialize());
        }
        return array;
    }

    /** One entry per tier, each at that tier's defaults. */
    public static List<EnchantingTableJsonConfig> defaultConfig() {
        List<EnchantingTableJsonConfig> entries = new ArrayList<>();
        for (int tier = EnchantingTierCaps.TIER_COPPER; tier <= EnchantingTierCaps.TIER_NETHERITE; tier++) {
            entries.add(new EnchantingTableJsonConfig(EnchantingTierCaps.getDefault(tier)));
        }
        return entries;
    }

    private static Integer asInt(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : null;
    }
}
