package net.aros.canon.impl;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagRegistry;

import java.util.HashSet;
import java.util.Set;

public class FlagRegistryImpl implements FlagRegistry {
    private final Set<FlagKey<?>> keys = new HashSet<>();

    @Override
    public <T> FlagKey<T> register(FlagKey<T> key) {
        keys.add(key);
        return key;
    }

    @Override
    public Set<FlagKey<?>> allKeys() {
        return keys;
    }
}
