package com.exemplo.ordemmod.cliente;

/** Interface colocada no AvatarRenderState pelo AvatarRenderStateMixin: progresso da pose de formacao (0..1) e id do jogador. */
public interface PoseFormacaoAcesso {
    float ordemmod$getProgresso();

    void ordemmod$setProgresso(float progresso);

    int ordemmod$getJogadorId();

    void ordemmod$setJogadorId(int id);
}
