package net.aros.canon.event;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.registration.EventSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface FlagEventHandler {
    <S, T> void addChangeListener(@NotNull FlagKey<S, T> flagKey, ChangeEvent<S, T> listener);

    <S, T> Collection<ChangeEvent<S, T>> getChangeListeners(@NotNull FlagKey<S, T> flagKey);

    void subscribeEvent(EventSubscription eventSubscription);
}
