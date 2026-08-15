package net.aros.canon.util;

import net.aros.canon.core.flag.FlagKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FlagMap extends HashMap<ScopedFlagKey<?, ?>, Object> {
    public FlagMap() {
    }

    public FlagMap(Map<? extends ScopedFlagKey<?, ?>, ?> m) {
        super(m);
    }

    @SuppressWarnings("unchecked")
    public <S, T> Optional<T> get(FlagKey<S, T> key, S scope) {
        return Optional.ofNullable((T) super.get(new ScopedFlagKey<>(key, scope)));
    }

    public <S, T> void put(FlagKey<S, T> key, S scope, T value) {
        super.put(new ScopedFlagKey<>(key, scope), value);
    }

    @Override
    @Deprecated
    public Object get(Object key) {
        return super.get(key);
    }

    @Override
    @Deprecated
    public Object put(ScopedFlagKey<?, ?> key, Object value) {
        return super.put(key, value);
    }
}
