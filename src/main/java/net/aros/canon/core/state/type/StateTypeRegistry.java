package net.aros.canon.core.state.type;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public interface StateTypeRegistry {
    void register(StateType<?> type);

    Optional<StateType<?>> get(ResourceLocation identifier);
}
