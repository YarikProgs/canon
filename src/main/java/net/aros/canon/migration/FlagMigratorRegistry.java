package net.aros.canon.migration;

import net.aros.canon.core.flag.type.FlagType;

import java.util.Optional;

public interface FlagMigratorRegistry {
    void register(FlagMigrator migrator);

    <T, R> Optional<R> tryMigrate(FlagType<T> givenType, T value, FlagType<R> expectedType);
}
