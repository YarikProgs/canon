package net.aros.canon.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.aros.canon.core.flag.FlagKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;

public class SerializationUtils {
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LogUtils.getLogger();

    @SuppressWarnings("unchecked")
    @NotNull
    public static Optional<Object> decodeJson(FlagKey<?> key, String json) {
        JsonElement element = GSON.fromJson(json, JsonElement.class);
        if (element == null) return Optional.empty();

        var result = key.decode(JsonOps.COMPRESSED, element);
        if (result.isError()) {
            LOGGER.error("Failed to load flag \"{}\": {}", key.identifier(), result.error().orElseThrow().message());
            return Optional.empty();
        }
        return (Optional<Object>) result.result();
    }

    public static <T> void tryEncodeEntryTo(Map<ResourceLocation, String> dest, Map.@NotNull Entry<FlagKey<?>, Object> entry) {
        //noinspection unchecked
        encodeToJson((FlagKey<T>) entry.getKey(), (T) entry.getValue()).ifPresent(json ->
                dest.put(entry.getKey().identifier(), json));
    }

    public static <T> Optional<String> encodeToJson(@NotNull FlagKey<T> key, T value) {
        var result = key.encode(JsonOps.COMPRESSED, value);
        if (result.isError()) {
            LOGGER.error("Failed to encode flag \"{}\": {}", key.identifier(), result.error().orElseThrow().message());
            return Optional.empty();
        }
        return result.result().map(GSON::toJson);
    }

    public static <T> Optional<String> encodeToJson(@NotNull FlagKey<T> key) {
        return encodeToJson(key, key.defaultValue());
    }
}
