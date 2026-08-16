package net.aros.canon.core.state.type;

import com.mojang.serialization.Codec;
import net.aros.canon.CanonLibMod;
import net.aros.canon.event.custom.StateRegistrationEvent;
import net.aros.canon.util.CanonExtraCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static net.aros.canon.CanonLibMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class BuiltinStateTypes {
    public static final StateType<Boolean> BOOL = of("bool", Codec.BOOL, false);
    public static final StateType<Character> CHAR = of("char", CanonExtraCodecs.CHAR, '\0');
    public static final StateType<Byte> BYTE = of("byte", Codec.BYTE, (byte) 0);
    public static final StateType<Short> SHORT = of("short", Codec.SHORT, (short) 0);
    public static final StateType<Integer> INT = of("int", Codec.INT, 0);
    public static final StateType<Long> LONG = of("long", Codec.LONG, 0L);
    public static final StateType<Float> FLOAT = of("float", Codec.FLOAT, 0F);
    public static final StateType<Double> DOUBLE = of("double", Codec.DOUBLE, 0D);

    public static final StateType<String> STRING = of("string", Codec.STRING, "");
    public static final StateType<Optional<ResourceLocation>> OPT_RESOURCE_LOCATION
            = of("resource_location", CanonExtraCodecs.OPT_RESOURCE_LOCATION, Optional.empty());

    @Contract("_, _, _ -> new")
    private static <T> @NotNull StateType<T> of(String name, Codec<T> codec, T defaultValue) {
        return new StateType<>(CanonLibMod.id(name), codec, defaultValue);
    }

    @SubscribeEvent
    public static void onStateRegistration(StateRegistrationEvent event) {
        event.registerType(BOOL);
        event.registerType(CHAR);
        event.registerType(BYTE);
        event.registerType(SHORT);
        event.registerType(INT);
        event.registerType(LONG);
        event.registerType(FLOAT);
        event.registerType(DOUBLE);
        event.registerType(STRING);
        event.registerType(OPT_RESOURCE_LOCATION);
    }
}
