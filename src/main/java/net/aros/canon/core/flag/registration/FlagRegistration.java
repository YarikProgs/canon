package net.aros.canon.core.flag.registration;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.event.ChangeEvent;
import net.aros.canon.impl.EventSubscriptionImpl;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;

public class FlagRegistration<S, T> {
    private final FlagKey<S, T> flagKey;

    @ApiStatus.Internal
    public FlagRegistration(FlagKey<S, T> flagKey) {
        this.flagKey = flagKey;
    }

    public <E extends Event> FlagRegistration<S, T> addEventListener(Class<E> eventClass, FlagEventHandler<S, T, E> listener) {
        Canon.get().flagEventHandler().subscribeEvent(new EventSubscriptionImpl<>(
                eventClass,
                e -> listener.accept(flagKey, e)
        ));
        return this;
    }

    public FlagRegistration<S, T> addChangeListener(ChangeEvent<S, T> event) {
        Canon.get().flagEventHandler().addChangeListener(flagKey, event);
        return this;
    }

    public interface FlagEventHandler<S, F, E extends Event> {
        void accept(FlagKey<S, F> key, E event);
    }
}
