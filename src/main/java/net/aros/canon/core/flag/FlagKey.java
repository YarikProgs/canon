package net.aros.canon.core.flag;

import net.aros.canon.core.flag.scope.ScopeType;
import net.aros.canon.core.flag.type.FlagType;
import net.minecraft.resources.ResourceLocation;

public record FlagKey<S, T>(ScopeType<S> scopeType, ResourceLocation identifier, FlagType<T> type) {
}
