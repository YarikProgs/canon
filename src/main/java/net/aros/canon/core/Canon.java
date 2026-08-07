package net.aros.canon.core;

import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.core.flag.type.FlagTypeRegistry;
import net.aros.canon.impl.FlagMigratorRegistryImpl;
import net.aros.canon.impl.FlagTypeRegistryImpl;
import net.aros.canon.migration.FlagMigratorRegistry;
import net.aros.canon.event.FlagEventHandler;
import net.aros.canon.impl.FlagEventHandlerImpl;
import net.aros.canon.impl.FlagReloadListener;
import net.aros.canon.impl.store.FlagStoreImpl;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

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

    private final FlagStoreImpl store = new FlagStoreImpl();
    private final FlagTypeRegistryImpl typeRegistry = new FlagTypeRegistryImpl();
    private final FlagEventHandlerImpl eventHandler = new FlagEventHandlerImpl();
    private final FlagMigratorRegistry migratorRegistry = new FlagMigratorRegistryImpl();

    public FlagStore flagStore() {
        return store;
    }

    public FlagTypeRegistry flagTypeRegistry() {
        return typeRegistry;
    }

    public FlagEventHandler flagEventHandler() {
        return eventHandler;
    }

    public FlagMigratorRegistry migratorRegistry() {
        return migratorRegistry;
    }

    private void onServerAboutToStart(@NotNull ServerAboutToStartEvent event) {
        store.createConnection(event.getServer());
        new FlagReloadListener(eventHandler, store, typeRegistry).simpleReload().join();
    }

    private void onAddReloadListener(@NotNull AddReloadListenerEvent event) {
        event.addListener(new FlagReloadListener(eventHandler, store, typeRegistry));
    }

    private void onServerShutdown(ServerStoppedEvent event) {
        store.closeConnection();
    }
}
