package net.aros.canon.event;

public interface ChangeEvent<S, T> {
    void onChange(S scope, T before, T current);
}