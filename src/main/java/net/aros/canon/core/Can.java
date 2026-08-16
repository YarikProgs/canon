package net.aros.canon.core;

import net.aros.canon.core.migration.StateMigrator;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.core.tx.Transaction;
import net.aros.canon.impl.SandboxImpl;
import net.aros.canon.impl.TransactionImpl;
import net.aros.canon.store.MutableStateStore;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public class Can {
    public static void registerMigrator(StateMigrator migrator) {
        Canon.get().migratorRegistry().register(migrator);
    }

    @Contract("_ -> new")
    public static @NotNull Sandbox sandbox(String name) {
        return new SandboxImpl((MutableStateStore) Canon.get().stateStore(), name);
    }

    @Contract(" -> new")
    public static @NotNull Transaction transaction() {
        return new TransactionImpl((MutableStateStore) Canon.get().stateStore());
    }

    public static <S, T> T get(StateKey<S, T> stateKey, S scope) {
        return Canon.get().stateStore().get(stateKey, scope);
    }

    public static <T> T getGlobal(StateKey<Unit, T> stateKey) {
        return get(stateKey, Unit.INSTANCE);
    }

    public static <T> T getForEntity(StateKey<UUID, T> stateKey, UUID uuid) {
        return get(stateKey, uuid);
    }

    public static <T> T getForDimension(StateKey<ResourceKey<Level>, T> stateKey, ResourceKey<Level> dimensionKey) {
        return get(stateKey, dimensionKey);
    }

    public static <S, T> void set(StateKey<S, T> stateKey, S scope, T state) {
        Canon.get().stateStore().set(stateKey, scope, state);
    }

    public static <T> void setGlobal(StateKey<Unit, T> stateKey, T state) {
        set(stateKey, Unit.INSTANCE, state);
    }

    public static <T> void setForEntity(StateKey<UUID, T> stateKey, UUID uuid, T state) {
        set(stateKey, uuid, state);
    }

    public static <T> void setForDimension(StateKey<ResourceKey<Level>, T> stateKey, ResourceKey<Level> dimensionKey, T state) {
        set(stateKey, dimensionKey, state);
    }

    @NotNull
    public static MinecraftServer server() {
        return Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Server is not running");
    }
}
