package com.muhofy.chestmemory.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.muhofy.chestmemory.ChestMemoryMod;
import com.muhofy.chestmemory.data.ChestRecord;
import com.muhofy.chestmemory.data.ChestItem;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ChestStorage {

    private static final ChestStorage INSTANCE = new ChestStorage();
    public static ChestStorage getInstance() { return INSTANCE; }
    private ChestStorage() {}

    private final Gson              gson          = new GsonBuilder().setPrettyPrinting().create();
    private final List<ChestRecord> chests        = new ArrayList<>();
    private final List<String>      searchHistory = new ArrayList<>();
    private static final int        MAX_HISTORY   = 8;
    private String currentWorld = null;

    // ── SearchResult ──────────────────────────────────────────────────────
    public static class SearchResult {
        public final ChestRecord     chest;
        public final List<ChestItem> matchedItems;
        public final int             totalCount;
        public final double          distance;

        public SearchResult(ChestRecord chest, List<ChestItem> matched, double distance) {
            this.chest        = chest;
            this.matchedItems = matched;
            this.totalCount   = matched.stream().mapToInt(ChestItem::getCount).sum();
            this.distance     = distance;
        }

        public ChestItem firstItem() {
            return matchedItems.isEmpty() ? null : matchedItems.get(0);
        }
    }

    // ── SearchData (disk I/O için) ────────────────────────────────────────
    public record SearchData(List<ChestRecord> chests, List<String> history) {}

    // ── Init / World ──────────────────────────────────────────────────────
    public void init() {
        ChestMemoryMod.LOGGER.info("[ChestStorage] Initialized.");
    }

    public void loadWorld(String worldName) {
        if (worldName.equals(currentWorld)) return;
        currentWorld = worldName;
        chests.clear();
        searchHistory.clear();

        SearchData data = readFromDisk(worldName);
        chests.addAll(data.chests());
        searchHistory.addAll(data.history());

        ChestMemoryMod.LOGGER.info("[ChestStorage] Loaded {} chests, {} history entries for world '{}'.",
                chests.size(), searchHistory.size(), worldName);
    }

    public void unloadWorld() {
        currentWorld = null;
        chests.clear();
        searchHistory.clear();
    }

    // ── CRUD ──────────────────────────────────────────────────────────────
    public ChestRecord addOrUpdate(int x, int y, int z, String dimension,
                                   List<ChestItem> items, boolean isDouble) {
        ChestRecord existing = getAt(x, y, z, dimension);
        if (existing != null) {
            existing.setItems(items);
            existing.setDouble(isDouble);
            existing.touchUpdated();
            save();
            return existing;
        }
        ChestRecord rec = new ChestRecord(x, y, z, dimension, isDouble);
        rec.setItems(items);
        chests.add(rec);
        save();
        return rec;
    }

    public void delete(String id) {
        chests.removeIf(c -> c.getId().equals(id));
        save();
    }

    public void rename(String id, String newName) {
        chests.stream()
              .filter(c -> c.getId().equals(id))
              .findFirst()
              .ifPresent(c -> { c.setCustomName(newName); save(); });
    }

    // ── Search History ────────────────────────────────────────────────────
    public List<String> getSearchHistory() {
        return Collections.unmodifiableList(searchHistory);
    }

    public void addToHistory(String query) {
        if (query == null || query.isBlank()) return;
        query = query.trim();
        searchHistory.remove(query);
        searchHistory.add(0, query);
        if (searchHistory.size() > MAX_HISTORY)
            searchHistory.subList(MAX_HISTORY, searchHistory.size()).clear();
        save();
    }

    public void removeFromHistory(String query) {
        searchHistory.remove(query);
        save();
    }

    // ── Queries ───────────────────────────────────────────────────────────
    public ChestRecord getAt(int x, int y, int z, String dimension) {
        return chests.stream()
                     .filter(c -> c.getX() == x && c.getY() == y && c.getZ() == z
                                  && c.getDimension().equals(dimension))
                     .findFirst().orElse(null);
    }

    public List<ChestRecord> getAll() {
        return Collections.unmodifiableList(chests);
    }

    public List<ChestRecord> getByDimension(String dimension) {
        return chests.stream()
                     .filter(c -> c.isInDimension(dimension))
                     .collect(Collectors.toList());
    }

    public List<SearchResult> searchItems(String query, String activeDimension, double px, double pz) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.toLowerCase(Locale.ROOT).trim();

        List<SearchResult> results = new ArrayList<>();
        for (ChestRecord chest : chests) {
            List<ChestItem> matched = new ArrayList<>();
            for (ChestItem item : chest.getItems()) {
                boolean nameMatch = item.getDisplayName() != null
                        && item.getDisplayName().toLowerCase(Locale.ROOT).contains(q);
                boolean idMatch   = item.getItemId() != null
                        && item.getItemId().toLowerCase(Locale.ROOT).contains(q);
                if (nameMatch || idMatch) matched.add(item);
            }
            if (!matched.isEmpty())
                results.add(new SearchResult(chest, matched, chest.distanceTo(px, pz)));
        }

        results.sort(Comparator
                .<SearchResult, Boolean>comparing(r -> !r.chest.isInDimension(activeDimension))
                .thenComparingDouble(r -> r.distance));

        return results;
    }

    public String getDisplayName(ChestRecord rec) {
        int idx = chests.indexOf(rec) + 1;
        return rec.getDisplayName(idx);
    }

    // ── Disk I/O ──────────────────────────────────────────────────────────
    private void save() {
        if (currentWorld == null) return;
        writeToDisk(currentWorld, chests);
    }

    private Path getFilePath(String worldName) {
        String safe = worldName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        return FabricLoader.getInstance().getConfigDir()
                           .resolve("stashfinder").resolve(safe).resolve("chests.json");
    }

    private SearchData readFromDisk(String worldName) {
        Path file = getFilePath(worldName);
        if (!Files.exists(file)) return new SearchData(new ArrayList<>(), new ArrayList<>());
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root   = JsonParser.parseReader(r).getAsJsonObject();
            Type listType     = new TypeToken<List<ChestRecord>>(){}.getType();
            Type strType      = new TypeToken<List<String>>(){}.getType();

            List<ChestRecord> loadedChests = root.has("chests")
                    ? gson.fromJson(root.get("chests"), listType) : new ArrayList<>();
            List<String> loadedHistory     = root.has("searchHistory")
                    ? gson.fromJson(root.get("searchHistory"), strType) : new ArrayList<>();

            return new SearchData(
                    loadedChests  != null ? loadedChests  : new ArrayList<>(),
                    loadedHistory != null ? loadedHistory : new ArrayList<>()
            );
        } catch (Exception e) {
            ChestMemoryMod.LOGGER.error("[ChestStorage] Failed to read chests.json — creating backup.", e);
            backupCorrupted(file);
            return new SearchData(new ArrayList<>(), new ArrayList<>());
        }
    }

    private void writeToDisk(String worldName, List<ChestRecord> data) {
        Path file = getFilePath(worldName);
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.add("chests", gson.toJsonTree(data));
            root.add("searchHistory", gson.toJsonTree(searchHistory));
            try (Writer w = Files.newBufferedWriter(file)) {
                gson.toJson(root, w);
            }
        } catch (IOException e) {
            ChestMemoryMod.LOGGER.error("[ChestStorage] Failed to write chests.json.", e);
        }
    }

    private void backupCorrupted(Path file) {
        try {
            Path bak = file.resolveSibling("chests.json.bak");
            Files.copy(file, bak, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(file);
            ChestMemoryMod.LOGGER.warn("[ChestStorage] Corrupted file backed up to chests.json.bak");
        } catch (IOException ex) {
            ChestMemoryMod.LOGGER.error("[ChestStorage] Backup failed.", ex);
        }
    }
}