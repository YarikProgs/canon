package net.aros.canon.impl;

import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.FlagStore;
import net.aros.canon.event.FlagHooks;
import net.minecraft.Util;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@ParametersAreNonnullByDefault
public class FlagReloadListener implements PreparableReloadListener {
    private final FlagRegistryImpl registry;
    private final FlagEventHandlerImpl eventHandler;
    private final FlagStore store;

    public FlagReloadListener(FlagRegistryImpl registry, FlagEventHandlerImpl eventHandler, FlagStore store) {
        this.registry = registry;
        this.eventHandler = eventHandler;
        this.store = store;
    }

    @Override
    @NotNull
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier, ResourceManager resourceManager, ProfilerFiller preparationsProfiler,
            ProfilerFiller reloadProfiler, Executor backgroundExecutor, Executor gameExecutor
    ) {
        if (ServerLifecycleHooks.getCurrentServer() == null)
            return CompletableFuture.runAsync(() -> {}).thenCompose(barrier::wait);

        return CompletableFuture
                .supplyAsync(() -> {
                    eventHandler.clearChangeListeners();
                    eventHandler.unsubscribeAllEvents();
                    List<FlagKey<?>> newKeys = FlagHooks.registerAllKeys();
                    registry.replaceWith(newKeys);
                    return registry;
                }, gameExecutor)
                .thenApplyAsync(store::diff, backgroundExecutor)
                .thenCompose(diff -> store.persistDiff(diff).thenApply(values -> {
                    diff.added().putAll(values);
                    return diff;
                }))
                .thenCompose(barrier::wait)
                .thenAcceptAsync(store::applyDiff, gameExecutor);
    }

    public CompletableFuture<Void> simpleReload() {
        eventHandler.clearChangeListeners();
        eventHandler.unsubscribeAllEvents();
        List<FlagKey<?>> newKeys = FlagHooks.registerAllKeys();
        registry.replaceWith(newKeys);
        return CompletableFuture
                .supplyAsync(() -> store.diff(registry), Util.backgroundExecutor())
                .thenCompose(diff -> store.persistDiff(diff).thenApply(values -> {
                    diff.added().putAll(values);
                    return diff;
                }))
                .thenAccept(store::applyDiff);
    }
}
