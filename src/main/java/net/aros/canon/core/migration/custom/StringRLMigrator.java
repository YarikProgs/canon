package net.aros.canon.core.migration.custom;

import net.aros.canon.core.migration.StateMigrator;
import net.aros.canon.core.state.type.BuiltinStateTypes;
import net.aros.canon.core.state.type.StateType;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class StringRLMigrator implements StateMigrator {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(StateType<T> givenType, T value, StateType<R> expectedType) {
        if (givenType == BuiltinStateTypes.STRING && expectedType == BuiltinStateTypes.OPT_RESOURCE_LOCATION) {
            String input = (String) value;
            return (Optional<R>) Optional.of(input.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(input)));
        }
        if (givenType == BuiltinStateTypes.OPT_RESOURCE_LOCATION && expectedType == BuiltinStateTypes.STRING) {
            Optional<ResourceLocation> input = (Optional<ResourceLocation>) value;
            return (Optional<R>) Optional.of(input.map(ResourceLocation::toString).orElse(""));
        }
        return Optional.empty();
    }
}
