package net.aros.canon.core.flag.registration;

import net.aros.canon.core.flag.FlagKey;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

public class FlagRegistrationEvent extends Event {
    private final List<FlagKey<?>> registeredKeys = new ArrayList<>();

    public <T> FlagRegistration<T> register(FlagKey<T> flagKey) {
        registeredKeys.add(flagKey);
        return new FlagRegistration<>(flagKey);
    }

    public List<FlagKey<?>> registeredKeys() {
        return registeredKeys;
    }
}
