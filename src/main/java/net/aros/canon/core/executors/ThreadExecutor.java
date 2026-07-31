package net.aros.canon.core.executors;

import java.util.function.Supplier;

public interface ThreadExecutor {
    void submit(Runnable task);

    <T> T submitAndWait(Supplier<T> task);

    void shutdown();
}
