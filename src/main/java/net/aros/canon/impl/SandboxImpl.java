package net.aros.canon.impl;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.impl.store.FlagStoreImpl;
import net.aros.canon.tx.Sandbox;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
public class SandboxImpl implements Sandbox {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<FlagKey<?>, Object> oldValues = new HashMap<>();
    private final FlagStore store;
    private final String name;

    public SandboxImpl(FlagStore store, String name) {
        this.store = store;
        this.name = name;
    }

    @Override
    public <T> T get(FlagKey<T> key) {
        return store.get(key);
    }

    @Override
    public <T> void set(FlagKey<T> key, T value) {
        oldValues.computeIfAbsent(key, store::get);
        store.setLive(key, value);
        FlagHooks.flagChanged(key, (T) oldValues.get(key), value);

        checkConflict(key);
        store.setOwnership(key, this);
    }

    @Override
    public void commit() {
        Map<FlagKey<?>, Object> commit = new HashMap<>();
        for (FlagKey<?> key : oldValues.keySet()) {
            if (store.getOwner(key) == this) {
                commit.put(key, get(key));
                store.removeOwnership(key, this);
            }
        }
        ((FlagStoreImpl) store).commit(commit, false);
    }

    @Override
    public void rollback() {
        Map<FlagKey<?>, Object> currentValues = new HashMap<>();
        for (FlagKey<?> key : oldValues.keySet()) {
            if (store.getOwner(key) != this) continue;

            currentValues.put(key, store.get(key));
            store.setLive((FlagKey) key, oldValues.get(key));
            store.removeOwnership(key, this);
        }
        for (FlagKey<?> key : currentValues.keySet()) {
            FlagHooks.flagChanged((FlagKey) key, currentValues.get(key), oldValues.get(key));
        }
    }

    @Override
    public String name() {
        return name;
    }

    private <T> void checkConflict(FlagKey<T> key) {
        Sandbox owner = store.getOwner(key);
        if (owner != null && owner != this) {
            LOGGER.warn("Flag {} conflict: sandbox {} overwrote {}", key.identifier(), name(), owner.name());
        }
    }
}
