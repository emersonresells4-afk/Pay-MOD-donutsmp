package com.example.donorfx.mixin;

import com.example.donorfx.CenaMode;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the texture of every rendered player while Cena mode is on.
 * The handler takes no target arguments on purpose so it is valid for any overload.
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "getTexture", at = @At("RETURN"), cancellable = true, require = 0)
    private void donorfx$cena(CallbackInfoReturnable<Identifier> cir) {
        if (CenaMode.enabled) {
            cir.setReturnValue(CenaMode.TEXTURE);
        }
    }
}
