package net.aros.canon.core.flag.type;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public record FlagType<T>(ResourceLocation identifier, Codec<T> codec, T defaultValue) {
}
