package net.aros.canon.migration;

import net.aros.canon.core.flag.type.FlagType;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface FlagMigrator {
    @NotNull
    <T, R> Optional<R> migrate(FlagType<T> givenType, T value, FlagType<R> expectedType);
}
