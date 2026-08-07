package net.aros.canon.event.custom;

import net.neoforged.bus.api.Event;

/**
 * The event is fired when server resources are reloaded, so flags too
 * At this point, all uncommitted transactions and sandboxes must cease to be used (be recreated)
 */
public class FlagResetEvent extends Event {
}
