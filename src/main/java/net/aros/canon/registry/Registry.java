package net.aros.canon.registry;

import net.minecraft.resources.ResourceLocation;

public interface Registry<T> {
    T get(ResourceLocation identifier);
}
