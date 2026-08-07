package net.aros.canon.migration.custom;

import net.aros.canon.core.flag.type.BuiltinFlagTypes;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.migration.FlagMigrator;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class StringRLMigrator implements FlagMigrator {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(FlagType<T> givenType, T value, FlagType<R> expectedType) {
        if (givenType == BuiltinFlagTypes.STRING && expectedType == BuiltinFlagTypes.OPT_RESOURCE_LOCATION) {
            String input = (String) value;
            return (Optional<R>) Optional.of(input.isEmpty() ? Optional.empty() : Optional.of(ResourceLocation.parse(input)));
        }
        if (givenType == BuiltinFlagTypes.OPT_RESOURCE_LOCATION && expectedType == BuiltinFlagTypes.STRING) {
            Optional<ResourceLocation> input = (Optional<ResourceLocation>) value;
            return (Optional<R>) Optional.of(input.map(ResourceLocation::toString).orElse(""));
        }
        return Optional.empty();
    }
}
