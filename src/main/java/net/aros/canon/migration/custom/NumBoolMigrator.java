package net.aros.canon.migration.custom;

import net.aros.canon.core.state.type.BuiltinStateTypes;
import net.aros.canon.core.state.type.StateType;
import net.aros.canon.migration.StateMigrator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NumBoolMigrator implements StateMigrator {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(StateType<T> givenType, T value, StateType<R> expectedType) {
        if (givenType == BuiltinStateTypes.BOOL && NumNumMigrator.BUILTIN_NUMERIC_TYPES.contains(expectedType)) {
            return (Optional<R>) Optional.of((Boolean) value ? 1 : 0);
        }
        if (NumNumMigrator.BUILTIN_NUMERIC_TYPES.contains(givenType) && expectedType == BuiltinStateTypes.BOOL) {
            return (Optional<R>) Optional.of(((Number) value).doubleValue() != 0);
        }
        return Optional.empty();
    }
}
