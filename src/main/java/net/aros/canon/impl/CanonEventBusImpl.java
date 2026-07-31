package net.aros.canon.impl;

import net.aros.canon.event.CanonEventBus;
import net.aros.canon.event.EventKey;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;

public class CanonEventBusImpl implements CanonEventBus {
    private volatile AtomicReferenceArray<Consumer<?>[]> subscribers = new AtomicReferenceArray<>(128);

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <P> void addListener(EventKey<P> event, Consumer<P> listener) {
        ensureCapacity(event.id());

        Consumer<P>[] listeners = (Consumer<P>[]) subscribers.get(event.id());

        Consumer<P>[] newListeners;
        if (listeners == null) {
            newListeners = new Consumer[]{listener};
        } else {
            newListeners = Arrays.copyOf(listeners, listeners.length + 1);
            newListeners[newListeners.length - 1] = listener;
        }
        subscribers.set(event.id(), newListeners);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <P> void fire(EventKey<P> event, P payload) {
        if (event.id() >= subscribers.length()) return;
        Consumer<P>[] listeners = (Consumer<P>[]) subscribers.get(event.id());
        if (listeners != null) {
            for (Consumer<P> listener : listeners) {
                listener.accept(payload);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void fire(EventKey<Void> event) {
        if (event.id() >= subscribers.length()) return;
        Consumer<Void>[] listeners = (Consumer<Void>[]) subscribers.get(event.id());
        if (listeners != null) {
            for (Consumer<Void> listener : listeners) {
                listener.accept(null);
            }
        }
    }

    private void ensureCapacity(int neededId) {
        if (neededId >= subscribers.length()) {
            int newSize = Math.max(subscribers.length() * 2, neededId + 1);
            AtomicReferenceArray<Consumer<?>[]> newSubscribers = new AtomicReferenceArray<>(newSize);

            for (int i = 0; i < subscribers.length(); i++) {
                newSubscribers.set(i, subscribers.get(i));
            }

            this.subscribers = newSubscribers;
        }
    }
}
