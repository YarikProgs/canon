package net.aros.canon.core.state.scope;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.aros.canon.util.GsonHelper;
import net.minecraft.resources.ResourceLocation;

public record ScopeType<S>(ResourceLocation identifier, Codec<S> scopeCodec) {
    public DataResult<S> parseScope(String scope) {
        return scopeCodec.parse(JsonOps.INSTANCE, GsonHelper.parse(scope));
    }
}
