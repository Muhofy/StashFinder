package com.muhofy.chestmemory.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.muhofy.chestmemory.ChestMemoryMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class ChestStorage {

    // ── Sort Mode ─────────────────────────────────────────────────────────
    public enum SortMode {
        DISTANCE,   // 📍 en yakın
        DATE,       // 📅 en son indexlenen
        COUNT,      // 🔢 en dolu
        NAME        // 🔤 alfabetik
    }

    private static final ChestStorage INSTANCE = new ChestStorage();
    public static ChestStorage getInstance() { return INSTANCE; }
    private ChestStorage() {}

    private final Gson              gson          = new GsonBuilder().setPrettyPrinting().create();
    private final List<ChestRecord> chests        = new ArrayList<>();
    private final List<String>      searchHistory = new ArrayList<>();
    private static final int        MAX_HISTORY   = 8;
    private SortMode                sortMode      = SortMode.DISTANCE;
    private String                  currentWorld  = null;

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

    // ── SearchData ────────────────────────────────────────────────────────
    public record SearchData(List<ChestRecord> chests, List<String> history, String sortMode) {}

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
        try {
            sortMode = data.sortMode() != null
                    ? SortMode.valueOf(data.sortMode()) : SortMode.DISTANCE;
        } catch (IllegalArgumentException e) {
            sortMode = SortMode.DISTANCE;
        }

        ChestMemoryMod.LOGGER.info("[ChestStorage] Loaded {} chests for world '{}'.",
                chests.size(), worldName);
    }

    public void unloadWorld() {
        currentWorld = null;
        chests.clear();
        searchHistory.clear();
        sortMode = SortMode.DISTANCE;
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

    // ── Sort Mode ─────────────────────────────────────────────────────────
    public SortMode getSortMode() { return sortMode; }

    public void setSortMode(SortMode mode) {
        this.sortMode = mode;
        save();
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

    public List<SearchResult> searchItems(String query, String activeDimension,
                                          double px, double pz) {
        if (query == null || query.isBlank()) return List.of();
        String q = query.toLowerCase(Locale.ROOT).trim();

        List<SearchResult> results = new ArrayList<>();
        for (ChestRecord chest : chests) {
            for (ChestItem item : chest.getItems()) {
                boolean nameMatch = item.getDisplayName() != null
                        && item.getDisplayName().toLowerCase(Locale.ROOT).contains(q);
                boolean idMatch   = item.getItemId() != null
                        && item.getItemId().toLowerCase(Locale.ROOT).contains(q);
                if (nameMatch || idMatch)
                    results.add(new SearchResult(chest, List.of(item), chest.distanceTo(px, pz)));
            }
        }

        // Önce mevcut dimension, sonra diğerleri
        Comparator<SearchResult> dimCmp = Comparator
                .<SearchResult, Boolean>comparing(r -> !r.chest.isInDimension(activeDimension));

        Comparator<SearchResult> sortCmp = switch (sortMode) {
            case DATE    -> Comparator.comparing(
                    r -> r.chest.getLastUpdated() != null ? r.chest.getLastUpdated() : "",
                    Comparator.reverseOrder());
            case COUNT   -> Comparator.comparingInt(
                    (SearchResult r) -> r.chest.getItems().stream()
                            .mapToInt(ChestItem::getCount).sum()).reversed();
            case NAME    -> Comparator.comparing(
                    r -> getDisplayName(r.chest).toLowerCase(Locale.ROOT));
            default      -> Comparator.comparingDouble(r -> r.distance); // DISTANCE
        };

        results.sort(dimCmp.thenComparing(sortCmp));
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
        if (!Files.exists(file)) return new SearchData(new ArrayList<>(), new ArrayList<>(), null);
        try (Reader r = Files.newBufferedReader(file)) {
            JsonObject root   = JsonParser.parseReader(r).getAsJsonObject();
            Type listType     = new TypeToken<List<ChestRecord>>(){}.getType();
            Type strType      = new TypeToken<List<String>>(){}.getType();

            List<ChestRecord> loadedChests = root.has("chests")
                    ? gson.fromJson(root.get("chests"), listType) : new ArrayList<>();
            List<String> loadedHistory     = root.has("searchHistory")
                    ? gson.fromJson(root.get("searchHistory"), strType) : new ArrayList<>();
            String loadedSort              = root.has("sortMode")
                    ? root.get("sortMode").getAsString() : null;

            return new SearchData(
                    loadedChests  != null ? loadedChests  : new ArrayList<>(),
                    loadedHistory != null ? loadedHistory : new ArrayList<>(),
                    loadedSort
            );
        } catch (Exception e) {
            ChestMemoryMod.LOGGER.error("[ChestStorage] Failed to read chests.json — backing up.", e);
            backupCorrupted(file);
            return new SearchData(new ArrayList<>(), new ArrayList<>(), null);
        }
    }

    private void writeToDisk(String worldName, List<ChestRecord> data) {
        Path file = getFilePath(worldName);
        try {
            Files.createDirectories(file.getParent());
            JsonObject root = new JsonObject();
            root.add("chests", gson.toJsonTree(data));
            root.add("searchHistory", gson.toJsonTree(searchHistory));
            root.addProperty("sortMode", sortMode.name());
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