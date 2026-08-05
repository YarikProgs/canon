package net.aros.canon.event;

public interface ChangeEvent<F> {
    void onChange(F before, F current);
}