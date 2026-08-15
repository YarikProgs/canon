package net.aros.canon.reconciliation;

import net.aros.canon.util.FlagMap;

public record ReconciliationResult(FlagMap newMap, FlagMap persist) {
}
