package net.aros.canon.core.flag;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.tx.Sandbox;
import net.aros.canon.wrapper.Can;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.aros.canon.CanonLibMod.MOD_ID;

public class FlagStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "canon-db"));
    private final FlagsDB db = new FlagsDB();

    private final Map<String, Object> flags = new ConcurrentHashMap<>();
    private final Map<String, Sandbox> ownership = new ConcurrentHashMap<>();

    private void loadAll(@NotNull FlagRegistry flagRegistry) {
        Map<String, String> raw = db.readAllFlags();

        for (FlagKey<?> key : flagRegistry.allKeys()) {
            String json = raw.get(key.key());
            flags.put(key.key(), Optional.ofNullable(json).flatMap(json1 -> decodeJson(key, json1))
                    .orElseGet(key::defaultValue));
        }
    }

    public void createConnection(MinecraftServer server) {
        db.createConnection(server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath());
        db.initialize();
    }

    public Diff diff(@NotNull FlagRegistry flagRegistry) {
        Set<FlagKey<?>> newKeys = flagRegistry.allKeys();
        Diff result = new Diff();

        removeOldKeys(newKeys, result);
        checkTypeConflicts(newKeys, result);
        addNewKeys(newKeys, result);

        return result;
    }

    private void removeOldKeys(@NotNull Set<FlagKey<?>> keys, Diff result) {
        Set<String> newStringKeys = keys.stream().map(FlagKey::key).collect(Collectors.toSet());
        flags.keySet().removeIf(key -> {
            if (newStringKeys.contains(key)) return false;

            Sandbox owner = ownership.get(key);
            if (owner != null) {
                LOGGER.warn("Removing sandbox {}'s ownership of {} due to dead key elimination", key, owner.name());;
            }
            result.deadKeys.add(key);

            return true;
        });
    }

    private void checkTypeConflicts(@NotNull Set<FlagKey<?>> keys, Diff result) {
        for (FlagKey<?> key : keys) {
            Object value = flags.get(key.key());
            if (value == null || key.type().isAssignableFrom(value.getClass())) continue;

            LOGGER.warn("Flag {} type mismatch: stored {}, expected {}. Removing stored value",
                    key.key(), value.getClass().getName(), key.type().getName());

            Sandbox owner = ownership.get(key.key());
            if (owner != null) {
                LOGGER.warn("Removing sandbox {}'s ownership of {} due to type incompatibility", owner.name(), key.key());
            }
            result.conflicts.add(key.key());
        }
    }

    private void addNewKeys(@NotNull Set<FlagKey<?>> keys, Diff result) {
        for (FlagKey<?> key : keys) {
            if (!flags.containsKey(key.key())) {
                encodeToJson(key).ifPresent(json -> {
                    result.added.put(key, json);
                });
            }
        }
    }

    public CompletableFuture<Void> persistDiff(Diff diff) {
        return CompletableFuture.runAsync(() -> db.writeDiff(diff), dbExecutor);
    }

    public void applyDiff(@NotNull Diff diff) {
        for (String key : diff.deadKeys) {
            ownership.remove(key);
            flags.remove(key);
        }
        for (String key : diff.conflicts) {
            ownership.remove(key);
            flags.remove(key);
        }
        for (FlagKey<?> key : diff.added.keySet()) {
            flags.put(key.key(), key.defaultValue());
        }
    }

    public void closeConnection() {
        db.closeConnection();
    }

    public <T> void set(FlagKey<T> key, T value) {
        var future = commit(Map.of(key, value), true);
        if (!Can.server().isSameThread())
            future.join();
    }

    public <T> void setLive(@NotNull FlagKey<T> key, T value) {
        flags.put(key.key(), value);
    }

    public void setOwnership(@NotNull FlagKey<?> key, Sandbox sandbox) {
        ownership.put(key.key(), sandbox);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull FlagKey<T> key) {
        return (T) flags.getOrDefault(key.key(), key.defaultValue());
    }

    public Sandbox getOwner(@NotNull FlagKey<?> key) {
        return ownership.get(key.key());
    }

    public void removeOwnership(@NotNull FlagKey<?> key, Sandbox sandbox) {
        ownership.remove(key.key(), sandbox);
    }

    public CompletableFuture<Void> commit(@NotNull Map<FlagKey<?>, Object> changes, boolean notifyListeners) {
        Map<String, String> encoded = new HashMap<>();
        for (Map.Entry<FlagKey<?>, Object> entry : changes.entrySet()) {
            tryEncodeEntryTo(encoded, entry);
        }

        return CompletableFuture
                .runAsync(() -> db.writeChanges(encoded), dbExecutor)
                .thenRunAsync(() -> {
                    Map<String, Object> oldFlags = new HashMap<>(flags);
                    for (var e : changes.entrySet())
                        flags.put(e.getKey().key(), e.getValue());
                    if (notifyListeners) {
                        for (var e : changes.entrySet())
                            //noinspection rawtypes,unchecked
                            FlagHooks.flagChanged((FlagKey) e.getKey(), oldFlags.get(e.getKey().key()), e.getValue());
                    }
                }, Can.server());
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Optional<Object> decodeJson(FlagKey<?> key, String json) {
        JsonElement element = GSON.fromJson(json, JsonElement.class);
        if (element == null) return Optional.empty();

        var result = key.decode(JsonOps.COMPRESSED, element);
        if (result.isError()) {
            LOGGER.error("Failed to load flag \"{}\": {}", key.key(), result.error().orElseThrow().message());
            return Optional.empty();
        }
        return (Optional<Object>) result.result();
    }

    private <T> void tryEncodeEntryTo(Map<String, String> dest, Map.@NotNull Entry<FlagKey<?>, Object> entry) {
        //noinspection unchecked
        encodeToJson((FlagKey<T>) entry.getKey(), (T) entry.getValue()).ifPresent(json ->
                dest.put(entry.getKey().key(), json));
    }

    private static <T> Optional<String> encodeToJson(@NotNull FlagKey<T> key, T value) {
        var result = key.encode(JsonOps.COMPRESSED, value);
        if (result.isError()) {
            LOGGER.error("Failed to encode flag \"{}\": {}", key.key(), result.error().orElseThrow().message());
            return Optional.empty();
        }
        return result.result().map(GSON::toJson);
    }

    private static <T> Optional<String> encodeToJson(@NotNull FlagKey<T> key) {
        return encodeToJson(key, key.defaultValue());
    }

    public record Diff(Map<FlagKey<?>, String> added, Set<String> deadKeys, Set<String> conflicts) {
        private Diff() {
            this(new HashMap<>(), new HashSet<>(), new HashSet<>());
        }
    }
}
