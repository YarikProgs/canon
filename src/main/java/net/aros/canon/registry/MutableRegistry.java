package net.aros.canon.registry;

import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public interface MutableRegistry<T> extends Registry<T> {
    void replaceWith(Map<ResourceLocation, T> contents);
}
