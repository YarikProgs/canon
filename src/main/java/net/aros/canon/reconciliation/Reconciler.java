package net.aros.canon.reconciliation;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import net.aros.canon.core.Canon;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.scope.ScopeType;
import net.aros.canon.util.StateMap;
import net.aros.canon.util.ScopedStateKey;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.Set;

public final class Reconciler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final StateMap currentMap;
    private final Set<StateKey<?, ?>> newKeys;

    public Reconciler(Set<StateKey<?, ?>> newKeys, StateMap currentMap) {
        this.newKeys = newKeys;
        this.currentMap = currentMap;
    }

    public @NotNull ReconciliationResult reconcileKeys() {
        ReconciliationResult result = new ReconciliationResult(new StateMap(), new StateMap());
        Multimap<KeyIdentity, ScopedStateKey<?, ?>> byIdentity = ArrayListMultimap.create();

        for (ScopedStateKey<?, ?> oldScopedKey : currentMap.keySet()) {
            byIdentity.put(new KeyIdentity(oldScopedKey.key().identifier(), oldScopedKey.key().scopeType()), oldScopedKey);
        }

        for (StateKey<?, ?> newKey : newKeys) {
            for (ScopedStateKey<?, ?> oldScopedKey : byIdentity.get(new KeyIdentity(newKey.identifier(), newKey.scopeType()))) {
                //noinspection unchecked,rawtypes
                reconcileKey(result, oldScopedKey, (StateKey) newKey);
            }
        }

        return result;
    }

    private <S, T1, T2> void reconcileKey(
            ReconciliationResult result,
            @NotNull ScopedStateKey<S, T1> old, @NotNull StateKey<S, T2> newKey
    ) {
        T1 oldValue = currentMap.get(old.key(), old.scope()).orElseThrow();

        if (Objects.equals(old.key().type(), newKey.type())) {
            //noinspection unchecked
            result.newMap().put(newKey, old.scope(), (T2) oldValue);
            return;
        }

        T2 newValue = migrateOrDefault(old, oldValue, newKey);

        result.persist().put(newKey, old.scope(), newValue);
        result.newMap().put(newKey, old.scope(), newValue);
    }

    private <S, T1, T2> T2 migrateOrDefault(@NotNull ScopedStateKey<S, T1> old, T1 oldValue, StateKey<S, T2> newKey) {
        var opt = Canon.get().migratorRegistry().tryMigrate(
                old.key().type(),
                oldValue,
                newKey.type()
        );

        if (opt.isPresent()) {
            LOGGER.info("State {} (scope {} = {}) migrated: {} -> {}",
                    old.key().identifier(),
                    old.key().scopeType().identifier(),
                    old.scope(),
                    old.key().type().identifier(),
                    newKey.type().identifier()
            );
        } else {
            LOGGER.warn("State {} (scope {} = {}) changed its type ({} -> {}) but migrator wasn't found",
                    old.key().identifier(),
                    old.key().scopeType().identifier(),
                    old.scope(),
                    old.key().type().identifier(),
                    newKey.type().identifier()
            );
        }

        return opt.orElse(newKey.type().defaultValue());
    }

    private record KeyIdentity(ResourceLocation identifier, ScopeType<?> scopeType) {
    }
}
