package net.aros.canon.impl.store;

import net.aros.canon.core.flag.FlagKey;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record Diff(Map<FlagKey<?>, String> added, Set<String> deadKeys, Set<String> conflicts) {
    public Diff() {
        this(new HashMap<>(), new HashSet<>(), new HashSet<>());
    }
}