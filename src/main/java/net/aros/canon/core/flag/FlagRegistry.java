package net.aros.canon.core.flag;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;

public interface FlagRegistry {
    <T> FlagKey<T> register(FlagKey<T> key);
    <T> Flag<T> flag(FlagKey<T> key);

    <T, U> DataResult<U> encode(FlagKey<T> key, T value, DynamicOps<U> ops);
    <T, U> DataResult<T> decode(FlagKey<T> key, DynamicOps<U> ops, U input);
}