package net.aros.canon.core;

import net.aros.canon.core.db.StatesDB;
import net.aros.canon.core.state.StateStore;
import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.core.state.type.StateType;
import net.aros.canon.event.StateEventHandler;
import net.aros.canon.impl.*;
import net.aros.canon.migration.StateMigratorRegistry;
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

    private final StatesDB db = new StatesDB();
    private final StateStoreImpl store = new StateStoreImpl();
    private final MutableRegistry<StateType<?>> typeRegistry = new RegistryImpl<>();
    private final MutableRegistry<ScopeType<?>> scopeRegistry = new RegistryImpl<>();
    private final StateEventHandlerImpl eventHandler = new StateEventHandlerImpl();
    private final StateMigratorRegistry migratorRegistry = new StateMigratorRegistryImpl();

    public StateStore stateStore() {
        return store;
    }

    public Registry<StateType<?>> stateTypeRegistry() {
        return typeRegistry;
    }

    public Registry<ScopeType<?>> scopeTypeRegistry() {
        return scopeRegistry;
    }

    public StateEventHandler stateEventHandler() {
        return eventHandler;
    }

    public StateMigratorRegistry migratorRegistry() {
        return migratorRegistry;
    }

    public StatesDB db() {
        return db;
    }

    private void onServerAboutToStart(@NotNull ServerAboutToStartEvent event) {
        store.createConnection(event.getServer(), db);
        new StateReloadListener(eventHandler, store, typeRegistry, scopeRegistry, db).simpleReload().join();
    }

    private void onAddReloadListener(@NotNull AddReloadListenerEvent event) {
        event.addListener(new StateReloadListener(eventHandler, store, typeRegistry, scopeRegistry, db));
    }

    private void onServerShutdown(ServerStoppedEvent event) {
        store.closeConnection(db);
    }
}
