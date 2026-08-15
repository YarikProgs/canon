package net.aros.canon.impl;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.core.tx.Transaction;
import net.aros.canon.util.FlagMap;
import net.aros.canon.util.ScopedFlagKey;

public class TransactionImpl implements Transaction {
    private final FlagMap pendingChanges = new FlagMap();
    private final FlagStore store;

    public TransactionImpl(FlagStore store) {
        this.store = store;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> T get(FlagKey<S, T> key, S scope) {
        return pendingChanges.containsKey(new ScopedFlagKey<>(key, scope)) ? (T) pendingChanges.get(key, scope) : store.get(key, scope);
    }

    @Override
    public <S, T> void set(FlagKey<S, T> key, S scope, T value) {
        pendingChanges.put(key, scope, value);
    }

    @Override
    public void commit() {
        ((FlagStoreImpl) store).commit(pendingChanges, true, Canon.get().db());
    }
}
