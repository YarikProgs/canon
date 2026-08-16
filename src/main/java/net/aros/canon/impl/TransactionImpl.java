package net.aros.canon.impl;

import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.tx.Transaction;
import net.aros.canon.store.MutableStateStore;
import net.aros.canon.util.ScopedStateKey;
import net.aros.canon.util.StateMap;

public class TransactionImpl implements Transaction {
    private final StateMap pendingChanges = new StateMap();
    private final MutableStateStore store;

    public TransactionImpl(MutableStateStore store) {
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
        store.commit(pendingChanges, true);
    }
}
