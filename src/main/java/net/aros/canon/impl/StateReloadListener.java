package net.aros.canon.impl;

import net.aros.canon.core.db.StatesDB;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.core.state.type.StateType;
import net.aros.canon.event.StateHooks;
import net.aros.canon.reconciliation.Reconciler;
import net.aros.canon.registry.MutableRegistry;
import net.aros.canon.util.StateMap;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ParametersAreNonnullByDefault
public class StateReloadListener implements PreparableReloadListener {
    private final StateEventHandlerImpl eventHandler;
    private final StateStoreImpl store;
    private final MutableRegistry<StateType<?>> typeRegistry;
    private final MutableRegistry<ScopeType<?>> scopeRegistry;
    private final StatesDB db;

    public StateReloadListener(StateEventHandlerImpl eventHandler, StateStoreImpl store, MutableRegistry<StateType<?>> typeRegistry, MutableRegistry<ScopeType<?>> scopeRegistry, StatesDB db) {
        this.eventHandler = eventHandler;
        this.store = store;
        this.typeRegistry = typeRegistry;
        this.scopeRegistry = scopeRegistry;
        this.db = db;
    }

    private Set<StateKey<?, ?>> prepare() {
        eventHandler.clearChangeListeners();
        eventHandler.unsubscribeAllEvents();
        var event = StateHooks.fireStateRegistration();;
        typeRegistry.replaceWith(event.registeredTypes());
        scopeRegistry.replaceWith(event.registeredScopeTypes());
        return event.registeredKeys();
    }

    private CompletableFuture<StateMap> selectReconcileAndPersist(Set<StateKey<?, ?>> newKeys) {
        return CompletableFuture.supplyAsync(() -> {
            StateMap currentMap = db.selectAll();
            var result = new Reconciler(newKeys, currentMap).reconcileKeys();
            db.persist(result.persist());
            return result.newMap();
        }, db.executor());
    }

    @Override
    @NotNull
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor
    ) {
        if (ServerLifecycleHooks.getCurrentServer() == null)
            return CompletableFuture.<Void>completedFuture(null).thenCompose(barrier::wait);

        return CompletableFuture
                .supplyAsync(this::prepare, gameExecutor)
                .thenCompose(this::selectReconcileAndPersist)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(store::replaceWith, gameExecutor);
    }

    public CompletableFuture<Void> simpleReload() {
        return selectReconcileAndPersist(prepare()).thenAccept(store::replaceWith);
    }
}
