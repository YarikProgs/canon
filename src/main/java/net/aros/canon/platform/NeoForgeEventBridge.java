package net.aros.canon.platform;

import net.aros.canon.core.Canon;
import net.aros.canon.examples.ExampleEvents;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class NeoForgeEventBridge {
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Canon.get().events().fire(ExampleEvents.LIVING_DEATH, event);
    }
}
