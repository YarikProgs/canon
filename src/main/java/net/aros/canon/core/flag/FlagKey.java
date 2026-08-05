package net.aros.canon.core.flag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public class FlagKey<T> {
    private final String key;
    private final Codec<T> codec;
    private final T defaultValue;

    public FlagKey(String key, Codec<T> codec, T defaultValue) {
        this.key = key;
        this.codec = codec;
        this.defaultValue = defaultValue;
    }

    public <U> DataResult<U> encode(DynamicOps<U> ops, T value) {
        return codec.encodeStart(ops, value);
    }

    public <U> DataResult<T> decode(DynamicOps<U> ops, U input) {
        return codec.parse(ops, input);
    }

    public String key() {
        return key;
    }

    public T defaultValue() {
        return defaultValue;
    }
}
