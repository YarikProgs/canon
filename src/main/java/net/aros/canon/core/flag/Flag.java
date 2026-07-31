package net.aros.canon.core.flag;

import net.aros.canon.core.Canon;
import net.aros.canon.event.EventKey;

import java.util.function.BiConsumer;

public interface Flag<T> {
    FlagKey<T> key();
    T get();
    void set(T value);

    default <P> Flag<T> hook(EventKey<P> event, BiConsumer<Flag<T>, P> handler) {
        Canon.get().events().addListener(event, p -> handler.accept(this, p));
        return this;
    }

//    Flag<T> onTransition(T from, T to, Runnable handler);
//    Flag<T> onChange(BiConsumer<T, T> oldNewHandler);
}
