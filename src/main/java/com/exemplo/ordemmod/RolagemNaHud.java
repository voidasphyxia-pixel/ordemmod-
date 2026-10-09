package com.exemplo.ordemmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Porta de entrada, no servidor, para mostrar uma rolagem JÁ FEITA no painel acima da barra de sanidade.
 * Diferente de {@link TesteNaTela}, não rola nada e não abre a tela do dado: só desenha o que o servidor decidiu.
 */
public final class RolagemNaHud {
    private RolagemNaHud() {
    }

    /** {@code reducaoPercentual} de 0 a 100 mostra "-X% de dano"; use -1 para não falar de dano. */
    public static void mostrar(ServerPlayer jogador, ResultadoTeste r, int reducaoPercentual, String motivo) {
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador),
                PacoteRolagemHud.de(r, reducaoPercentual, motivo));
    }
}
