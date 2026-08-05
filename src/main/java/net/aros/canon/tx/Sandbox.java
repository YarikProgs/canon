package net.aros.canon.tx;

import net.aros.canon.core.flag.FlagKey;

public interface Sandbox {
    <T> T get(FlagKey<T> key);

    <T> void set(FlagKey<T> key, T value);

    void commit();

    void rollback();

    String name();
}
