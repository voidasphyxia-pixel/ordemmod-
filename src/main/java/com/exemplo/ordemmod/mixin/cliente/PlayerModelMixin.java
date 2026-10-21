package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.cliente.PoseFormacao;
import com.exemplo.ordemmod.cliente.PoseFormacaoAcesso;
import com.exemplo.ordemmod.entidade.ArmaduraSangueJogadorEntity;

import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Levanta o braco direito do PERSONAGEM durante a formacao da Armadura de Sangue (mesmo gancho usado pelo Not Enough Animations). */
@Mixin(value = PlayerModel.class, priority = 2000) // prioridade alta: roda DEPOIS do EMF/NEA no fim do setupAnim
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("RETURN"))
    private void ordemmod$levantarBraco(AvatarRenderState estado, CallbackInfo ci) {
        if (!(estado instanceof PoseFormacaoAcesso acesso)) {
            return;
        }
        PlayerModel modelo = (PlayerModel) (Object) this;
        float p = acesso.ordemmod$getProgresso();
        if (p > 0.0F) {
            PoseFormacao.aplicar(modelo.rightArm, p);
        }
        // Armadura de Sangue ativa neste jogador: guarda a pose final (com resource packs/mods) para a armadura copiar.
        int id = acesso.ordemmod$getJogadorId();
        if (id >= 0 && ArmaduraSangueJogadorEntity.doDono(id) != null) {
            PoseFormacao.Capturada.guardar(id, modelo.head, modelo.body, modelo.leftArm, modelo.rightArm,
                    modelo.leftLeg, modelo.rightLeg);
        }
    }
}
