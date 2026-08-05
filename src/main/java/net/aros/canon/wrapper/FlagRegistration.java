package net.aros.canon.wrapper;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.event.ChangeEvent;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.common.NeoForge;

public class FlagRegistration<T> {
    private final FlagKey<T> flagKey;

    FlagRegistration(FlagKey<T> flagKey) {
        this.flagKey = flagKey;
    }

    public <E extends Event> FlagRegistration<T> addEventListener(Class<E> event, FlagEventHandler<T, E> listener) {
        NeoForge.EVENT_BUS.addListener(event, e -> listener.accept(flagKey, Can.get(flagKey), e));
        return this;
    }

    public FlagRegistration<T> addChangeListener(ChangeEvent<T> event) {
        Canon.get().flagListeners().addChangeListener(flagKey, event);
        return this;
    }

    public FlagKey<T> buildAndRegister() {
        return Canon.get().flagRegistry().register(flagKey);
    }

    public interface FlagEventHandler<F, E extends Event> {
        void accept(FlagKey<F> key, F flag, E event);
    }
}
