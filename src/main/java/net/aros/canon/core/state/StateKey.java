package net.aros.canon.core.state;

import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.core.state.type.StateType;
import net.minecraft.resources.ResourceLocation;

public record StateKey<S, T>(ScopeType<S> scopeType, ResourceLocation identifier, StateType<T> type) {
}
