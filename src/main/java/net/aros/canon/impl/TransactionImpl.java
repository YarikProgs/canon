package net.aros.canon.impl;

import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.StateStore;
import net.aros.canon.core.tx.Transaction;
import net.aros.canon.util.StateMap;
import net.aros.canon.util.ScopedStateKey;

public class TransactionImpl implements Transaction {
    private final StateMap pendingChanges = new StateMap();
    private final StateStore store;

    public TransactionImpl(StateStore store) {
        this.store = store;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <S, T> T get(StateKey<S, T> key, S scope) {
        return pendingChanges.containsKey(new ScopedStateKey<>(key, scope)) ? (T) pendingChanges.get(key, scope) : store.get(key, scope);
    }

    @Override
    public <S, T> void set(StateKey<S, T> key, S scope, T value) {
        pendingChanges.put(key, scope, value);
    }

    @Override
    public void commit() {
        ((StateStoreImpl) store).commit(pendingChanges, true, Canon.get().db());
    }
}
