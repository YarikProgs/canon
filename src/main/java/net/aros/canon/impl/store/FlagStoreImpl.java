package net.aros.canon.impl.store;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagRegistry;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.tx.Sandbox;
import net.aros.canon.util.SerializationUtils;
import net.aros.canon.wrapper.Can;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static net.aros.canon.CanonLibMod.MOD_ID;

public class FlagStoreImpl implements FlagStore {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "canon-db"));
    private final FlagsDB db = new FlagsDB();
    private final Differ differ = new Differ();

    private final Map<ResourceLocation, Object> flags = new ConcurrentHashMap<>();
    private final Map<ResourceLocation, Sandbox> ownership = new ConcurrentHashMap<>();

    public void createConnection(@NotNull MinecraftServer server) {
        db.createConnection(server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath());
        db.initialize();
    }

    public void closeConnection() {
        db.closeConnection();
        flags.clear();
        ownership.clear();
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
            var key = entry.getKey();
            var flag = SerializationUtils.decodeJson(key, entry.getValue()).orElse(key.defaultValue());

            flags.put(key.identifier(), flag);
        }
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
            SerializationUtils.tryEncodeEntryTo(encoded, entry);
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
                    SerializationUtils.encodeToJson(key).ifPresent(json -> result.added().put(key, json));
                }
            }
        }
    }
}
