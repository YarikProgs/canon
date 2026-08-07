package net.aros.canon.core.db;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.type.FlagType;
import net.aros.canon.util.GsonHelper;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("SqlDialectInspection")
public class FlagsDB {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String FILENAME = "flags.db";
    private static final String TABLE = "flags";

    public static final String SQL_INIT = """
            CREATE TABLE IF NOT EXISTS %s (
                key   TEXT PRIMARY KEY,
                type  TEXT NOT NULL,
                value TEXT NOT NULL
            )
            """.formatted(TABLE);

    private static final String SQL_UPSERT = """
            INSERT INTO %s(key, type, value)
            VALUES (?, ?, ?)
            ON CONFLICT(key) DO UPDATE SET
                type = excluded.type,
                value = excluded.value
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

    public void writeChanges(Map<FlagKey<?>, String> changes) {
        withConnection("writeChanges", conn -> {
            conn.setAutoCommit(false);

            try (PreparedStatement statement = conn.prepareStatement(SQL_UPSERT)) {
                for (var entry : changes.entrySet()) {
                    statement.setString(1, entry.getKey().identifier().toString());
                    statement.setString(2, entry.getKey().type().identifier().toString());
                    statement.setString(3, entry.getValue());
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        }, Connection::rollback, conn -> conn.setAutoCommit(true));
    }

    public Map<FlagKey<?>, Object> reconcileKeys(Set<FlagKey<?>> keys) {
        Map<FlagKey<?>, Object> result = new HashMap<>();
        if (keys.isEmpty()) return result;

        withConnection("loadAll", conn -> {
            conn.setAutoCommit(false);

            Map<ResourceLocation, Row> existing = new HashMap<>();
            readExisting(conn, keys, existing);

            List<FlagKey<?>> toInsert = new ArrayList<>();
            Map<FlagKey<?>, Object> toPersistMigrated = new HashMap<>();

            for (FlagKey<?> key : keys) {
                Row row = existing.get(key.identifier());

                if (row == null) {
                    toInsert.add(key);
                    result.put(key, key.type().defaultValue());
                    continue;
                }

                if (row.type().equals(key.type().identifier().toString())) {
                    result.put(key, key.parseOrDefault(JsonOps.INSTANCE, GsonHelper.parse(row.value())));
                    continue;
                }

                Optional<Object> migrated = tryMigrate(row, key);
                if (migrated.isPresent()) {
                    result.put(key, migrated.get());
                    toPersistMigrated.put(key, migrated.get());
                    LOGGER.info("Flag {} migrated: {} -> {}", key.identifier(), row.type(), key.type().identifier());
                } else {
                    LOGGER.warn("Flag {} changed its type ({} -> {}) but migrator wasn't found. Using default value, also not modifying db's row",
                            key.identifier(), row.type(), key.type().identifier());
                    result.put(key, key.type().defaultValue());
                }
            }

            insertMissing(conn, toInsert);
            persistMigrated(conn, toPersistMigrated);

        }, Connection::rollback, conn -> conn.setAutoCommit(true));

        return result;
    }

    private void insertMissing(Connection conn, List<FlagKey<?>> toInsert) throws SQLException {
        if (toInsert.isEmpty()) return;
        try (PreparedStatement st = conn.prepareStatement(SQL_UPSERT)) {
            for (FlagKey<?> key : toInsert) {
                String json = key.encodeDefault(JsonOps.INSTANCE).map(GsonHelper::toString)
                        .orElseThrow(() -> new IllegalStateException("Cannot encode default for " + key.identifier()));
                st.setString(1, key.identifier().toString());
                st.setString(2, key.type().identifier().toString());
                st.setString(3, json);
                st.addBatch();
            }
            st.executeBatch();
        }
    }

    @SuppressWarnings("unchecked")
    private void persistMigrated(Connection conn, Map<FlagKey<?>, Object> migrated) throws SQLException {
        if (migrated.isEmpty()) return;
        try (PreparedStatement st = conn.prepareStatement(SQL_UPSERT)) {
            for (var entry : migrated.entrySet()) {
                FlagKey<Object> key = (FlagKey<Object>) entry.getKey();
                String json = key.encode(JsonOps.INSTANCE, entry.getValue()).map(GsonHelper::toString)
                        .orElseThrow(() -> new IllegalStateException("Cannot encode migrated value for " + key.identifier()));
                st.setString(1, key.identifier().toString());
                st.setString(2, key.type().identifier().toString());
                st.setString(3, json);
                st.addBatch();
            }
            st.executeBatch();
        }
    }

    private Optional<Object> tryMigrate(Row row, FlagKey<?> key) {
        ResourceLocation oldTypeId = ResourceLocation.parse(row.type());
        Optional<FlagType<?>> oldType = Canon.get().flagTypeRegistry().get(oldTypeId);
        return tryMigrateFrom(oldTypeId, oldType.orElse(null), key, row.value());
    }

    private <T> Optional<Object> tryMigrateFrom(ResourceLocation oldTypeId, FlagType<T> oldType, FlagKey<?> key, String valueJson) {
        if (oldType == null) {
            LOGGER.warn("Flag {}: type '{}' is no longer registered; there is no way to decode the old value",
                    key.identifier(), oldTypeId);
            return Optional.empty();
        }

        var decoded = oldType.codec().parse(JsonOps.INSTANCE, GsonHelper.parse(valueJson));

        if (decoded.isError()) {
            LOGGER.warn("Flag {}: unable to parse the value using the old '{}' type: {}",
                    key.identifier(), oldTypeId, decoded.error().orElseThrow().message());
            return Optional.empty();
        }

        return Canon.get().migratorRegistry().tryMigrate(oldType, decoded.getOrThrow(), key.type()).map(Function.identity());
    }

    private void readExisting(Connection conn, Set<FlagKey<?>> keys, Map<ResourceLocation, Row> out) throws SQLException {
        String placeholders = keys.stream().map(k -> "?").collect(Collectors.joining(","));
        String sql = "SELECT key, type, value FROM %s WHERE key IN (%s)".formatted(TABLE, placeholders);

        try (PreparedStatement st = conn.prepareStatement(sql)) {
            int i = 1;
            for (FlagKey<?> key : keys) st.setString(i++, key.identifier().toString());

            try (var rs = st.executeQuery()) {
                while (rs.next()) {
                    out.put(ResourceLocation.parse(rs.getString("key")),
                            new Row(rs.getString("type"), rs.getString("value")));
                }
            }
        }
    }

//    public AlteredDiff applyAndAlterDiff(Diff diff) {
//        AlteredDiff alteredDiff = new AlteredDiff();
//
//        withConnection("applyAndAlterDiff", conn -> {
//            conn.setAutoCommit(false);
//
//            if (!diff.conflicts().isEmpty()) {
//                try (PreparedStatement statement = conn.prepareStatement(SQL_DELETE)) {
//                    for (var key : diff.conflicts()) {
//                        statement.setString(1, key.toString());
//                        statement.addBatch();
//                    }
//                    statement.executeBatch();
//                }
//            }
//
//            if (!diff.newKeys().isEmpty()) {
//                addMissingOnly(conn, diff, loaded);
//            }
//        }, Connection::rollback, conn -> conn.setAutoCommit(true));
//
//        return loaded;
//    }
//
//    private void addMissingOnly(Connection conn, Diff diff, Map<FlagKey<?>, String> loaded) throws SQLException {
//        Map<FlagKey<?>, String> missing = new HashMap<>(diff.newKeys());
//
//        try (PreparedStatement statement = conn.prepareStatement(SQL_SELECT_BY_KEY)) {
//            for (FlagKey<?> key : diff.newKeys().keySet()) {
//                statement.setString(1, key.identifier().toString());
//
//                try (var rs = statement.executeQuery()) {
//                    if (rs.next()) {
//                        loaded.put(key, rs.getString("value"));
//                        missing.remove(key);
//                    }
//                }
//            }
//        }
//
//        if (!missing.isEmpty()) {
//            try (PreparedStatement st = conn.prepareStatement(SQL_UPSERT)) {
//                for (var entry : missing.entrySet()) {
//                    st.setString(1, entry.getKey().identifier().toString());
//                    st.setString(2, entry.getValue());
//                    st.addBatch();
//                }
//
//                st.executeBatch();
//            }
//        }
//    }

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

    private record Row(String type, String value) {
    }
}
