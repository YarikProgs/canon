package net.aros.canon.impl;

import net.aros.canon.core.Canon;
import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.util.FlagMap;
import net.aros.canon.util.ScopedFlagKey;
import net.aros.canon.wrapper.Can;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.aros.canon.CanonLibMod.MOD_ID;

public class FlagStoreImpl implements FlagStore {
    private final FlagMap flags = new FlagMap();
    private final Map<ScopedFlagKey<?, ?>, Sandbox> ownership = new ConcurrentHashMap<>();

    public void createConnection(@NotNull MinecraftServer server, FlagsDB db) {
        db.createConnection(server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath());
        db.initialize();
    }

    public void closeConnection(FlagsDB db) {
        db.closeConnection();
        flags.clear();
        ownership.clear();
    }

    @Override
    public <S, T> void set(FlagKey<S, T> key, S scope, T value) {
        var future = commit(new FlagMap(Map.of(new ScopedFlagKey<>(key, scope), value)), true, Canon.get().db());
        if (!Can.server().isSameThread())
            future.join();
    }

    @Override
    public <S, T> void setLive(FlagKey<S, T> key, S scope, T value) {
        flags.put(key, scope, value);
    }

    @Override
    public <S, T> T get(FlagKey<S, T> key, S scope) {
        return flags.get(key, scope).orElseGet(() -> key.type().defaultValue());
    }

    @Override
    public <S> void setOwner(FlagKey<S, ?> key, S scope, @Nullable Sandbox sandbox) {
        if (sandbox == null) {
            ownership.remove(new ScopedFlagKey<>(key, scope));
            return;
        }
        ownership.put(new ScopedFlagKey<>(key, scope), sandbox);
    }

    @Override
    public @Nullable <S> Sandbox getOwner(FlagKey<S, ?> key, S scope) {
        return ownership.get(new ScopedFlagKey<>(key, scope));
    }

    public void replaceWith(FlagMap newFlags) {
        flags.clear();
        ownership.clear();
        flags.putAll(newFlags);
        FlagHooks.fireFlagReset();
    }

    public CompletableFuture<Void> commit(@NotNull FlagMap changes, boolean notifyListeners, FlagsDB db) {
        return CompletableFuture
                .runAsync(() -> db.persist(changes), db.executor())
                .thenRunAsync(() -> {
                    FlagMap oldFlags = new FlagMap(flags);
                    flags.putAll(changes);
                    if (notifyListeners) {
                        for (var e : changes.entrySet())
                            //noinspection rawtypes,deprecation,unchecked
                            FlagHooks.flagChanged((FlagKey) e.getKey().key(), e.getKey().scope(), oldFlags.get(e.getKey()), e.getValue());
                    }
                }, Can.server());
    }
}
