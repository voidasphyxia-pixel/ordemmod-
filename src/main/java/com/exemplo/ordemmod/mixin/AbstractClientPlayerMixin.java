package com.exemplo.ordemmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exemplo.ordemmod.aparencia.AparenciaCliente;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

/**
 * Troca a skin do jogador pela skin montada da aparência. Todo desenho do jogador (terceira pessoa,
 * mão em primeira pessoa, Epic Fight) pergunta a skin por aqui. Quem não tem aparência escolhida
 * continua com a skin normal.
 */
@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    @Inject(method = "getSkinTextureLocation", at = @At("HEAD"), cancellable = true)
    private void ordemmod$skin(CallbackInfoReturnable<ResourceLocation> retorno) {
        ResourceLocation textura = AparenciaCliente.texturaDe(((AbstractClientPlayer) (Object) this).getUUID());
        if (textura != null) {
            retorno.setReturnValue(textura);
        }
    }

    /** As peças são feitas para o braço grosso (Steve): força esse modelo. */
    @Inject(method = "getModelName", at = @At("HEAD"), cancellable = true)
    private void ordemmod$modelo(CallbackInfoReturnable<String> retorno) {
        if (AparenciaCliente.de(((AbstractClientPlayer) (Object) this).getUUID()) != null) {
            retorno.setReturnValue("default");
        }
    }
}
