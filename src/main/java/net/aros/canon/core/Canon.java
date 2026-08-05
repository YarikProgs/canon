package net.aros.canon.core;

import net.aros.canon.core.flag.FlagRegistry;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagListeners;
import net.aros.canon.impl.FlagListenersImpl;
import net.aros.canon.impl.FlagRegistryImpl;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import static net.aros.canon.CanonLibMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class Canon {
    private static final Canon INSTANCE = new Canon();

    public static Canon get() {
        return INSTANCE;
    }

    private final FlagRegistry flagRegistry = new FlagRegistryImpl();
    private final FlagStore flagStore = new FlagStore();
    private final FlagListeners flagListeners = new FlagListenersImpl();

    public FlagRegistry flagRegistry() {
        return flagRegistry;
    }

    public FlagStore flagStore() {
        return flagStore;
    }

    public FlagListeners flagListeners() {
        return flagListeners;
    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppedEvent event) {
        INSTANCE.flagStore.serverShutdown();
    }

    @SubscribeEvent
    public static void onServerStart(ServerAboutToStartEvent event) {
        INSTANCE.flagStore.serverAboutToStart(event.getServer(), INSTANCE.flagRegistry);
    }
}
