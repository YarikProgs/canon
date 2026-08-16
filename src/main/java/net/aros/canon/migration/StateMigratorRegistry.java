package net.aros.canon.migration;

import net.aros.canon.core.state.type.StateType;

import java.util.Optional;

public interface StateMigratorRegistry {
    void register(StateMigrator migrator);

    <T, R> Optional<R> tryMigrate(StateType<T> givenType, T value, StateType<R> expectedType);
}
