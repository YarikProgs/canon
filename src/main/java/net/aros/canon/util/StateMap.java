package net.aros.canon.util;

import net.aros.canon.core.state.StateKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class StateMap extends HashMap<ScopedStateKey<?, ?>, Object> {
    public StateMap() {
    }

    public StateMap(Map<? extends ScopedStateKey<?, ?>, ?> m) {
        super(m);
    }

    @SuppressWarnings("unchecked")
    public <S, T> Optional<T> get(StateKey<S, T> key, S scope) {
        return Optional.ofNullable((T) super.get(new ScopedStateKey<>(key, scope)));
    }

    public <S, T> void put(StateKey<S, T> key, S scope, T value) {
        super.put(new ScopedStateKey<>(key, scope), value);
    }

    @Override
    @Deprecated
    public Object get(Object key) {
        return super.get(key);
    }

    @Override
    @Deprecated
    public Object put(ScopedStateKey<?, ?> key, Object value) {
        return super.put(key, value);
    }
}
