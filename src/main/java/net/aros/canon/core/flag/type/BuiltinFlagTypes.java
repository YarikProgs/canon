package net.aros.canon.core.flag.type;

import com.mojang.serialization.Codec;
import net.aros.canon.CanonLibMod;
import net.aros.canon.event.custom.FlagRegistrationEvent;
import net.aros.canon.util.CanonExtraCodecs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

import static net.aros.canon.CanonLibMod.MOD_ID;

@EventBusSubscriber(modid = MOD_ID)
public class BuiltinFlagTypes {
    public static final FlagType<Boolean> BOOL = of("bool", Codec.BOOL, false);
    public static final FlagType<Character> CHAR = of("char", CanonExtraCodecs.CHAR, '\0');
    public static final FlagType<Byte> BYTE = of("byte", Codec.BYTE, (byte) 0);
    public static final FlagType<Short> SHORT = of("short", Codec.SHORT, (short) 0);
    public static final FlagType<Integer> INT = of("int", Codec.INT, 0);
    public static final FlagType<Long> LONG = of("long", Codec.LONG, 0L);
    public static final FlagType<Float> FLOAT = of("float", Codec.FLOAT, 0F);
    public static final FlagType<Double> DOUBLE = of("double", Codec.DOUBLE, 0D);

    public static final FlagType<String> STRING = of("string", Codec.STRING, "");
    public static final FlagType<Optional<ResourceLocation>> OPT_RESOURCE_LOCATION
            = of("resource_location", CanonExtraCodecs.OPT_RESOURCE_LOCATION, Optional.empty());

    @Contract("_, _, _ -> new")
    private static <T> @NotNull FlagType<T> of(String name, Codec<T> codec, T defaultValue) {
        return new FlagType<>(CanonLibMod.id(name), codec, defaultValue);
    }

    @SubscribeEvent
    public static void onFlagRegister(FlagRegistrationEvent event) {
        event.registerTypes(BOOL, CHAR, BYTE, SHORT, INT, LONG, FLOAT, DOUBLE, STRING, OPT_RESOURCE_LOCATION);
    }
}
