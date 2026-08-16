package net.aros.canon.impl;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.StateStore;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.event.StateHooks;
import net.aros.canon.util.StateMap;
import net.aros.canon.util.ScopedStateKey;
import org.slf4j.Logger;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SandboxImpl implements Sandbox {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final StateMap oldValues = new StateMap();
    private final StateStore store;
    private final String name;

    public SandboxImpl(StateStore store, String name) {
        this.store = store;
        this.name = name;
    }

    @Override
    public <S, T> T get(StateKey<S, T> key, S scope) {
        return store.get(key, scope);
    }

    @Override
    public <S, T> void set(StateKey<S, T> key, S scope, T value) {
        oldValues.computeIfAbsent(new ScopedStateKey<>(key, scope), key1 -> store.get((StateKey) key1.key(), key1.scope()));
        store.setLive(key, scope, value);

        checkConflict(key, scope);
        store.setOwner(key, scope, this);
    }

    @Override
    public void commit() {
        StateMap commit = new StateMap();
        for (ScopedStateKey<?, ?> scopeKey : oldValues.keySet()) {
            removeAndCommitIfOwner(commit, scopeKey);
        }
        ((StateStoreImpl) store).commit(commit, false, Canon.get().db());
    }

    private <S, T> void removeAndCommitIfOwner(StateMap commit, ScopedStateKey<S, T> scopedKey) {
        var key = scopedKey.key();
        var scope = scopedKey.scope();

        if (store.getOwner(key, scope) == this) {
            commit.put(key, scope, get(key, scope));
            store.setOwner(key, scope, null);
        }
    }

    @Override
    public void rollback() {
        StateMap currentValues = new StateMap();
        for (ScopedStateKey<?, ?> scopedKey : oldValues.keySet()) {
            removeAndSetIfOwner(currentValues, scopedKey);
        }
        for (ScopedStateKey<?, ?> key : currentValues.keySet()) {
            fireStateChanged(key, currentValues);
        }
    }

    private <S, T> void fireStateChanged(ScopedStateKey<S, T> key, StateMap currentValues) {
        StateHooks.stateChanged(
                key.key(),
                key.scope(),
                currentValues.get(key.key(), key.scope()).orElseThrow(),
                oldValues.get(key.key(), key.scope()).orElseThrow()
        );
    }

    private <S, T> void removeAndSetIfOwner(StateMap currentValues, ScopedStateKey<S, T> scopedKey) {
        var key = scopedKey.key();
        var scope = scopedKey.scope();
        if (store.getOwner(key, scope) == this) {
            currentValues.put(key, scope, store.get(key, scope));
            store.setLive(key, scope, oldValues.get(key, scope).orElseThrow());
            store.setOwner(key, scope, null);
        }
    }

    @Override
    public String name() {
        return name;
    }

    private <S, T> void checkConflict(StateKey<S, T> key, S scope) {
        Sandbox owner = store.getOwner(key, scope);
        if (owner != null && owner != this) {
            LOGGER.warn("State {} conflict: sandbox {} overwrote {}", key.identifier(), name(), owner.name());
        }
    }
}
