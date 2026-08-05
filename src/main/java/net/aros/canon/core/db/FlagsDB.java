package net.aros.canon.core.db;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.jetbrains.annotations.NotNull;
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

import static net.aros.canon.CanonLibMod.MOD_ID;

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
    private static final String SQL_SELECT = "SELECT key, value FROM flags";

    private Connection connection;

    public void createConnection(@NotNull MinecraftServer server) {
        Path path = server.getWorldPath(LevelResource.ROOT).resolve(MOD_ID).toAbsolutePath();
        try {
            Files.createDirectories(path);
        } catch (IOException e) {
            LOGGER.error("Failed to mkdirs for db");
        }

        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + path.resolve(FILENAME));
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

    public Map<String, String> readAllFlags() {
        Map<String, String> flags = new HashMap<>();

        withConnection("readAllFlags", conn -> {
            try (var st = conn.createStatement()) {
                var rs = st.executeQuery(SQL_SELECT);
                while (rs.next()) flags.put(rs.getString("key"), rs.getString("value"));
            }
        });

        return flags;
    }

    public void writeChanges(Map<String, String> changes) {
        withConnection("writeChanges", conn -> {
            conn.setAutoCommit(false);

            try (PreparedStatement statement = conn.prepareStatement(SQL_UPSERT)) {
                for (var entry : changes.entrySet()) {
                    statement.setString(1, entry.getKey());
                    statement.setString(2, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }, Connection::rollback, conn -> conn.setAutoCommit(true));
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
