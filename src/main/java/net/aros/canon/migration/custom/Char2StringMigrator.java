package net.aros.canon.migration.custom;

import net.aros.canon.core.flag.type.BuiltinFlagTypes;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.migration.FlagMigrator;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Char2StringMigrator implements FlagMigrator {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(FlagType<T> givenType, T value, FlagType<R> expectedType) {
        if (givenType == BuiltinFlagTypes.CHAR && expectedType == BuiltinFlagTypes.STRING)
            return Optional.of((R) String.valueOf(value));
        return Optional.empty();
    }
}
