package net.aros.canon.event;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.registration.FlagRegistrationEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.List;

public class FlagHooks {
    public static <T> void flagChanged(FlagKey<T> flagKey, T before, T current) {
        for (ChangeEvent<T> listener : Canon.get().flagEventHandler().getChangeListeners(flagKey)) {
            listener.onChange(before, current);
        }
    }

    public static List<FlagKey<?>> registerAllKeys() {
        return NeoForge.EVENT_BUS.post(new FlagRegistrationEvent()).registeredKeys();
    }
}
