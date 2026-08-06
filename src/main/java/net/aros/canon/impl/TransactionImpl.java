package net.aros.canon.impl;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.impl.store.FlagStoreImpl;
import net.aros.canon.tx.Transaction;

import java.util.HashMap;
import java.util.Map;

public class TransactionImpl implements Transaction {
    private final Map<FlagKey<?>, Object> pendingChanges = new HashMap<>();
    private final FlagStore store;

    public TransactionImpl(FlagStore store) {
        this.store = store;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(FlagKey<T> key) {
        return pendingChanges.containsKey(key) ? (T) pendingChanges.get(key) : store.get(key);
    }

    @Override
    public <T> void set(FlagKey<T> key, T value) {
        pendingChanges.put(key, value);
    }

    @Override
    public void commit() {
        ((FlagStoreImpl) store).commit(pendingChanges, true);
    }
}
