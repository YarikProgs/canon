package net.aros.canon.impl;

import net.aros.canon.core.db.FlagsDB;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.scope.ScopeType;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.impl.store.FlagStoreImpl;
import net.aros.canon.reconciliation.Reconciler;
import net.aros.canon.registry.MutableRegistry;
import net.aros.canon.util.FlagMap;
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
public class FlagReloadListener implements PreparableReloadListener {
    private final FlagEventHandlerImpl eventHandler;
    private final FlagStoreImpl store;
    private final MutableRegistry<FlagType<?>> typeRegistry;
    private final MutableRegistry<ScopeType<?>> scopeRegistry;
    private final FlagsDB db;

    public FlagReloadListener(FlagEventHandlerImpl eventHandler, FlagStoreImpl store, MutableRegistry<FlagType<?>> typeRegistry, MutableRegistry<ScopeType<?>> scopeRegistry, FlagsDB db) {
        this.eventHandler = eventHandler;
        this.store = store;
        this.typeRegistry = typeRegistry;
        this.scopeRegistry = scopeRegistry;
        this.db = db;
    }

    private Set<FlagKey<?, ?>> prepare() {
        eventHandler.clearChangeListeners();
        eventHandler.unsubscribeAllEvents();
        var event = FlagHooks.fireFlagRegistration();;
        typeRegistry.replaceWith(event.registeredTypes());
        scopeRegistry.replaceWith(event.registeredScopeTypes());
        return event.registeredKeys();
    }

    private CompletableFuture<FlagMap> selectReconcileAndPersist(Set<FlagKey<?, ?>> newKeys) {
        return CompletableFuture.supplyAsync(() -> {
            FlagMap currentMap = db.selectAll();
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
