package net.aros.canon.core.state;

import net.aros.canon.core.tx.Sandbox;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public interface StateStore {
    <S, T> void set(StateKey<S, T> key, S scope, T value);

    <S, T> void setLive(StateKey<S, T> key, S scope, T value);

    <S, T> T get(StateKey<S, T> key, S scope);

    <S> void setOwner(StateKey<S, ?> key, S scope, @Nullable Sandbox sandbox);

    @Nullable
    <S> Sandbox getOwner(StateKey<S, ?> key, S scope);
}
