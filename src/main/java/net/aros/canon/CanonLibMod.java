package net.aros.canon;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.Can;
import net.aros.canon.core.migration.custom.Char2StringMigrator;
import net.aros.canon.core.migration.custom.NumBoolMigrator;
import net.aros.canon.core.migration.custom.NumNumMigrator;
import net.aros.canon.core.migration.custom.StringRLMigrator;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.common.Mod;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(CanonLibMod.MOD_ID)
public class CanonLibMod {
    public static final String MOD_ID = "canon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CanonLibMod() {
        LOGGER.info("Canon Lib is initializing");

        Can.registerMigrator(new NumNumMigrator());
        Can.registerMigrator(new NumBoolMigrator());
        Can.registerMigrator(new StringRLMigrator());
        Can.registerMigrator(new Char2StringMigrator());
    }

    @Contract("_ -> new")
    public static @NotNull ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
