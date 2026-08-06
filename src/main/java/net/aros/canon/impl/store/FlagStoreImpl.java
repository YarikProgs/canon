package net.aros.canon.impl.store;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagRegistry;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.tx.Sandbox;
import net.aros.canon.wrapper.Can;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static net.aros.canon.CanonLibMod.MOD_ID;

public class FlagStoreImpl implements FlagStore {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "canon-db"));
    private final FlagsDB db = new FlagsDB();
    private final Differ differ = new Differ();

    private final Map<ResourceLocation, Object> flags = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Sandbox> ownership = new ConcurrentHashMap<>();

    public void createConnection(@NotNull MinecraftServer server) {
        db.createConnection(server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath());
        db.initialize();
    }

    public CompletableFuture<Map<FlagKey<?>, String>> persistDiff(Diff diff) {
        return CompletableFuture.supplyAsync(() -> db.writeDiffAndGetNewFlags(diff), dbExecutor);
    }

    public void applyDiff(@NotNull Diff diff) {
        for (ResourceLocation key : diff.deadKeys()) {
            ownership.remove(key);
            flags.remove(key);
        }
        for (ResourceLocation key : diff.conflicts()) {
            ownership.remove(key);
            flags.remove(key);
        }
        for (Map.Entry<FlagKey<?>, String> entry : diff.added().entrySet()) {
            flags.put(entry.getKey().identifier(), decodeJson(entry.getKey(), entry.getValue()).orElse(entry.getKey().defaultValue()));
        }
    }

    public void closeConnection() {
        db.closeConnection();
        flags.clear();
        ownership.clear();
    }

    @Override
    public <T> void set(FlagKey<T> key, T value) {
        var future = commit(Map.of(key, value), true);
        if (!Can.server().isSameThread())
            future.join();
    }

    @Override
    public <T> void setLive(@NotNull FlagKey<T> key, T value) {
        flags.put(key.identifier(), value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull FlagKey<T> key) {
        return (T) flags.getOrDefault(key.identifier(), key.defaultValue());
    }

    @Override
    public void setOwnership(@NotNull FlagKey<?> key, Sandbox sandbox) {
        ownership.put(key.identifier(), sandbox);
    }

    @Override
    public Sandbox getOwner(@NotNull FlagKey<?> key) {
        return ownership.get(key.identifier());
    }

    @Override
    public void removeOwnership(@NotNull FlagKey<?> key, Sandbox sandbox) {
        ownership.remove(key.identifier(), sandbox);
    }

    public CompletableFuture<Void> commit(@NotNull Map<FlagKey<?>, Object> changes, boolean notifyListeners) {
        Map<ResourceLocation, String> encoded = new HashMap<>();
        for (Map.Entry<FlagKey<?>, Object> entry : changes.entrySet()) {
            tryEncodeEntryTo(encoded, entry);
        }

        return CompletableFuture
                .runAsync(() -> db.writeChanges(encoded), dbExecutor)
                .thenRunAsync(() -> {
                    Map<ResourceLocation, Object> oldFlags = new HashMap<>(flags);
                    for (var e : changes.entrySet())
                        flags.put(e.getKey().identifier(), e.getValue());
                    if (notifyListeners) {
                        for (var e : changes.entrySet())
                            //noinspection rawtypes,unchecked
                            FlagHooks.flagChanged((FlagKey) e.getKey(), oldFlags.get(e.getKey().identifier()), e.getValue());
                    }
                }, Can.server());
    }

    public Differ differ() {
        return differ;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    private static Optional<Object> decodeJson(FlagKey<?> key, String json) {
        JsonElement element = GSON.fromJson(json, JsonElement.class);
        if (element == null) return Optional.empty();

        var result = key.decode(JsonOps.COMPRESSED, element);
        if (result.isError()) {
            LOGGER.error("Failed to load flag \"{}\": {}", key.identifier(), result.error().orElseThrow().message());
            return Optional.empty();
        }
        return (Optional<Object>) result.result();
    }

    private <T> void tryEncodeEntryTo(Map<ResourceLocation, String> dest, Map.@NotNull Entry<FlagKey<?>, Object> entry) {
        //noinspection unchecked
        encodeToJson((FlagKey<T>) entry.getKey(), (T) entry.getValue()).ifPresent(json ->
                dest.put(entry.getKey().identifier(), json));
    }

    private static <T> Optional<String> encodeToJson(@NotNull FlagKey<T> key, T value) {
        var result = key.encode(JsonOps.COMPRESSED, value);
        if (result.isError()) {
            LOGGER.error("Failed to encode flag \"{}\": {}", key.identifier(), result.error().orElseThrow().message());
            return Optional.empty();
        }
        return result.result().map(GSON::toJson);
    }

    private static <T> Optional<String> encodeToJson(@NotNull FlagKey<T> key) {
        return encodeToJson(key, key.defaultValue());
    }

    public class Differ {
        public Diff diff(@NotNull FlagRegistry flagRegistry) {
            Set<FlagKey<?>> newKeys = flagRegistry.allKeys();
            Diff result = new Diff();

            removeOldKeys(newKeys, result);
            checkTypeConflicts(newKeys, result);
            addNewKeys(newKeys, result);

            return result;
        }

        private void removeOldKeys(@NotNull Set<FlagKey<?>> keys, Diff result) {
            Set<ResourceLocation> newIdentifierKeys = keys.stream().map(FlagKey::identifier).collect(Collectors.toSet());
            flags.keySet().removeIf(key -> {
                if (newIdentifierKeys.contains(key)) return false;

                Sandbox owner = ownership.get(key);
                if (owner != null) {
                    LOGGER.warn("Removing sandbox {}'s ownership of {} due to dead key elimination", key, owner.name());
                }
                result.deadKeys().add(key);

                return true;
            });
        }

        private void checkTypeConflicts(@NotNull Set<FlagKey<?>> keys, Diff result) {
            for (FlagKey<?> key : keys) {
                Object value = flags.get(key.identifier());
                if (value == null || key.type().isAssignableFrom(value.getClass())) continue;

                LOGGER.warn("Flag {} type mismatch: stored {}, expected {}. Removing stored value",
                        key.identifier(), value.getClass().getName(), key.type().getName());

                Sandbox owner = ownership.get(key.identifier());
                if (owner != null) {
                    LOGGER.warn("Removing sandbox {}'s ownership of {} due to type incompatibility", owner.name(), key.identifier());
                }
                result.conflicts().add(key.identifier());
            }
        }

        private void addNewKeys(@NotNull Set<FlagKey<?>> keys, Diff result) {
            for (FlagKey<?> key : keys) {
                if (!flags.containsKey(key.identifier())) {
                    encodeToJson(key).ifPresent(json -> result.added().put(key, json));
                }
            }
        }
    }
}
