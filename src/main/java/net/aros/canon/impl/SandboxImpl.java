package net.aros.canon.impl;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.impl.store.FlagStoreImpl;
import net.aros.canon.util.FlagMap;
import net.aros.canon.util.ScopedFlagKey;
import org.slf4j.Logger;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SandboxImpl implements Sandbox {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final FlagMap oldValues = new FlagMap();
    private final FlagStore store;
    private final String name;

    public SandboxImpl(FlagStore store, String name) {
        this.store = store;
        this.name = name;
    }

    @Override
    public <S, T> T get(FlagKey<S, T> key, S scope) {
        return store.get(key, scope);
    }

    @Override
    public <S, T> void set(FlagKey<S, T> key, S scope, T value) {
        oldValues.computeIfAbsent(new ScopedFlagKey<>(key, scope), key1 -> store.get((FlagKey) key1.key(), key1.scope()));
        store.setLive(key, scope, value);
        FlagHooks.flagChanged(key, scope, (T) oldValues.get(key, scope), value);

        checkConflict(key, scope);
        store.setOwner(key, scope, this);
    }

    @Override
    public void commit() {
        FlagMap commit = new FlagMap();
        for (ScopedFlagKey<?, ?> scopeKey : oldValues.keySet()) {
            removeAndCommitIfOwner(commit, scopeKey);
        }
        ((FlagStoreImpl) store).commit(commit, false, Canon.get().db());
    }

    private <S, T> void removeAndCommitIfOwner(FlagMap commit, ScopedFlagKey<S, T> scopedKey) {
        var key = scopedKey.key();
        var scope = scopedKey.scope();

        if (store.getOwner(key, scope) == this) {
            commit.put(key, scope, get(key, scope));
            store.setOwner(key, scope, null);
        }
    }

    @Override
    public void rollback() {
        FlagMap currentValues = new FlagMap();
        for (ScopedFlagKey<?, ?> scopedKey : oldValues.keySet()) {
            removeAndSetIfOwner(currentValues, scopedKey);
        }
        for (ScopedFlagKey<?, ?> key : currentValues.keySet()) {
            fireFlagChanged(key, currentValues);
        }
    }

    private <S, T> void fireFlagChanged(ScopedFlagKey<S, T> key, FlagMap currentValues) {
        FlagHooks.flagChanged(
                key.key(),
                key.scope(),
                currentValues.get(key.key(), key.scope()).orElseThrow(),
                oldValues.get(key.key(), key.scope()).orElseThrow()
        );
    }

    private <S, T> void removeAndSetIfOwner(FlagMap currentValues, ScopedFlagKey<S, T> scopedKey) {
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

    private <S, T> void checkConflict(FlagKey<S, T> key, S scope) {
        Sandbox owner = store.getOwner(key, scope);
        if (owner != null && owner != this) {
            LOGGER.warn("Flag {} conflict: sandbox {} overwrote {}", key.identifier(), name(), owner.name());
        }
    }
}
