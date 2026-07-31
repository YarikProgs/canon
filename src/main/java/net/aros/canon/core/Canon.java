package net.aros.canon.core;

import net.aros.canon.core.executors.ThreadExecutor;
import net.aros.canon.core.flag.FlagRegistry;
import net.aros.canon.event.CanonEventBus;
import net.aros.canon.impl.CanonEventBusImpl;
import net.aros.canon.impl.FlagRegistryImpl;
import net.aros.canon.impl.ScriptThreadExecutor;
import net.aros.canon.impl.ServerThreadExecutor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import static net.aros.canon.CanonLibMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class Canon {
    private static final Canon INSTANCE = new Canon(
            new CanonEventBusImpl(),
            new FlagRegistryImpl()
    );

    public static Canon get() {
        return INSTANCE;
    }

    private final CanonEventBus eventBus;
    private final FlagRegistry flagRegistry;
    private ThreadExecutor scriptThreadExecutor;
    private ThreadExecutor mainThreadExecutor;

    public Canon(CanonEventBus eventBus, FlagRegistry flagRegistry) {
        this.eventBus = eventBus;
        this.flagRegistry = flagRegistry;
    }

    public ThreadExecutor mainThreadExecutor() {
        return mainThreadExecutor;
    }

    public ThreadExecutor scriptExecutor() {
        return scriptThreadExecutor;
    }

    public FlagRegistry flags() {
        return flagRegistry;
    }

    public CanonEventBus events() {
        return eventBus;
    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppedEvent event) {
        try {
            INSTANCE.scriptThreadExecutor.shutdown();
            INSTANCE.mainThreadExecutor.shutdown();
            INSTANCE.scriptThreadExecutor = null;
            INSTANCE.mainThreadExecutor = null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        INSTANCE.mainThreadExecutor = new ServerThreadExecutor(event.getServer());
        INSTANCE.scriptThreadExecutor = new ScriptThreadExecutor();
    }
}
