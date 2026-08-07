package net.aros.canon.examples;

import net.aros.canon.CanonLibMod;
import net.aros.canon.core.flag.FlagKey;
import net.aros.canon.core.flag.type.BuiltinFlagTypes;
import net.aros.canon.event.custom.FlagRegistrationEvent;
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

import static net.aros.canon.CanonLibMod.MOD_ID;

@ParametersAreNonnullByDefault
@EventBusSubscriber(modid = MOD_ID)
public class TestItem extends Item {
    public TestItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static boolean usingLegacy = true;

    public static final FlagKey<Boolean> PLAYER_DIED_LEGACY = new FlagKey<>(CanonLibMod.id("died"), BuiltinFlagTypes.BOOL);
    public static final FlagKey<Integer> PLAYER_DIED = new FlagKey<>(CanonLibMod.id("died"), BuiltinFlagTypes.INT);

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && usedHand == InteractionHand.MAIN_HAND) {
            if (player.isShiftKeyDown()) {
                usingLegacy = !usingLegacy;
            } else {
                player.sendSystemMessage(Component.literal(Can.get(deathsFlag()) + " смертей"));
            }
        }
        return InteractionResultHolder.success(stack);
    }

    public static FlagKey<?> deathsFlag() {
        return usingLegacy ? PLAYER_DIED_LEGACY : PLAYER_DIED;
    }

    @SubscribeEvent
    public static void onFlagRegistration(FlagRegistrationEvent e) {
        if (usingLegacy) {
            e.registerFlag(PLAYER_DIED_LEGACY)
                    .addEventListener(LivingDeathEvent.class, (key, flag, event) -> {
                        if (event.getEntity() instanceof ServerPlayer) Can.set(key, true);
                    })
                    .addEventListener(EntityJoinLevelEvent.class, (key, flag, event) -> {
                        if (event.getEntity() instanceof ServerPlayer player) {
                            player.sendSystemMessage(Component.literal(flag ? "Ты умирал хотя бы раз" : "Ты не умирал"));
                        }
                    })
                    .addChangeListener((before, current) -> {
                        Component msg = Component.literal("Теперь ты " + (current ? "" : "не ") + "умирал");
                        for (ServerPlayer player : Can.server().getPlayerList().getPlayers()) {
                            player.sendSystemMessage(msg);
                        }
                    });
        } else {
            e.registerFlag(PLAYER_DIED)
                    .addEventListener(LivingDeathEvent.class, (key, flag, event) -> {
                        if (event.getEntity() instanceof ServerPlayer) Can.set(key, flag + 1);
                    })
                    .addEventListener(EntityJoinLevelEvent.class, (key, flag, event) -> {
                        if (event.getEntity() instanceof ServerPlayer player) {
                            player.sendSystemMessage(Component.literal(flag == 0 ? "Ты не умирал" : "Ты умирал хотя бы раз"));
                        }
                    })
                    .addChangeListener((before, current) -> {
                        Component msg = Component.literal("Теперь у тебя ").append(String.valueOf(current)).append(" смертей");
                        for (ServerPlayer player : Can.server().getPlayerList().getPlayers()) {
                            player.sendSystemMessage(msg);
                        }
                    });
        }
    }
}
