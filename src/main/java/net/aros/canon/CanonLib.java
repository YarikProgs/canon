package net.aros.canon;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(CanonLib.MOD_ID)
public class CanonLib {
    public static final String MOD_ID = "canon";
    public static final Logger LOGGER = LogUtils.getLogger();

    public CanonLib() {
        LOGGER.info("Canon Lib is initializing");
    }
}
