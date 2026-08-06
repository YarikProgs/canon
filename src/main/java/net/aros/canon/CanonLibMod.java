package net.aros.canon;

import com.mojang.logging.LogUtils;
import net.aros.canon.core.Canon;
import net.aros.canon.examples.TestItem;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
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

        // touching event registration inside of class
        try {
            Class.forName("net.aros.canon.core.Canon");
        } catch (ClassNotFoundException ignore) {
        }
    }
}
