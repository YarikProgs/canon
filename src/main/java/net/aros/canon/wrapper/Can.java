package net.aros.canon.wrapper;

import com.mojang.serialization.Codec;
import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.impl.SandboxImpl;
import net.aros.canon.impl.TransactionImpl;
import net.aros.canon.tx.Sandbox;
import net.aros.canon.tx.Transaction;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Can {
    @Contract("_, _, _, _ -> new")
    public static <T> @NotNull FlagKey<T> flag(String key, Class<T> type, Codec<T> codec, T defaultValue) {
        return new FlagKey<>(key, codec, type, defaultValue);
    }

    @Contract("_ -> new")
    public static @NotNull Sandbox sandbox(String name) {
        return new SandboxImpl(Canon.get().flagStore(), name);
    }

    @Contract(" -> new")
    public static @NotNull Transaction transaction() {
        return new TransactionImpl(Canon.get().flagStore());
    }

    public static <T> T get(FlagKey<T> flagKey) {
        return Canon.get().flagStore().get(flagKey);
    }

    public static <T> void set(FlagKey<T> flagKey, T flag) {
        Canon.get().flagStore().set(flagKey, flag);
    }

    @NotNull
    public static MinecraftServer server() {
        return Objects.requireNonNull(ServerLifecycleHooks.getCurrentServer(), "Server is not running");
    }
}
