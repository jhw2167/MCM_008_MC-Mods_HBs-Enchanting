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

    private final Map<String, BlockPower> powerMap;

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


    @Override
    public String serialize() {
        JsonObject root = new JsonObject();
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

        public BlockPower(String block, int maxCount, int maxPower) {
            this.block = block == null ? "" : block;
            this.maxCount = maxCount;
            this.maxPower = maxPower;
        }

        public String getBlock() { return block; }
        public int getMaxCount() { return maxCount; }
        public int getMaxPower() { return maxPower; }

        public JsonObject serialize() {
            JsonObject obj = new JsonObject();
            obj.addProperty("block", block);
            obj.addProperty("maxCount", maxCount);
            obj.addProperty("maxPower", maxPower);
            return obj;
        }

        public static BlockPower deserialize(JsonObject obj) {
            String block = obj.has("block") ? obj.get("block").getAsString() : "";
            int maxCount = obj.has("maxCount") ? obj.get("maxCount").getAsInt() : 0;
            int maxPower = obj.has("maxPower") ? obj.get("maxPower").getAsInt() : 0;
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
