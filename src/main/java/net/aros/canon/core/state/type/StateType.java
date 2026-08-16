package net.aros.canon.core.state.type;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;

public record StateType<T>(ResourceLocation identifier, Codec<T> codec, T defaultValue) {
}
