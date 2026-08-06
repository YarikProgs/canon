package net.aros.canon.core.flag;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.resources.ResourceLocation;

public class FlagKey<T> {
    private final ResourceLocation key;
    private final Class<T> type;
    private final Codec<T> codec;
    private final T defaultValue;

    public FlagKey(ResourceLocation key, Class<T> type, Codec<T> codec, T defaultValue) {
        this.key = key;
        this.type = type;
        this.codec = codec;
        this.defaultValue = defaultValue;
    }

    public <U> DataResult<U> encode(DynamicOps<U> ops, T value) {
        return codec.encodeStart(ops, value);
    }

    public <U> DataResult<T> decode(DynamicOps<U> ops, U input) {
        return codec.parse(ops, input);
    }

    public ResourceLocation identifier() {
        return key;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }
}
