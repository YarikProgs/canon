package net.aros.canon.core.flag;

import net.aros.canon.tx.Sandbox;
import org.jetbrains.annotations.NotNull;

public interface FlagStore {
    <T> void set(FlagKey<T> key, T value);

    <T> void setLive(@NotNull FlagKey<T> key, T value);

    <T> T get(@NotNull FlagKey<T> key);

    void setOwnership(@NotNull FlagKey<?> key, Sandbox sandbox);

    void removeOwnership(@NotNull FlagKey<?> key, Sandbox sandbox);

    Sandbox getOwner(@NotNull FlagKey<?> key);
}
