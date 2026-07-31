package net.aros.canon.event;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class EventKey<T> {
    private final int id = CanonEventBus.ID_GENERATOR.getAndIncrement();
    private final String name;

    public EventKey(String name) {
        this.name = name;
    }

    @Contract("_ -> new")
    public static @NotNull EventKey<Void> of(String key) {
        return new EventKey<>(key);
    }

    @Contract(value = "_, _ -> new")
    public static <P> @NotNull EventKey<P> of(String key, Class<P> ignored) {
        return new EventKey<>(key);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }
}
