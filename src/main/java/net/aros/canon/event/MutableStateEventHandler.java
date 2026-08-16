package net.aros.canon.event;

public interface MutableStateEventHandler extends StateEventHandler {
    void clearChangeListeners();

    void unsubscribeAllEvents();
}
