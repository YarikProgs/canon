package net.aros.canon.util;

import net.aros.canon.core.flag.FlagKey;

public record ScopedFlagKey<S, T>(FlagKey<S, T> key, S scope) {
}
