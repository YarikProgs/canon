package net.aros.canon.impl;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.event.FlagHooks;
import net.aros.canon.impl.store.FlagStoreImpl;
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
    private final FlagTypeRegistryImpl typeRegistry;

    public FlagReloadListener(FlagEventHandlerImpl eventHandler, FlagStoreImpl store, FlagTypeRegistryImpl typeRegistry) {
        this.eventHandler = eventHandler;
        this.store = store;
        this.typeRegistry = typeRegistry;
    }

    private Set<FlagKey<?>> prepare() {
        eventHandler.clearChangeListeners();
        eventHandler.unsubscribeAllEvents();
        typeRegistry.clear();
        var event = FlagHooks.fireFlagRegistration();
        typeRegistry.putAll(event.registeredTypes());
        return event.registeredKeys();
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
                .thenCompose(store::reconcileKeysInDB)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(store::reconcileKeysInLive, gameExecutor);
    }

    public CompletableFuture<Void> simpleReload() {
        return store.reconcileKeysInDB(prepare())
                .thenAccept(store::reconcileKeysInLive);
    }
}
