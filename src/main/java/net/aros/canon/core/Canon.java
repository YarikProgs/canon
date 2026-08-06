package net.aros.canon.core;

import net.aros.canon.core.flag.FlagRegistry;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagEventHandler;
import net.aros.canon.impl.FlagEventHandlerImpl;
import net.aros.canon.impl.FlagRegistryImpl;
import net.aros.canon.impl.FlagReloadListener;
import net.minecraft.Util;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class Canon {
    private static final Canon INSTANCE = new Canon();

    public static Canon get() {
        return INSTANCE;
    }

    private Canon() {
        NeoForge.EVENT_BUS.addListener(this::onServerShutdown);
        NeoForge.EVENT_BUS.addListener(this::onAddReloadListener);
        NeoForge.EVENT_BUS.addListener(this::onServerAboutToStart);
    }

    private final FlagRegistryImpl flagRegistry = new FlagRegistryImpl();
    private final FlagStore flagStore = new FlagStore();
    private final FlagEventHandlerImpl eventHandler = new FlagEventHandlerImpl();

    public FlagRegistry flagRegistry() {
        return flagRegistry;
    }

    public FlagStore flagStore() {
        return flagStore;
    }

    public FlagEventHandler flagEventHandler() {
        return eventHandler;
    }

    private void onServerAboutToStart(ServerAboutToStartEvent event) {
        flagStore.createConnection(event.getServer());
        new FlagReloadListener(flagRegistry, eventHandler, flagStore).simpleReload();
    }

    private void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(new FlagReloadListener(flagRegistry, eventHandler, flagStore));
    }

    private void onServerShutdown(ServerStoppedEvent event) {
        flagStore.closeConnection();
    }
}
