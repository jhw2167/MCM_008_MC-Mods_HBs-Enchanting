package com.holybuckets.enchanting.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.holybuckets.foundation.modelInterface.IStringSerializable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnchantingBlockPower implements IStringSerializable {

    public static final String DEF_CONFIG_FILE_PATH = "config/HBsEnchantingOverhaulConfig.json";
    public static final String ROOT_KEY = "enchantingBlockPower";

    public static final String SEARCH_RADIUS_KEY = "searchRadius";
    public static final String STANDARD_MAX_POWER_KEY = "standardTableMaxPower";
    public static final String COPPER_MAX_POWER_KEY = "copperTableMaxPower";

    public static final int DEF_SEARCH_RADIUS = 16;
    public static final int DEF_STANDARD_MAX_POWER = 0;
    public static final int DEF_COPPER_MAX_POWER = 15;

    public static final int MAX_SEARCH_RADIUS = 32;

    private final Map<String, BlockPower> powerMap;

    private int searchRadius = DEF_SEARCH_RADIUS;
    private int standardTableMaxPower = DEF_STANDARD_MAX_POWER;
    private int copperTableMaxPower = DEF_COPPER_MAX_POWER;

    public EnchantingBlockPower(List<BlockPower> entries) {
        this.powerMap = new LinkedHashMap<>();
        if (entries != null) {
            entries.forEach(e -> powerMap.put(e.getBlock(), e));
        }
    }

    public EnchantingBlockPower(String jsonString) {
        this(List.of());
        deserialize(jsonString);
    }

    public Map<String, BlockPower> getAll() {
        return Collections.unmodifiableMap(powerMap);
    }

    public List<BlockPower> getAllAsList() {
        return List.copyOf(powerMap.values());
    }

    @Nullable
    public BlockPower get(String blockId) {
        return powerMap.get(blockId);
    }

    public boolean has(String blockId) {
        return powerMap.containsKey(blockId);
    }

    public int size() {
        return powerMap.size();
    }

    public void remove(String blockId) {
        powerMap.remove(blockId);
    }

    /** Radius, in blocks, that an enchanting table scans for power providing blocks. */
    public int getSearchRadius() {
        return searchRadius;
    }

    /** Hard cap on the enchanting power of a standard enchanting table; 0 or less means uncapped. */
    public int getStandardTableMaxPower() {
        return standardTableMaxPower;
    }

    /** Hard cap on the enchanting power of a copper enchanting table; 0 or less means uncapped. */
    public int getCopperTableMaxPower() {
        return copperTableMaxPower;
    }


    @Override
    public String serialize() {
        JsonObject root = new JsonObject();
        root.addProperty(SEARCH_RADIUS_KEY, searchRadius);
        root.addProperty(STANDARD_MAX_POWER_KEY, standardTableMaxPower);
        root.addProperty(COPPER_MAX_POWER_KEY, copperTableMaxPower);

        JsonArray entries = new JsonArray();
        for (BlockPower bp : powerMap.values()) {
            entries.add(bp.serialize());
        }
        root.add(ROOT_KEY, entries);

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(root);
    }

    @Override
    public void deserialize(String jsonString) throws RuntimeException {
        if (jsonString == null || jsonString.isBlank()) return;
        try {
            JsonElement parsed = JsonParser.parseString(jsonString);
            if (!parsed.isJsonObject()) {
                throw new RuntimeException("Expected a JSON object at the root of HBsEnchantingOverhaulConfig.json");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (!root.has(ROOT_KEY) || !root.get(ROOT_KEY).isJsonArray()) {
                throw new RuntimeException("Root JSON object is missing required array '" + ROOT_KEY + "'");
            }

            if (root.has(SEARCH_RADIUS_KEY)) {
                this.searchRadius = Math.max(0, Math.min(MAX_SEARCH_RADIUS, root.get(SEARCH_RADIUS_KEY).getAsInt()));
            }
            if (root.has(STANDARD_MAX_POWER_KEY)) {
                this.standardTableMaxPower = root.get(STANDARD_MAX_POWER_KEY).getAsInt();
            }
            if (root.has(COPPER_MAX_POWER_KEY)) {
                this.copperTableMaxPower = root.get(COPPER_MAX_POWER_KEY).getAsInt();
            }

            parseArray(root.getAsJsonArray(ROOT_KEY));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON format for EnchantingBlockPower", e);
        }
    }

    private void parseArray(JsonArray array) {
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            BlockPower bp = BlockPower.deserialize(element.getAsJsonObject());
            if (bp.getBlock() != null && !bp.getBlock().isEmpty()) {
                powerMap.put(bp.getBlock(), bp);
            }
        }
    }


    public static EnchantingBlockPower buildDefaultConfig() {
        List<BlockPower> entries = new ArrayList<>();
        entries.add(new BlockPower("minecraft:bookshelf", 15, 15));
        entries.add(new BlockPower("apotheosis:sea_shelf", 10, 25));
        entries.add(new BlockPower("apotheosis:rectifier", 4, 12));
        entries.add(new BlockPower("apotheosis:rectifier_t2", 2, 18));
        entries.add(new BlockPower("apotheosis:rectifier_t3", 1, 24));
        entries.add(new BlockPower("apotheosis:hellshelf", 12, 18));
        entries.add(new BlockPower("apotheosis:infused_hellshelf", 6, 22));
        return new EnchantingBlockPower(entries);
    }


    public static class BlockPower {

        private final String block;
        private final int maxCount;
        private final int maxPower;
        private final float power;

        public BlockPower(String block, int maxCount, int maxPower) {
            this(block, maxCount, maxPower, maxCount > 0 ? (float) maxPower / maxCount : 0f);
        }

        public BlockPower(String block, int maxCount, int maxPower, float power) {
            this.block = block == null ? "" : block;
            this.maxCount = maxCount;
            this.maxPower = maxPower;
            this.power = power;
        }

        public String getBlock() { return block; }

        /** Maximum number of this block that may contribute to a single table. */
        public int getMaxCount() { return maxCount; }

        /** Maximum total power this block type may contribute to a single table. */
        public int getMaxPower() { return maxPower; }

        /** Power contributed per individual block. */
        public float getPower() { return power; }

        /** Total power contributed by the given number of this block, with both caps applied. */
        public float contribution(int count) {
            int counted = Math.min(count, maxCount);
            return Math.min(counted * power, maxPower);
        }

        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("block", block);
            obj.addProperty("maxCount", maxCount);
            obj.addProperty("maxPower", maxPower);
            obj.addProperty("power", power);
            return obj;
        }

        public static BlockPower deserialize(JsonObject obj) {
            String block = obj.has("block") ? obj.get("block").getAsString() : "";
            int maxCount = obj.has("maxCount") ? obj.get("maxCount").getAsInt() : 0;
            int maxPower = obj.has("maxPower") ? obj.get("maxPower").getAsInt() : 0;
            if (obj.has("power")) {
                return new BlockPower(block, maxCount, maxPower, obj.get("power").getAsFloat());
            }
            return new BlockPower(block, maxCount, maxPower);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BlockPower other)) return false;
            return block != null && block.equals(other.block);
        }

        @Override
        public int hashCode() {
            return block != null ? block.hashCode() : 0;
        }
    }
}
