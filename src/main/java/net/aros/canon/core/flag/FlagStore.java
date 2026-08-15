package net.aros.canon.core.flag;

import net.aros.canon.core.tx.Sandbox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface FlagStore {
    <S, T> void set(FlagKey<S, T> key, S scope, T value);

    <S, T> void setLive(FlagKey<S, T> key, S scope, T value);

    <S, T> T get(FlagKey<S, T> key, S scope);

    <S> void setOwner(FlagKey<S, ?> key, S scope, @Nullable Sandbox sandbox);

    @Nullable
    <S> Sandbox getOwner(FlagKey<S, ?> key, S scope);
}
