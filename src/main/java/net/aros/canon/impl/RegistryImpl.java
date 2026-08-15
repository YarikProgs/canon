package net.aros.canon.impl;

import net.aros.canon.registry.MutableRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class RegistryImpl<T> implements MutableRegistry<T> {
    private final Map<ResourceLocation, T> contents = new HashMap<>();

    @Override
    public void replaceWith(Map<ResourceLocation, T> contents) {
        this.contents.clear();
        this.contents.putAll(contents);
    }

    @Override
    public T get(ResourceLocation identifier) {
        return contents.get(identifier);
    }
}
