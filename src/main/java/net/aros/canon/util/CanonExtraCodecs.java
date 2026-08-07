package net.aros.canon.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.PrimitiveCodec;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public class CanonExtraCodecs {
    public static final Codec<Character> CHAR = new PrimitiveCodec<>() {
        @Override
        public <T> DataResult<Character> read(DynamicOps<T> ops, T input) {
            return ops.getStringValue(input).flatMap(string -> {
                if (string.length() != 1) return DataResult.error(() -> "Expected char, got: " + string);
                return DataResult.success(string.charAt(0));
            });
        }

        @Override
        public <T> T write(DynamicOps<T> ops, Character value) {
            return ops.createString(value.toString());
        }
    };

    public static final Codec<Optional<ResourceLocation>> OPT_RESOURCE_LOCATION = Codec.STRING.xmap(
            string -> string.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(string)),
            opt -> opt.map(ResourceLocation::toString).orElse("")
    );
}
