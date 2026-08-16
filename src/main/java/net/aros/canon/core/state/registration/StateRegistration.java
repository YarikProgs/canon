package net.aros.canon.core.state.registration;

import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.event.ChangeEvent;
import net.aros.canon.impl.EventSubscriptionImpl;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.ApiStatus;

public class StateRegistration<S, T> {
    private final StateKey<S, T> stateKey;

    @ApiStatus.Internal
    public StateRegistration(StateKey<S, T> stateKey) {
        this.stateKey = stateKey;
    }

    public <E extends Event> StateRegistration<S, T> addEventListener(Class<E> eventClass, StateEventHandler<S, T, E> listener) {
        Canon.get().stateEventHandler().subscribeEvent(new EventSubscriptionImpl<>(
                eventClass,
                e -> listener.accept(stateKey, e)
        ));
        return this;
    }

    public StateRegistration<S, T> addChangeListener(ChangeEvent<S, T> event) {
        Canon.get().stateEventHandler().addChangeListener(stateKey, event);
        return this;
    }

    public interface StateEventHandler<S, F, E extends Event> {
        void accept(StateKey<S, F> key, E event);
    }
}
