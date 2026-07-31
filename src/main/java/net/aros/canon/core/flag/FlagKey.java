package net.aros.canon.core.flag;

import com.mojang.serialization.Codec;

public class FlagKey<T> {
    private final String key;
    private final Codec<T> codec;
    private final T defaultValue;

    public FlagKey(String key, Codec<T> codec, T defaultValue) {
        this.key = key;
        this.codec = codec;
        this.defaultValue = defaultValue;
    }

    public String key() {
        return key;
    }

    public Codec<T> codec() {
        return codec;
    }

    public T defaultValue() {
        return defaultValue;
    }
}
