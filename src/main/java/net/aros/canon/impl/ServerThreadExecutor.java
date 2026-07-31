package net.aros.canon.impl;

import net.aros.canon.core.executors.ThreadExecutor;
import net.minecraft.server.MinecraftServer;

import java.util.function.Supplier;

public class ServerThreadExecutor implements ThreadExecutor {
    private final MinecraftServer server;

    public ServerThreadExecutor(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void submit(Runnable task) {
        server.submitAsync(task);
    }

    @Override
    public <T> T submitAndWait(Supplier<T> task) {
        try {
            return server.submit(task).get();
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void shutdown() {
    }
}
