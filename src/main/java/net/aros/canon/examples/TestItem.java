package net.aros.canon.examples;

import com.mojang.serialization.Codec;
import net.aros.canon.core.Canon;
import net.aros.canon.core.flag.FlagKey;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class TestItem extends Item {
    public TestItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.EPIC));
    }

    public static final FlagKey<Integer> SPEECH = Canon.get().flags().register(new FlagKey<>("speech", Codec.INT, 0));

    @Override
    @NotNull
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        var stack = player.getItemInHand(usedHand);
        if (!level.isClientSide && usedHand == InteractionHand.MAIN_HAND) {
            Canon.get().scriptExecutor().submit(() -> {
                try {
                    doDialogue(player);
                } catch (InterruptedException ignore) {
                }
            });
        }
        return InteractionResultHolder.success(stack);
    }

    private void doDialogue(Player player) throws InterruptedException {
        var speech = Canon.get().flags().flag(SPEECH);
        switch (speech.get()) {
            case 0 -> {
                Canon.get().mainThreadExecutor().submit(() -> speech.set(-1));

                player.sendSystemMessage(Component.literal("Дай мне досчитать до трёх!"));
                Thread.sleep(1000);
                player.sendSystemMessage(Component.literal("1"));
                Thread.sleep(1000);
                player.sendSystemMessage(Component.literal("2"));
                Thread.sleep(1000);
                player.sendSystemMessage(Component.literal("3"));
                Thread.sleep(1000);
                player.sendSystemMessage(Component.literal("OK!"));

                Canon.get().mainThreadExecutor().submit(() -> speech.set(1));
            }
            case 1 -> {
                player.sendSystemMessage(Component.literal("Всё!!"));
            }
            case -1 -> {
                player.sendSystemMessage(Component.literal("Ты не дал мне досчитать..."));
            }
        }
    }
}
