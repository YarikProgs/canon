package net.aros.canon.event.custom;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.registration.FlagRegistration;
import net.aros.canon.core.flag.type.FlagType;
import net.neoforged.bus.api.Event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FlagRegistrationEvent extends Event {
    private final Set<FlagType<?>> registeredTypes = new HashSet<>();
    private final Set<FlagKey<?>> registeredKeys = new HashSet<>();

    public final void registerTypes(FlagType<?>... flagTypes) {
        registeredTypes.addAll(List.of(flagTypes));
    }

    public <T> FlagRegistration<T> registerFlag(FlagKey<T> flagKey) {
        registeredKeys.add(flagKey);
        return new FlagRegistration<>(flagKey);
    }

    public Set<FlagType<?>> registeredTypes() {
        return registeredTypes;
    }

    public Set<FlagKey<?>> registeredKeys() {
        return registeredKeys;
    }
}
