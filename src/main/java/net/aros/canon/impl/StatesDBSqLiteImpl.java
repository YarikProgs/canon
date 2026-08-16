package net.aros.canon.impl;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.core.state.type.StateType;
import net.aros.canon.db.StatesDB;
import net.aros.canon.util.GsonHelper;
import net.aros.canon.util.ScopedStateKey;
import net.aros.canon.util.StateMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings("SqlDialectInspection")
public class StatesDBSqLiteImpl implements StatesDB {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILENAME = "states.db";
    private static final String TABLE = "states";

    public static final String SQL_INIT = """
            CREATE TABLE IF NOT EXISTS %s (
                scope_type TEXT NOT NULL,
                scope      TEXT NOT NULL,
                key        TEXT NOT NULL,
                type       TEXT NOT NULL,
                value      TEXT NOT NULL,
                PRIMARY KEY (scope_type, scope, key)
            )
            """.formatted(TABLE);

    private static final String SQL_UPSERT = """
            INSERT INTO %s(scope_type, scope, key, type, value)
            VALUES (?, ?, ?, ?, ?)
            ON CONFLICT(scope_type, scope, key) DO UPDATE SET
                type = excluded.type,
                value = excluded.value
            """.formatted(TABLE);

    private static final String SQL_SELECT_ALL = "SELECT * FROM %s".formatted(TABLE);

    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "states-db"));
    private Connection connection;

    @Override
    public void createConnection(Path dbPath) {
        try {
            Files.createDirectories(dbPath);
        } catch (IOException e) {
            LOGGER.error("Failed to mkdirs for db");
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath.resolve(FILENAME));
        } catch (SQLException e) {
            LOGGER.error("Failed to create connection", e);
        }
    }

    @Override
    public void initialize() {
        withConnection("initialize", conn -> {
            try (var st = conn.createStatement()) {
                st.execute(SQL_INIT);
            }
        });
    }

    @Override
    public void closeConnection() {
        withConnection("closeConnection", Connection::close);
    }

    @Override
    public StateMap selectAll() {
        StateMap map = new StateMap();

        withConnection("selectAll", conn -> {
            try (Statement st = conn.createStatement()) {
                try (var rs = st.executeQuery(SQL_SELECT_ALL)) {
                    while (rs.next()) {
                        String scopeType = rs.getString(1);
                        String scope = rs.getString(2);
                        String key = rs.getString(3);
                        String type = rs.getString(4);
                        String value = rs.getString(5);

                        parseIntoMap(
                                map,
                                ResourceLocation.parse(scopeType),
                                scope,
                                ResourceLocation.parse(key),
                                ResourceLocation.parse(type),
                                value
                        );
                    }
                }
            }
        });

        return map;
    }

    @Override
    @SuppressWarnings({"rawtypes","unchecked"})
    public void persist(StateMap map) {
        withConnection("persist", conn -> {
            conn.setAutoCommit(false);

            try (PreparedStatement statement = conn.prepareStatement(SQL_UPSERT)) {
                for (var entry : map.entrySet()) {
                    if (encodeIntoStatement(statement, (ScopedStateKey) entry.getKey(), entry.getValue())) {
                        statement.addBatch();
                    }
                }
                statement.executeBatch();
            }
        }, Connection::rollback, conn -> conn.setAutoCommit(true));
    }

    private <S, T> boolean encodeIntoStatement(PreparedStatement statement, ScopedStateKey<S, T> key, T value) throws SQLException {
        String encodedScopeType = key.key().scopeType().identifier().toString();
        String encodedKey = key.key().identifier().toString();
        String encodedType = key.key().type().identifier().toString();

        DataResult<String> encodedScope = key.key().scopeType().scopeCodec().encodeStart(JsonOps.INSTANCE, key.scope())
                .map(GsonHelper::toString);
        if (encodedScope.isError()) {
            LOGGER.warn("Failed to encode state '{}': Failed to encode scope '{}': {}",
                    encodedKey, encodedScopeType, encodedScope.error().orElseThrow());
            return false;
        }

        DataResult<String> encodedValue = key.key().type().codec().encodeStart(JsonOps.INSTANCE, value)
                .map(GsonHelper::toString);
        if (encodedValue.isError()) {
            LOGGER.warn("Failed to encode state '{}': Failed to encode value '{}': {}",
                    encodedKey, encodedType, encodedValue.error().orElseThrow());
            return false;
        }
        statement.setString(1, encodedScopeType);
        statement.setString(2, encodedScope.getOrThrow());
        statement.setString(3, encodedKey);
        statement.setString(4, encodedType);
        statement.setString(5, encodedValue.getOrThrow());
        return true;
    }

    private void parseIntoMap(StateMap map, ResourceLocation scopeType, String scope, ResourceLocation key, ResourceLocation type, String value) {
        ScopeType<?> parsedScopeType = Canon.get().scopeTypeRegistry().get(scopeType);
        if (parsedScopeType == null) {
            LOGGER.warn("Failed to parse state '{}': Unknown scope type '{}'", key, scopeType);
            return;
        }
        StateType<?> parsedType = Canon.get().stateTypeRegistry().get(type);
        if (parsedType == null) {
            LOGGER.warn("Failed to parse state '{}': Unknown state type '{}'", key, type);
            return;
        }
        parseIntoMap(map, parsedScopeType, scope, key, parsedType, value);
    }

    private <S, T> void parseIntoMap(StateMap map, ScopeType<S> parsedScopeType, String scope, ResourceLocation key, StateType<T> parsedType, String value) {
        DataResult<S> parsedScope = parsedScopeType.scopeCodec().parse(JsonOps.INSTANCE, GsonHelper.parse(scope));
        if (parsedScope.isError()) {
            LOGGER.warn("Failed to parse state '{}', invalid scope '{}': {}", key, parsedScopeType.identifier(), parsedScope.error().orElseThrow());
            return;
        }
        DataResult<T> parsedValue = parsedType.codec().parse(JsonOps.INSTANCE, GsonHelper.parse(value));
        if (parsedValue.isError()) {
            LOGGER.warn("Failed to parse state '{}', invalid value: {}", key, parsedValue.error().orElseThrow());
            return;
        }
        StateKey<S, T> stateKey = new StateKey<>(parsedScopeType, key, parsedType);
        map.put(stateKey, parsedScope.getOrThrow(), parsedValue.getOrThrow());
    }

    private void withConnection(
            String actionName,
            ConnectionAction action,
            @Nullable ConnectionAction onCatch,
            @Nullable ConnectionAction onFinally
    ) {
        if (connection == null) {
            LOGGER.error("Failed to '{}': DB is not connected", actionName);
            return;
        }
        try {
            action.execute(connection);
        } catch (SQLException e) {
            LOGGER.error("Failed to '{}'", actionName, e);
            if (onCatch != null) withConnection(actionName + "@onCatch", onCatch);
        } finally {
            if (onFinally != null) withConnection(actionName + "@onFinally", onFinally);
        }
    }

    private void withConnection(String actionName, ConnectionAction action) {
        withConnection(actionName, action, null, null);
    }

    public Executor executor() {
        return dbExecutor;
    }

    @FunctionalInterface
    private interface ConnectionAction {
        void execute(Connection connection) throws SQLException;
    }
}
