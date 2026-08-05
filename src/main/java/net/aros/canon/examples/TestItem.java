package net.aros.canon.examples;

import com.mojang.serialization.Codec;
import net.aros.canon.core.flag.FlagKey;
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
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TestItem extends Item {
    public TestItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static final FlagKey<Integer> DEATH_COUNTER = Can
            .flag("deaths", Codec.INT, 0)
            .addEventListener(LivingDeathEvent.class, (key, flag, event) -> {
                if (event.getEntity() instanceof Player) Can.set(key, flag + 1);
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
            })
            .buildAndRegister();

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && usedHand == InteractionHand.MAIN_HAND) {
            player.sendSystemMessage(Component.literal(Can.get(DEATH_COUNTER) + " смертей"));
        }
        return InteractionResultHolder.success(stack);
    }
}
