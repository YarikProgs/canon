package net.aros.canon.impl;

import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.migration.FlagMigrator;
import net.aros.canon.migration.FlagMigratorRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FlagMigratorRegistryImpl implements FlagMigratorRegistry {
    private final List<FlagMigrator> migrators = new ArrayList<>();

    @Override
    public <T, R> Optional<R> tryMigrate(FlagType<T> givenType, T value, FlagType<R> expectedType) {
        for (FlagMigrator migrator : migrators) {
            var result = migrator.migrate(givenType, value, expectedType);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public void register(FlagMigrator migrator) {
        migrators.add(migrator);
    }
}
