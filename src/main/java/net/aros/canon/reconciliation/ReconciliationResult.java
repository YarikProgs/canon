package net.aros.canon.reconciliation;

import net.aros.canon.util.StateMap;

public record ReconciliationResult(StateMap newMap, StateMap persist) {
}
