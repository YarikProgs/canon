package net.aros.canon.event;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;

public class FlagHooks {
    public static <T> void flagChanged(FlagKey<T> flagKey, T before, T current) {
        for (ChangeEvent<T> listener : Canon.get().flagListeners().getChangeListeners(flagKey)) {
            listener.onChange(before, current);
        }
    }
}
