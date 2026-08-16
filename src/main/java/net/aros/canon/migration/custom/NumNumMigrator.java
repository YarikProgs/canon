package net.aros.canon.migration.custom;

import net.aros.canon.core.state.type.BuiltinStateTypes;
import net.aros.canon.core.state.type.StateType;
import net.aros.canon.migration.StateMigrator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class NumNumMigrator implements StateMigrator {
    static final List<StateType<?>> BUILTIN_NUMERIC_TYPES = List.of(
            BuiltinStateTypes.BYTE, BuiltinStateTypes.SHORT, BuiltinStateTypes.INT,
            BuiltinStateTypes.LONG, BuiltinStateTypes.FLOAT, BuiltinStateTypes.DOUBLE
    );

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(StateType<T> givenType, T value, StateType<R> expectedType) {
        if (!BUILTIN_NUMERIC_TYPES.contains(givenType) || !BUILTIN_NUMERIC_TYPES.contains(expectedType))
            return Optional.empty();

        Number number = (Number) value;

        if (expectedType == BuiltinStateTypes.BYTE)
            return (Optional<R>) Optional.of(number.byteValue());
        if (expectedType == BuiltinStateTypes.SHORT)
            return (Optional<R>) Optional.of(number.shortValue());
        if (expectedType == BuiltinStateTypes.INT)
            return (Optional<R>) Optional.of(number.intValue());
        if (expectedType == BuiltinStateTypes.LONG)
            return (Optional<R>) Optional.of(number.longValue());
        if (expectedType == BuiltinStateTypes.FLOAT)
            return (Optional<R>) Optional.of(number.floatValue());
        if (expectedType == BuiltinStateTypes.DOUBLE)
            return (Optional<R>) Optional.of(number.doubleValue());

        return Optional.empty();
    }
}
