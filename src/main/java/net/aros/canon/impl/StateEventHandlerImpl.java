package net.aros.canon.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.registration.EventSubscription;
import net.aros.canon.event.ChangeEvent;
import net.aros.canon.event.MutableStateEventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class StateEventHandlerImpl implements MutableStateEventHandler {
    private final Multimap<StateKey<?, ?>, ChangeEvent<?, ?>> eventListeners = ArrayListMultimap.create();
    private final List<EventSubscription> subscriptions = new ArrayList<>();

    @Override
    public <S, T> void addChangeListener(@NotNull StateKey<S, T> stateKey, ChangeEvent<S, T> listener) {
        eventListeners.put(stateKey, listener);
    }

    @Override
    public <S, T> Collection<ChangeEvent<S, T>> getChangeListeners(@NotNull StateKey<S, T> stateKey) {
        //noinspection unchecked
        return (Collection<ChangeEvent<S, T>>) (Object) eventListeners.get(stateKey);
    }

    @Override
    public void subscribeEvent(EventSubscription subscription) {
        subscriptions.add(subscription);
    }

    @Override
    public void unsubscribeAllEvents() {
        subscriptions.forEach(EventSubscription::unsubscribe);
        subscriptions.clear();
    }

    @Override
    public void clearChangeListeners() {
        eventListeners.clear();
    }
}
