package net.aros.canon.event;

import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.registration.EventSubscription;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface StateEventHandler {
    <S, T> void addChangeListener(@NotNull StateKey<S, T> stateKey, ChangeEvent<S, T> listener);

    <S, T> Collection<ChangeEvent<S, T>> getChangeListeners(@NotNull StateKey<S, T> stateKey);

    void subscribeEvent(EventSubscription eventSubscription);
}
