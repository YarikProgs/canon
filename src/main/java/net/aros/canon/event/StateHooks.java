package net.aros.canon.event;

import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.event.custom.StateRegistrationEvent;
import net.aros.canon.event.custom.StateResetEvent;
import net.neoforged.neoforge.common.NeoForge;

public class StateHooks {
    public static <S, T> void stateChanged(StateKey<S, T> stateKey, S scope, T before, T current) {
        for (ChangeEvent<S, T> listener : Canon.get().stateEventHandler().getChangeListeners(stateKey)) {
            listener.onChange(scope, before, current);
        }
    }

    public static StateRegistrationEvent fireStateRegistration() {
        return NeoForge.EVENT_BUS.post(new StateRegistrationEvent());
    }

    public static void fireStateReset() {
        NeoForge.EVENT_BUS.post(new StateResetEvent());
    }
}
