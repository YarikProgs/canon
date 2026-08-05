package net.aros.canon.core.flag;

import java.util.Set;

public interface FlagRegistry {
    <T> FlagKey<T> register(FlagKey<T> key);

    Set<FlagKey<?>> allKeys();
}