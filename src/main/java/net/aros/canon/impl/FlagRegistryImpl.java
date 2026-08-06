package net.aros.canon.impl;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagRegistry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlagRegistryImpl implements FlagRegistry {
    private final Set<FlagKey<?>> keys = new HashSet<>();

    @Override
    public Set<FlagKey<?>> allKeys() {
        return keys;
    }

    public void replaceWith(List<FlagKey<?>> newKeys) {
        keys.clear();
        keys.addAll(newKeys);
    }
}
