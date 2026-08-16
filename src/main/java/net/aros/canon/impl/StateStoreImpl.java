package net.aros.canon.impl;

import net.aros.canon.core.Can;
import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.event.StateHooks;
import net.aros.canon.store.MutableStateStore;
import net.aros.canon.util.ScopedStateKey;
import net.aros.canon.util.StateMap;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
public class StateStoreImpl implements MutableStateStore {
    private final StateMap states = new StateMap();
    private final Map<ScopedStateKey<?, ?>, Sandbox> ownership = new ConcurrentHashMap<>();

    @Override
    public void clear() {
        onServer(() -> {
            states.clear();
            ownership.clear();
        });
    }

    @Override
    public <S, T> void set(StateKey<S, T> key, S scope, T value) {
        var future = commit(new StateMap(Map.of(new ScopedStateKey<>(key, scope), value)), true);
        if (!Can.server().isSameThread())
            future.join();
    }

    @Override
    public <S, T> void setLive(StateKey<S, T> key, S scope, T value) {
        onServer(() -> {
            var oldValue = get(key, scope);
            states.put(key, scope, value);

            StateHooks.stateChanged(key, scope, oldValue, value);
        });
    }

    @Override
    public <S, T> T get(StateKey<S, T> key, S scope) {
        return onServer(() -> states.get(key, scope).orElseGet(() -> key.type().defaultValue()));
    }

    @Override
    public <S> void setOwner(StateKey<S, ?> key, S scope, @Nullable Sandbox sandbox) {
        onServer(() -> {
            if (sandbox == null) {
                ownership.remove(new ScopedStateKey<>(key, scope));
                return;
            }
            ownership.put(new ScopedStateKey<>(key, scope), sandbox);
        });
    }

    @Override
    public @Nullable <S> Sandbox getOwner(StateKey<S, ?> key, S scope) {
        return onServer(() -> ownership.get(new ScopedStateKey<>(key, scope)));
    }

    @Override
    public void replaceLiveWith(StateMap newStates) {
        onServer(() -> {
            var oldStates = new StateMap(states);
            states.clear();
            ownership.clear();
            states.putAll(newStates);
            fireChanges(oldStates, newStates);
            StateHooks.fireStateReset();
        });
    }

    @Override
    public CompletableFuture<Void> commit(StateMap changes, boolean notifyListeners) {
        return CompletableFuture
                .runAsync(() -> Canon.get().db().persist(changes), Canon.get().db().executor())
                .thenRunAsync(() -> {
                    StateMap oldStates = new StateMap(states);
                    states.putAll(changes);
                    if (notifyListeners) {
                        fireChanges(oldStates, changes);
                    }
                }, Can.server());
    }

    private <T> T onServer(Supplier<T> supplier) {
        return Can.server().isSameThread() ? supplier.get() : Can.server().submit(supplier).join();
    }

    private void onServer(Runnable runnable) {
        if (Can.server().isSameThread())
            runnable.run();
        else
            Can.server().submit(runnable).join();
    }

    @SuppressWarnings({"rawtypes", "deprecation", "unchecked"})
    private static void fireChanges(StateMap oldMap, StateMap newMap) {
        for (Map.Entry<ScopedStateKey<?, ?>, Object> entry : newMap.entrySet()) {
            StateHooks.stateChanged((StateKey) entry.getKey().key(), entry.getKey().scope(), oldMap.get(entry.getKey()), entry.getValue());
        }
    }
}
