package net.aros.canon.core.flag.registration;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.event.ChangeEvent;
import net.aros.canon.impl.EventSubscriptionImpl;
import net.aros.canon.wrapper.Can;
import net.neoforged.bus.api.Event;

public class FlagRegistration<T> {
    private final FlagKey<T> flagKey;

    FlagRegistration(FlagKey<T> flagKey) {
        this.flagKey = flagKey;
    }

    public <E extends Event> FlagRegistration<T> addEventListener(Class<E> eventClass, FlagEventHandler<T, E> listener) {
        Canon.get().flagEventHandler().subscribeEvent(new EventSubscriptionImpl<>(
                eventClass,
                e -> listener.accept(flagKey, Can.get(flagKey), e)
        ));
        return this;
    }

    public FlagRegistration<T> addChangeListener(ChangeEvent<T> event) {
        Canon.get().flagEventHandler().addChangeListener(flagKey, event);
        return this;
    }

    public interface FlagEventHandler<F, E extends Event> {
        void accept(FlagKey<F> key, F flag, E event);
    }
}
