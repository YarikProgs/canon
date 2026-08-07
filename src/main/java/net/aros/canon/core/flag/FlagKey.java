package net.aros.canon.core.flag;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import net.aros.canon.core.flag.type.FlagType;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Optional;

public record FlagKey<T>(ResourceLocation identifier, FlagType<T> type) {
    private static final Logger LOGGER = LogUtils.getLogger();

    public <U> Optional<U> encodeDefault(DynamicOps<U> ops) {
        return encode(ops, type.defaultValue());
    }

    public <U> Optional<U> encode(DynamicOps<U> ops, T value) {
        var result = type.codec().encodeStart(ops, value);
        if (result.isError()) {
            LOGGER.error("Failed to encode flag '{}': {}",
                    identifier, result.error().orElseThrow().message());
        }
        return result.result();
    }

    public <U> T parseOrDefault(DynamicOps<U> ops, U input) {
        return parse(ops, input).orElse(type.defaultValue());
    }

    public <U> Optional<T> parse(DynamicOps<U> ops, U input) {
        var result = type.codec().parse(ops, input);
        if (result.isError()) {
            LOGGER.error("Failed to parse flag '{}': {}",
                    identifier, result.error().orElseThrow().message());
        }
        return result.result();
    }
}
