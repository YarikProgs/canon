package net.aros.canon.event.custom;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.registration.FlagRegistration;
import net.aros.canon.core.flag.scope.ScopeType;
import net.aros.canon.core.flag.type.FlagType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FlagRegistrationEvent extends Event {
    private final Map<ResourceLocation, FlagType<?>> registeredTypes = new HashMap<>();
    private final Map<ResourceLocation, ScopeType<?>> registeredScopeTypes = new HashMap<>();
    private final Set<FlagKey<?, ?>> registeredKeys = new HashSet<>();

    public void registerType(FlagType<?> flagType) {
        registeredTypes.put(flagType.identifier(), flagType);
    }

    public void registerScopeType(ScopeType<?> scopeType) {
        registeredScopeTypes.put(scopeType.identifier(), scopeType);
    }

    public <S, T> FlagRegistration<S, T> registerFlag(FlagKey<S, T> flagKey) {
        registeredKeys.add(flagKey);
        return new FlagRegistration<>(flagKey);
    }

    public Map<ResourceLocation, FlagType<?>> registeredTypes() {
        return registeredTypes;
    }

    public Map<ResourceLocation, ScopeType<?>> registeredScopeTypes() {
        return registeredScopeTypes;
    }

    public Set<FlagKey<?, ?>> registeredKeys() {
        return registeredKeys;
    }
}
