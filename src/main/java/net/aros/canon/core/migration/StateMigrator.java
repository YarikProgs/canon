package net.aros.canon.core.migration;

import net.aros.canon.core.state.type.StateType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface StateMigrator {
    @NotNull
    <T, R> Optional<R> migrate(StateType<T> givenType, T value, StateType<R> expectedType);
}
