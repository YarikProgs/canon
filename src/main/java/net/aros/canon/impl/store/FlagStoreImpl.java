package net.aros.canon.impl.store;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.tx.Sandbox;
import net.aros.canon.util.GsonHelper;
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

import static net.aros.canon.CanonLibMod.MOD_ID;

public class FlagStoreImpl implements FlagStore {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "canon-db"));
    private final FlagsDB db = new FlagsDB();

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
        return (T) flags.getOrDefault(key.identifier(), key.type().defaultValue());
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

    public CompletableFuture<Map<FlagKey<?>, Object>> reconcileKeysInDB(Set<FlagKey<?>> keys) {
        return CompletableFuture.supplyAsync(() -> db.reconcileKeys(keys), dbExecutor);
    }

    public void reconcileKeysInLive(@NotNull Map<FlagKey<?>, Object> map) {
        flags.clear();
        ownership.clear();
        for (Map.Entry<FlagKey<?>, Object> entry : map.entrySet()) {
            flags.put(entry.getKey().identifier(), entry.getValue());
        }
        FlagHooks.fireFlagReset();
    }

    public CompletableFuture<Void> commit(@NotNull Map<FlagKey<?>, Object> changes, boolean notifyListeners) {
        Map<FlagKey<?>, String> encoded = new HashMap<>();
        for (Map.Entry<FlagKey<?>, Object> entry : changes.entrySet()) {
            //noinspection unchecked
            ((FlagKey<Object>) entry.getKey()).encode(JsonOps.COMPRESSED, entry.getValue()).ifPresent(result -> {
                encoded.put(entry.getKey(), GsonHelper.toString(result));
            });
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
}
