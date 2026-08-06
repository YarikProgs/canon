package net.aros.canon.core.flag;

import java.util.Set;

public interface FlagRegistry {
    Set<FlagKey<?>> allKeys();
}