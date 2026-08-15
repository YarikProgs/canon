package net.aros.canon.core;

import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.core.flag.scope.ScopeType;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.event.FlagEventHandler;
import net.aros.canon.impl.*;
import net.aros.canon.migration.FlagMigratorRegistry;
import net.aros.canon.registry.MutableRegistry;
import net.aros.canon.registry.Registry;
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

    private final FlagsDB db = new FlagsDB();
    private final FlagStoreImpl store = new FlagStoreImpl();
    private final MutableRegistry<FlagType<?>> typeRegistry = new RegistryImpl<>();
    private final MutableRegistry<ScopeType<?>> scopeRegistry = new RegistryImpl<>();
    private final FlagEventHandlerImpl eventHandler = new FlagEventHandlerImpl();
    private final FlagMigratorRegistry migratorRegistry = new FlagMigratorRegistryImpl();

    public FlagStore flagStore() {
        return store;
    }

    public Registry<FlagType<?>> flagTypeRegistry() {
        return typeRegistry;
    }

    public Registry<ScopeType<?>> scopeTypeRegistry() {
        return scopeRegistry;
    }

    public FlagEventHandler flagEventHandler() {
        return eventHandler;
    }

    public FlagMigratorRegistry migratorRegistry() {
        return migratorRegistry;
    }

    public FlagsDB db() {
        return db;
    }

    private void onServerAboutToStart(@NotNull ServerAboutToStartEvent event) {
        store.createConnection(event.getServer(), db);
        new FlagReloadListener(eventHandler, store, typeRegistry, scopeRegistry, db).simpleReload().join();
    }

    private void onAddReloadListener(@NotNull AddReloadListenerEvent event) {
        event.addListener(new FlagReloadListener(eventHandler, store, typeRegistry, scopeRegistry, db));
    }

    private void onServerShutdown(ServerStoppedEvent event) {
        store.closeConnection(db);
    }
}
