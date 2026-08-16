package net.aros.canon.core;

import net.aros.canon.core.migration.StateMigratorRegistry;
import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.core.state.type.StateType;
import net.aros.canon.db.StatesDB;
import net.aros.canon.event.MutableStateEventHandler;
import net.aros.canon.event.StateEventHandler;
import net.aros.canon.impl.*;
import net.aros.canon.registry.MutableRegistry;
import net.aros.canon.registry.Registry;
import net.aros.canon.store.MutableStateStore;
import net.aros.canon.store.StateStore;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;

import static net.aros.canon.CanonLibMod.MOD_ID;

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

    private final StatesDB db = new StatesDBSqLiteImpl();
    private final MutableStateStore store = new StateStoreImpl();
    private final MutableRegistry<StateType<?>> typeRegistry = new RegistryImpl<>();
    private final MutableRegistry<ScopeType<?>> scopeRegistry = new RegistryImpl<>();
    private final MutableStateEventHandler eventHandler = new StateEventHandlerImpl();
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
        db.createConnection(event.getServer().getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath());
        db.initialize();

        new StateReloadListener(eventHandler, store, typeRegistry, scopeRegistry, db).simpleReload().join();
    }

    private void onAddReloadListener(@NotNull AddReloadListenerEvent event) {
        event.addListener(new StateReloadListener(eventHandler, store, typeRegistry, scopeRegistry, db));
    }

    private void onServerShutdown(ServerStoppedEvent event) {
        store.clear();
        db.closeConnection();
    }
}
