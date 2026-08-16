package net.aros.canon.store;

import net.aros.canon.util.StateMap;

import java.util.concurrent.CompletableFuture;

public interface MutableStateStore extends StateStore {
    void clear();

    void replaceLiveWith(StateMap map);

    CompletableFuture<Void> commit(StateMap changes, boolean notifyListeners);
}
