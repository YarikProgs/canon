package net.aros.canon.wrapper;

import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.impl.SandboxImpl;
import net.aros.canon.impl.TransactionImpl;
import net.aros.canon.migration.FlagMigrator;
import net.aros.canon.core.tx.Sandbox;
import net.aros.canon.core.tx.Transaction;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Can {
    public static void registerMigrator(FlagMigrator migrator) {
        Canon.get().migratorRegistry().register(migrator);
    }

    @Contract("_ -> new")
    public static @NotNull Sandbox sandbox(String name) {
        return new SandboxImpl(Canon.get().flagStore(), name);
    }

    @Contract(" -> new")
    public static @NotNull Transaction transaction() {
        return new TransactionImpl(Canon.get().flagStore());
    }

    public static <S, T> T get(FlagKey<S, T> flagKey, S scope) {
        return Canon.get().flagStore().get(flagKey, scope);
    }

    public static <S, T> void set(FlagKey<S, T> flagKey, S scope, T flag) {
        Canon.get().flagStore().set(flagKey, scope, flag);
    }

    @NotNull
    public static MinecraftServer server() {
        return Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Server is not running");
    }
}
