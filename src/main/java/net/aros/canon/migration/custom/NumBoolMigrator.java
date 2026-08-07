package net.aros.canon.migration.custom;

import net.aros.canon.core.flag.type.BuiltinFlagTypes;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.migration.FlagMigrator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NumBoolMigrator implements FlagMigrator {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(FlagType<T> givenType, T value, FlagType<R> expectedType) {
        if (givenType == BuiltinFlagTypes.BOOL && NumNumMigrator.BUILTIN_NUMERIC_TYPES.contains(expectedType)) {
            return (Optional<R>) Optional.of((Boolean) value ? 1 : 0);
        }
        if (NumNumMigrator.BUILTIN_NUMERIC_TYPES.contains(givenType) && expectedType == BuiltinFlagTypes.BOOL) {
            return (Optional<R>) Optional.of(((Number) value).doubleValue() != 0);
        }
        return Optional.empty();
    }
}
