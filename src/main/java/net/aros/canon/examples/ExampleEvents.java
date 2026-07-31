package net.aros.canon.examples;

import net.aros.canon.event.EventKey;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

public class ExampleEvents {
    public static final EventKey<LivingDeathEvent> LIVING_DEATH = EventKey.of("living_death", LivingDeathEvent.class);
}
