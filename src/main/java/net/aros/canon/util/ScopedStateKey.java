package net.aros.canon.util;

import net.aros.canon.core.state.StateKey;

public record ScopedStateKey<S, T>(StateKey<S, T> key, S scope) {
}
