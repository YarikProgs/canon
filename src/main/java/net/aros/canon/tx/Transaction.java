package net.aros.canon.tx;

import net.aros.canon.core.flag.FlagKey;

public interface Transaction {
    <T> T get(FlagKey<T> key);

    <T> void set(FlagKey<T> key, T value);

    void commit();
}
