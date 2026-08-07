package net.aros.canon.impl;

import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.core.flag.type.FlagTypeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FlagTypeRegistryImpl implements FlagTypeRegistry {
    private final Map<ResourceLocation, FlagType<?>> flagTypes = new HashMap<>();

    @Override
    public void register(FlagType<?> type) {
        flagTypes.put(type.identifier(), type);
    }

    @Override
    public Optional<FlagType<?>> get(ResourceLocation identifier) {
        return Optional.ofNullable(flagTypes.get(identifier));
    }

    public void clear() {
        flagTypes.clear();
    }

    public void putAll(@NotNull Collection<FlagType<?>> flagTypes) {
        for (FlagType<?> flagType : flagTypes) {
            this.flagTypes.put(flagType.identifier(), flagType);
        }
    }
}
