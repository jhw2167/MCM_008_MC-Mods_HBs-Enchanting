package com.holybuckets.enchanting.config.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Per tier settings for an enchanting table: how far it searches for stat providing blocks, and
 * the ceiling it puts on each of the three enchanting stats.
 * <p>
 * Every field is an Integer so a partial config object defaults the values it leaves out.
 */
public class EnchantingTierCaps {

    public static final int TIER_COPPER = 0;
    public static final int TIER_NORMAL = 1;
    public static final int TIER_NETHERITE = 2;

    public static final int MAX_RADIUS = 32;

    private final int tier;
    private final int radius;
    private final int eternaMax;
    private final int quantaMax;
    private final int arcanaMax;

    public EnchantingTierCaps(Integer tier, Integer radius, Integer eternaMax, Integer quantaMax, Integer arcanaMax) {
        this.tier = tier == null ? TIER_NORMAL : tier;
        this.radius = Math.min(radius == null ? defaultRadius(this.tier) : radius, MAX_RADIUS);
        this.eternaMax = eternaMax == null ? defaultEterna(this.tier) : eternaMax;
        this.quantaMax = quantaMax == null ? defaultQuanta(this.tier) : quantaMax;
        this.arcanaMax = arcanaMax == null ? defaultArcana(this.tier) : arcanaMax;
    }

    public int getTier() { return tier; }
    public int getRadius() { return radius; }
    public int getEternaMax() { return eternaMax; }
    public int getQuantaMax() { return quantaMax; }
    public int getArcanaMax() { return arcanaMax; }

    public float capEterna(float value) { return cap(value, eternaMax); }
    public float capQuanta(float value) { return cap(value, quantaMax); }
    public float capArcana(float value) { return cap(value, arcanaMax); }

    private static float cap(float value, int max) {
        if (max <= 0) return value;
        return Math.min(value, max);
    }

    public static EnchantingTierCaps getDefault(int tier) {
        return new EnchantingTierCaps(tier, null, null, null, null);
    }

    private static int defaultRadius(int tier) {
        return switch (tier) {
            case TIER_COPPER -> 10;
            case TIER_NETHERITE -> 14;
            default -> 12;
        };
    }

    private static int defaultEterna(int tier) {
        return switch (tier) {
            case TIER_COPPER -> 10;
            case TIER_NETHERITE -> 50;
            default -> 30;
        };
    }

    private static int defaultQuanta(int tier) {
        return switch (tier) {
            case TIER_COPPER -> 20;
            case TIER_NETHERITE -> 50;
            default -> 60;
        };
    }

    private static int defaultArcana(int tier) {
        return switch (tier) {
            case TIER_COPPER -> 5;
            case TIER_NETHERITE -> 50;
            default -> 25;
        };
    }

    public JsonObject serialize() {
        JsonObject obj = new JsonObject();
        obj.addProperty("tier", tier);
        obj.addProperty("radius", radius);
        obj.addProperty("eternaMax", eternaMax);
        obj.addProperty("quantaMax", quantaMax);
        obj.addProperty("arcanaMax", arcanaMax);
        return obj;
    }

    public static EnchantingTierCaps deserialize(JsonObject obj) {
        return new EnchantingTierCaps(
            asInt(obj, "tier"),
            asInt(obj, "radius"),
            asInt(obj, "eternaMax"),
            asInt(obj, "quantaMax"),
            asInt(obj, "arcanaMax"));
    }

    public static EnchantingTierCaps fromJson(String json) {
        if (json == null || json.isBlank()) return getDefault(TIER_NORMAL);
        return deserialize(JsonParser.parseString(json).getAsJsonObject());
    }

    public String toJson() {
        return serialize().toString();
    }

    private static Integer asInt(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsInt() : null;
    }
}
