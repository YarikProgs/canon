package net.aros.canon.core.tx;

import net.aros.canon.core.flag.FlagKey;

public interface Transaction {
    <S, T> T get(FlagKey<S, T> key, S scope);

    <S, T> void set(FlagKey<S, T> key, S scope, T value);

    void commit();
}
