package net.aros.canon.core.flag.type;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface FlagTypeRegistry {
    void register(FlagType<?> type);

    Optional<FlagType<?>> get(ResourceLocation identifier);
}
