package net.aros.canon.core.flag.scope;

import com.mojang.serialization.Codec;
import net.aros.canon.CanonLibMod;
import net.aros.canon.event.custom.FlagRegistrationEvent;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Unit;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.UUID;

@EventBusSubscriber(modid = CanonLibMod.MOD_ID)
public class BuiltinScopeTypes {
    public static final ScopeType<Unit> GLOBAL = new ScopeType<>(CanonLibMod.id("global"), Codec.unit(Unit.INSTANCE));
    public static final ScopeType<UUID> ENTITY = new ScopeType<>(CanonLibMod.id("entity"), UUIDUtil.STRING_CODEC);
    public static final ScopeType<ResourceKey<Level>> DIMENSION = new ScopeType<>(CanonLibMod.id("dimension"), ResourceKey.codec(Registries.DIMENSION));

    @SubscribeEvent
    public static void onFlagRegistration(FlagRegistrationEvent event) {
        event.registerScopeType(GLOBAL);
        event.registerScopeType(ENTITY);
        event.registerScopeType(DIMENSION);
    }
}
