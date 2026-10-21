package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.cliente.PoseFormacao;
import com.exemplo.ordemmod.cliente.PoseFormacaoAcesso;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Levanta o braco direito do PERSONAGEM durante a formacao da Armadura de Sangue (mesmo gancho usado pelo Not Enough Animations). */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ordemmod$levantarBraco(AvatarRenderState estado, CallbackInfo ci) {
        if (!(estado instanceof PoseFormacaoAcesso acesso)) {
            return;
        }
        float p = acesso.ordemmod$getProgresso();
        if (p <= 0.0F) {
            return;
        }
        PlayerModel modelo = (PlayerModel) (Object) this;
        PoseFormacao.aplicar(modelo.rightArm, p);
    }
}
