package net.aros.canon.core.db;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.impl.store.Diff;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("SqlDialectInspection")
public class FlagsDB {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILENAME = "flags.db";
    private static final String TABLE = "flags";

    public static final String SQL_INIT = """
            CREATE TABLE IF NOT EXISTS %s (
                key   TEXT PRIMARY KEY,
                value TEXT NOT NULL
            )
            """.formatted(TABLE);

    private static final String SQL_UPSERT = """
            INSERT INTO %s(key, value)
            VALUES (?, ?)
            ON CONFLICT(key) DO UPDATE SET
                value = excluded.value
            """.formatted(TABLE);

    private static final String SQL_DELETE = """
            DELETE FROM %s
            WHERE key = ?
            """.formatted(TABLE);

    private static final String SQL_SELECT_BY_KEY = """
            SELECT key, value
            FROM %s
            WHERE key = ?
            """.formatted(TABLE);

    private Connection connection;

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

    public void initialize() {
        withConnection("initialize", conn -> {
            try (var st = conn.createStatement()) {
                st.execute(SQL_INIT);
            }
        });
    }

    public void closeConnection() {
        withConnection("closeConnection", Connection::close);
    }

    public void writeChanges(Map<ResourceLocation, String> changes) {
        withConnection("writeChanges", conn -> {
            conn.setAutoCommit(false);

            try (PreparedStatement statement = conn.prepareStatement(SQL_UPSERT)) {
                for (var entry : changes.entrySet()) {
                    statement.setString(1, entry.getKey().toString());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }, Connection::rollback, conn -> conn.setAutoCommit(true));
    }

    public Map<FlagKey<?>, String> writeDiffAndGetNewFlags(Diff diff) {
        Map<FlagKey<?>, String> loaded = new HashMap<>();

        withConnection("writeDiff", conn -> {
            conn.setAutoCommit(false);

            if (!diff.conflicts().isEmpty()) {
                try (PreparedStatement statement = conn.prepareStatement(SQL_DELETE)) {
                    for (var key : diff.conflicts()) {
                        statement.setString(1, key.toString());
                        statement.addBatch();
                    }
                    statement.executeBatch();
                }
            }

            if (!diff.added().isEmpty()) {
                addMissingOnly(conn, diff, loaded);
            }
        }, Connection::rollback, conn -> conn.setAutoCommit(true));

        return loaded;
    }

    private void addMissingOnly(Connection conn, Diff diff, Map<FlagKey<?>, String> loaded) throws SQLException {
        Map<FlagKey<?>, String> missing = new HashMap<>(diff.added());

        try (PreparedStatement statement = conn.prepareStatement(SQL_SELECT_BY_KEY)) {
            for (FlagKey<?> key : diff.added().keySet()) {
                statement.setString(1, key.identifier().toString());

                try (var rs = statement.executeQuery()) {
                    if (rs.next()) {
                        loaded.put(key, rs.getString("value"));
                        missing.remove(key);
                    }
                }
            }
        }

        if (!missing.isEmpty()) {
            try (PreparedStatement st = conn.prepareStatement(SQL_UPSERT)) {
                for (var entry : missing.entrySet()) {
                    st.setString(1, entry.getKey().identifier().toString());
                    st.setString(2, entry.getValue());
                    st.addBatch();
                }

                st.executeBatch();
            }
        }
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

    @FunctionalInterface
    private interface ConnectionAction {
        void execute(Connection connection) throws SQLException;
    }
}
