package net.aros.canon.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.event.ChangeEvent;
import net.aros.canon.event.FlagListeners;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class FlagListenersImpl implements FlagListeners {
    private final Multimap<FlagKey<?>, ChangeEvent<?>> eventListeners = ArrayListMultimap.create();

    @Override
    public <T> void addChangeListener(@NotNull FlagKey<T> flagKey, ChangeEvent<T> listener) {
        eventListeners.put(flagKey, listener);
    }

    @Override
    public <T> Collection<ChangeEvent<T>> getChangeListeners(@NotNull FlagKey<T> flagKey) {
        //noinspection unchecked
        return (Collection<ChangeEvent<T>>) (Object) eventListeners.get(flagKey);
    }
}
