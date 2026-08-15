package net.aros.canon.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.registration.EventSubscription;
import net.aros.canon.event.ChangeEvent;
import net.aros.canon.event.FlagEventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FlagEventHandlerImpl implements FlagEventHandler {
    private final Multimap<FlagKey<?, ?>, ChangeEvent<?, ?>> eventListeners = ArrayListMultimap.create();
    private final List<EventSubscription> subscriptions = new ArrayList<>();

    @Override
    public <S, T> void addChangeListener(@NotNull FlagKey<S, T> flagKey, ChangeEvent<S, T> listener) {
        eventListeners.put(flagKey, listener);
    }

    @Override
    public <S, T> Collection<ChangeEvent<S, T>> getChangeListeners(@NotNull FlagKey<S, T> flagKey) {
        //noinspection unchecked
        return (Collection<ChangeEvent<S, T>>) (Object) eventListeners.get(flagKey);
    }

    @Override
    public void subscribeEvent(EventSubscription subscription) {
        subscriptions.add(subscription);
    }

    public void unsubscribeAllEvents() {
        subscriptions.forEach(EventSubscription::unsubscribe);
        subscriptions.clear();
    }

    public void clearChangeListeners() {
        eventListeners.clear();
    }
}
