package net.aros.canon.impl;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.aros.canon.core.flag.Flag;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class FlagRegistryImpl implements FlagRegistry {
    private final Map<FlagKey<?>, Flag<?>> direct = new HashMap<>();

    @Override
    public <T> FlagKey<T> register(FlagKey<T> key) {
        direct.put(key, new FlagImpl<>(key));
        return key;
    }

    @Override
    public <T> Flag<T> flag(FlagKey<T> key) {
        //noinspection unchecked
        return (Flag<T>) direct.get(key);
    }

    @Override
    public <T, U> DataResult<U> encode(@NotNull FlagKey<T> key, T value, DynamicOps<U> ops) {
        return key.codec().encodeStart(ops, value);
    }

    @Override
    public <T, U> DataResult<T> decode(@NotNull FlagKey<T> key, DynamicOps<U> ops, U input) {
        return key.codec().parse(ops, input);
    }

    private static class FlagImpl<T> implements Flag<T> {
        private final FlagKey<T> key;
        private T value;

        public FlagImpl(FlagKey<T> key) {
            this.key = key;
            this.value = key().defaultValue();
        }

        @Override
        public FlagKey<T> key() {
            return key;
        }

        @Override
        public T get() {
            return value;
        }

        @Override
        public void set(T value) {
            this.value = value;
        }
    }
}
