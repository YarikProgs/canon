package net.aros.canon.event;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.registration.EventSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface FlagEventHandler {
    <T> void addChangeListener(@NotNull FlagKey<T> flagKey, ChangeEvent<T> listener);

    <T> Collection<ChangeEvent<T>> getChangeListeners(@NotNull FlagKey<T> flagKey);

    void subscribeEvent(EventSubscription eventSubscription);
}
