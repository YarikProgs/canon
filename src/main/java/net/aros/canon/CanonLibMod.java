package net.aros.canon;

import com.mojang.logging.LogUtils;
import net.aros.canon.examples.TestItem;
import net.aros.canon.migration.custom.Char2StringMigrator;
import net.aros.canon.migration.custom.NumBoolMigrator;
import net.aros.canon.migration.custom.NumNumMigrator;
import net.aros.canon.migration.custom.StringRLMigrator;
import net.aros.canon.wrapper.Can;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(CanonLibMod.MOD_ID)
public class CanonLibMod {
    public static final String MOD_ID = "canon";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredItem<TestItem> TEST_ITEM = ITEMS.registerItem("test_item", TestItem::new);

    public CanonLibMod(IEventBus bus) {
        LOGGER.info("Canon Lib is initializing");

        ITEMS.register(bus);

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
