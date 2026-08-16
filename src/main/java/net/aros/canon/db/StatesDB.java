package net.aros.canon.db;

import net.aros.canon.util.StateMap;

import java.nio.file.Path;
import java.util.concurrent.Executor;

public interface StatesDB {
    void createConnection(Path dbPath);

    void initialize();

    void closeConnection();

    StateMap selectAll();

    void persist(StateMap map);

    Executor executor();
}
