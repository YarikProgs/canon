package net.aros.canon.event.custom;

import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.registration.StateRegistration;
import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.core.state.type.StateType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StateRegistrationEvent extends Event {
    private final Map<ResourceLocation, StateType<?>> registeredTypes = new HashMap<>();
    private final Map<ResourceLocation, ScopeType<?>> registeredScopeTypes = new HashMap<>();
    private final Set<StateKey<?, ?>> registeredKeys = new HashSet<>();

    public void registerType(StateType<?> stateType) {
        registeredTypes.put(stateType.identifier(), stateType);
    }

    public void registerScopeType(ScopeType<?> scopeType) {
        registeredScopeTypes.put(scopeType.identifier(), scopeType);
    }

    public <S, T> StateRegistration<S, T> registerStateKey(StateKey<S, T> stateKey) {
        registeredKeys.add(stateKey);
        return new StateRegistration<>(stateKey);
    }

    public Map<ResourceLocation, StateType<?>> registeredTypes() {
        return registeredTypes;
    }

    public Map<ResourceLocation, ScopeType<?>> registeredScopeTypes() {
        return registeredScopeTypes;
    }

    public Set<StateKey<?, ?>> registeredKeys() {
        return registeredKeys;
    }
}
