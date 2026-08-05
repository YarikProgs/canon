package net.aros.canon.event;

import net.aros.canon.core.flag.FlagKey;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface FlagListeners {
    <T> void addChangeListener(@NotNull FlagKey<T> flagKey, ChangeEvent<T> listener);

    <T> Collection<ChangeEvent<T>> getChangeListeners(@NotNull FlagKey<T> flagKey);
}
