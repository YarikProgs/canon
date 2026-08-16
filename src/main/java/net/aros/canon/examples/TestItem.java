package net.aros.canon.examples;

import net.aros.canon.CanonLibMod;
import net.aros.canon.core.state.StateKey;
import net.aros.canon.core.state.scope.BuiltinScopeTypes;
import net.aros.canon.core.state.type.BuiltinStateTypes;
import net.aros.canon.event.custom.StateRegistrationEvent;
import net.aros.canon.wrapper.Can;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.UUID;

import static net.aros.canon.CanonLibMod.MOD_ID;

@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = MOD_ID)
public class TestItem extends Item {
    public TestItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static boolean usingLegacy = true;

    public static final StateKey<UUID, Boolean> PLAYER_DIED_LEGACY = new StateKey<>(BuiltinScopeTypes.ENTITY, CanonLibMod.id("died"), BuiltinStateTypes.BOOL);
    public static final StateKey<UUID, Integer> PLAYER_DIED = new StateKey<>(BuiltinScopeTypes.ENTITY, CanonLibMod.id("died"), BuiltinStateTypes.INT);

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && usedHand == InteractionHand.MAIN_HAND) {
            if (player.isShiftKeyDown()) {
                usingLegacy = !usingLegacy;
            } else {
                player.sendSystemMessage(Component.literal(Can.get(playerDiedState(), player.getUUID()) + " смертей"));
            }
        }
        return InteractionResultHolder.success(stack);
    }

    public static StateKey<UUID, ?> playerDiedState() {
        return usingLegacy ? PLAYER_DIED_LEGACY : PLAYER_DIED;
    }

    @SubscribeEvent
    public static void onStateRegistration(StateRegistrationEvent e) {
        if (usingLegacy) {
            e.registerStateKey(PLAYER_DIED_LEGACY)
                    .addEventListener(LivingDeathEvent.class, (key, event) -> {
                        if (event.getEntity() instanceof ServerPlayer) Can.set(key, event.getEntity().getUUID(), true);
                    })
                    .addEventListener(EntityJoinLevelEvent.class, (key, event) -> {
                        if (event.getEntity() instanceof ServerPlayer player) {
                            player.sendSystemMessage(Component.literal(Can.get(key, event.getEntity().getUUID()) ? "Ты умирал хотя бы раз" : "Ты не умирал"));
                        }
                    })
                    .addChangeListener((scope, before, current) -> {
                        Component msg = Component.literal("Теперь ты " + (current ? "" : "не ") + "умирал");
                        Can.server().getPlayerList().getPlayer(scope).sendSystemMessage(msg);
                    });
        } else {
            e.registerStateKey(PLAYER_DIED)
                    .addEventListener(LivingDeathEvent.class, (key, event) -> {
                        if (event.getEntity() instanceof ServerPlayer) Can.set(key, event.getEntity().getUUID(), Can.get(key, event.getEntity().getUUID()) + 1);
                    })
                    .addEventListener(EntityJoinLevelEvent.class, (key, event) -> {
                        if (event.getEntity() instanceof ServerPlayer player) {
                            player.sendSystemMessage(Component.literal(Can.get(key, player.getUUID()) == 0 ? "Ты не умирал" : "Ты умирал хотя бы раз"));
                        }
                    })
                    .addChangeListener((scope, before, current) -> {
                        Component msg = Component.literal("Теперь у тебя ").append(String.valueOf(current)).append(" смертей");
                        Can.server().getPlayerList().getPlayer(scope).sendSystemMessage(msg);
                    });
        }
    }
}
