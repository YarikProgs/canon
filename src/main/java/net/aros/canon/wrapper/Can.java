package net.aros.canon.wrapper;

import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.core.tx.Transaction;
import net.aros.canon.impl.SandboxImpl;
import net.aros.canon.impl.TransactionImpl;
import net.aros.canon.migration.StateMigrator;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Can {
    public static void registerMigrator(StateMigrator migrator) {
        Canon.get().migratorRegistry().register(migrator);
    }

    @Contract("_ -> new")
    public static @NotNull Sandbox sandbox(String name) {
        return new SandboxImpl(Canon.get().stateStore(), name);
    }

    @Contract(" -> new")
    public static @NotNull Transaction transaction() {
        return new TransactionImpl(Canon.get().stateStore());
    }

    public static <S, T> T get(StateKey<S, T> stateKey, S scope) {
        return Canon.get().stateStore().get(stateKey, scope);
    }

    public static <S, T> void set(StateKey<S, T> stateKey, S scope, T state) {
        Canon.get().stateStore().set(stateKey, scope, state);
    }

    @NotNull
    public static MinecraftServer server() {
        return Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Server is not running");
    }
}
