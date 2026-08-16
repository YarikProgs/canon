package net.aros.canon.impl;

import net.aros.canon.core.state.registration.EventSubscription;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class EventSubscriptionImpl<E extends Event> implements EventSubscription {
    private final Consumer<E> registeredConsumer;

    public EventSubscriptionImpl(Class<E> eventClass, @NotNull Consumer<E> registeredConsumer) {
        this.registeredConsumer = registeredConsumer;
        NeoForge.EVENT_BUS.addListener(eventClass, this.registeredConsumer);
    }

    @Override
    public void unsubscribe() {
        NeoForge.EVENT_BUS.unregister(registeredConsumer);
    }
}
