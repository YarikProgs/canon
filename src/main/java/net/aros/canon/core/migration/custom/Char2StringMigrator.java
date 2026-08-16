package net.aros.canon.core.migration.custom;

import net.aros.canon.core.migration.StateMigrator;
import net.aros.canon.core.state.type.BuiltinStateTypes;
import net.aros.canon.core.state.type.StateType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class Char2StringMigrator implements StateMigrator {
    @SuppressWarnings("unchecked")
    @Override
    public @NotNull <T, R> Optional<R> migrate(StateType<T> givenType, T value, StateType<R> expectedType) {
        if (givenType == BuiltinStateTypes.CHAR && expectedType == BuiltinStateTypes.STRING)
            return Optional.of((R) String.valueOf(value));
        return Optional.empty();
    }
}
