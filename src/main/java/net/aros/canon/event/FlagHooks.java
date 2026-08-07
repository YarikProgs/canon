package net.aros.canon.event;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.event.custom.FlagRegistrationEvent;
import net.aros.canon.event.custom.FlagResetEvent;
import net.neoforged.neoforge.common.NeoForge;

public class FlagHooks {
    public static <T> void flagChanged(FlagKey<T> flagKey, T before, T current) {
        for (ChangeEvent<T> listener : Canon.get().flagEventHandler().getChangeListeners(flagKey)) {
            listener.onChange(before, current);
        }
    }

    public static FlagRegistrationEvent fireFlagRegistration() {
        return NeoForge.EVENT_BUS.post(new FlagRegistrationEvent());
    }

    public static void fireFlagReset() {
        NeoForge.EVENT_BUS.post(new FlagResetEvent());
    }
}
