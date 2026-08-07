package net.aros.canon.migration.custom;

import net.aros.canon.core.flag.type.BuiltinFlagTypes;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.migration.FlagMigrator;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public class NumNumMigrator implements FlagMigrator {
    static final List<FlagType<?>> BUILTIN_NUMERIC_TYPES = List.of(
            BuiltinFlagTypes.BYTE, BuiltinFlagTypes.SHORT, BuiltinFlagTypes.INT,
            BuiltinFlagTypes.LONG, BuiltinFlagTypes.FLOAT, BuiltinFlagTypes.DOUBLE
    );

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(FlagType<T> givenType, T value, FlagType<R> expectedType) {
        if (!BUILTIN_NUMERIC_TYPES.contains(givenType) || !BUILTIN_NUMERIC_TYPES.contains(expectedType))
            return Optional.empty();

        Number number = (Number) value;

        if (expectedType == BuiltinFlagTypes.BYTE)
            return (Optional<R>) Optional.of(number.byteValue());
        if (expectedType == BuiltinFlagTypes.SHORT)
            return (Optional<R>) Optional.of(number.shortValue());
        if (expectedType == BuiltinFlagTypes.INT)
            return (Optional<R>) Optional.of(number.intValue());
        if (expectedType == BuiltinFlagTypes.LONG)
            return (Optional<R>) Optional.of(number.longValue());
        if (expectedType == BuiltinFlagTypes.FLOAT)
            return (Optional<R>) Optional.of(number.floatValue());
        if (expectedType == BuiltinFlagTypes.DOUBLE)
            return (Optional<R>) Optional.of(number.doubleValue());

        return Optional.empty();
    }
}
