package net.aros.canon.event;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public interface CanonEventBus {
    AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    <P> void addListener(EventKey<P> event, Consumer<P> listener);

    <P> void fire(EventKey<P> event, P payload);

    void fire(EventKey<Void> event);
}
