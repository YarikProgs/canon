package net.aros.canon.impl;

import net.aros.canon.core.state.type.StateType;
import net.aros.canon.migration.StateMigrator;
import net.aros.canon.migration.StateMigratorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StateMigratorRegistryImpl implements StateMigratorRegistry {
    private final List<StateMigrator> migrators = new ArrayList<>();

    @Override
    public <T, R> Optional<R> tryMigrate(StateType<T> givenType, T value, StateType<R> expectedType) {
        for (StateMigrator migrator : migrators) {
            var result = migrator.migrate(givenType, value, expectedType);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public void register(StateMigrator migrator) {
        migrators.add(migrator);
    }
}
