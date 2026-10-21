package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.exemplo.ordemmod.cliente.PoseFormacaoAcesso;

import net.minecraft.client.renderer.entity.state.AvatarRenderState;

/** Guarda no estado de render do jogador o quanto o braco direito deve estar levantado (formacao da Armadura de Sangue). */
@Mixin(AvatarRenderState.class)
public abstract class AvatarRenderStateMixin implements PoseFormacaoAcesso {
    @Unique
    private float ordemmod$progresso;

    @Unique
    private int ordemmod$jogadorId = -1;

    @Override
    public int ordemmod$getJogadorId() {
        return this.ordemmod$jogadorId;
    }

    @Override
    public void ordemmod$setJogadorId(int id) {
        this.ordemmod$jogadorId = id;
    }

    @Override
    public float ordemmod$getProgresso() {
        return this.ordemmod$progresso;
    }

    @Override
    public void ordemmod$setProgresso(float progresso) {
        this.ordemmod$progresso = progresso;
    }
}
