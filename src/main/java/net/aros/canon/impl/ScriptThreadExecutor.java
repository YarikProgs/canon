package net.aros.canon.impl;

import net.aros.canon.core.executors.ThreadExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

public class ScriptThreadExecutor implements ThreadExecutor {
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Override
    public void submit(Runnable script) {
        executor.submit(script);
    }

    @Override
    public <T> T submitAndWait(Supplier<T> task) {
        var future = executor.submit(task::get);
        try {
            return future.get();
        } catch (Exception e) {
            throw new IllegalStateException();
        }
    }

    @Override
    public void shutdown() {
        executor.shutdownNow();
    }
}
