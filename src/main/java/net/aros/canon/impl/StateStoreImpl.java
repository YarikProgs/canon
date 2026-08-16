package net.aros.canon.impl;

import net.aros.canon.core.Canon;
import net.aros.canon.core.db.StatesDB;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.StateStore;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.event.StateHooks;
import net.aros.canon.util.ScopedStateKey;
import net.aros.canon.util.StateMap;
import net.aros.canon.wrapper.Can;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static net.aros.canon.CanonLibMod.MOD_ID;

@ParametersAreNonnullByDefault
public class StateStoreImpl implements StateStore {
    private final StateMap states = new StateMap();
    private final Map<ScopedStateKey<?, ?>, Sandbox> ownership = new ConcurrentHashMap<>();

    public void createConnection(MinecraftServer server, StatesDB db) {
        db.createConnection(server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath());
        db.initialize();
    }

    public void closeConnection(StatesDB db) {
        db.closeConnection();
        states.clear();
        ownership.clear();
    }

    @Override
    public <S, T> void set(StateKey<S, T> key, S scope, T value) {
        var future = commit(new StateMap(Map.of(new ScopedStateKey<>(key, scope), value)), true, Canon.get().db());
        if (!Can.server().isSameThread())
            future.join();
    }

    @Override
    public <S, T> void setLive(StateKey<S, T> key, S scope, T value) {
        var oldValue = get(key, scope);
        states.put(key, scope, value);

        StateHooks.stateChanged(key, scope, oldValue, value);
    }

    @Override
    public <S, T> T get(StateKey<S, T> key, S scope) {
        return states.get(key, scope).orElseGet(() -> key.type().defaultValue());
    }

    @Override
    public <S> void setOwner(StateKey<S, ?> key, S scope, @Nullable Sandbox sandbox) {
        if (sandbox == null) {
            ownership.remove(new ScopedStateKey<>(key, scope));
            return;
        }
        ownership.put(new ScopedStateKey<>(key, scope), sandbox);
    }

    @Override
    public @Nullable <S> Sandbox getOwner(StateKey<S, ?> key, S scope) {
        return ownership.get(new ScopedStateKey<>(key, scope));
    }

    public void replaceWith(StateMap newStates) {
        var oldStates = new StateMap(states);
        states.clear();
        ownership.clear();
        states.putAll(newStates);
        fireChanges(oldStates, newStates);
        StateHooks.fireStateReset();
    }

    public CompletableFuture<Void> commit(StateMap changes, boolean notifyListeners, StatesDB db) {
        return CompletableFuture
                .runAsync(() -> db.persist(changes), db.executor())
                .thenRunAsync(() -> {
                    StateMap oldStates = new StateMap(states);
                    states.putAll(changes);
                    if (notifyListeners) {
                        fireChanges(oldStates, changes);
                    }
                }, Can.server());
    }

    @SuppressWarnings({"rawtypes", "deprecation", "unchecked"})
    private static void fireChanges(StateMap oldMap, StateMap newMap) {
        for (Map.Entry<ScopedStateKey<?, ?>, Object> entry : newMap.entrySet()) {
            StateHooks.stateChanged((StateKey) entry.getKey().key(), entry.getKey().scope(), oldMap.get(entry.getKey()), entry.getValue());
        }
    }
}
