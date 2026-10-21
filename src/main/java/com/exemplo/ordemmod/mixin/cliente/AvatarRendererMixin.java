package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.cliente.PoseFormacao;
import com.exemplo.ordemmod.cliente.PoseFormacaoAcesso;

import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;

/** Ao extrair o estado de render do jogador, calcula o progresso da pose de formacao da Armadura de Sangue. */
@Mixin(AvatarRenderer.class)
public abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void ordemmod$poseFormacao(Avatar avatar, AvatarRenderState estado, float parcial, CallbackInfo ci) {
        if (estado instanceof PoseFormacaoAcesso acesso) {
            acesso.ordemmod$setJogadorId(avatar.getId());
            acesso.ordemmod$setProgresso(PoseFormacao.progresso(avatar.getId(), parcial));
        }
    }
}
