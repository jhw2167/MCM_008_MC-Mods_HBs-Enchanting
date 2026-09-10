package com.holybuckets.enchanting.config.model;

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
            case TIER_COPPER -> 4;
            case TIER_NETHERITE -> 10;
            default -> 8;
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
}
