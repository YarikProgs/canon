package net.aros.canon.core.tx;

import net.aros.canon.core.state.StateKey;

public interface Transaction {
    <S, T> T get(StateKey<S, T> key, S scope);

    <S, T> void set(StateKey<S, T> key, S scope, T value);

    void commit();
}
