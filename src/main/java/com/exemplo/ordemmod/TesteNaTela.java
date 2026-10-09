package com.exemplo.ordemmod;

import java.util.List;
import java.util.Random;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * A porta de entrada para "rolar um teste COM animação na tela do jogador".
 * O servidor rola (Pericias.testar), separa o bônus por fonte e manda o pacote; o cliente toca a animação.
 *
 * Use de qualquer lugar do servidor, por exemplo num ponto de investigação ou numa conversa:
 *   TesteNaTela.rolar(jogador, Pericia.MEDICINA, 20, "Examinar o corpo");
 */
public final class TesteNaTela {
    private static final Random ALEATORIO = new Random();

    private TesteNaTela() {
    }

    public static ResultadoTeste rolar(ServerPlayer jogador, Pericia pericia, int dt, String motivo) {
        return rolar(jogador, pericia, dt, 0, null, motivo);
    }

    /** {@code bonusExtra} entra na soma com o nome {@code rotuloExtra} (ex.: +2 "Lupa"). */
    public static ResultadoTeste rolar(ServerPlayer jogador, Pericia pericia, int dt, int bonusExtra,
            String rotuloExtra, String motivo) {
        Personagem p = EventosOrdem.de(jogador);
        ResultadoTeste r = p.pericias.testar(pericia, dt, bonusExtra, ALEATORIO);
        List<ParteBonus> partes = p.pericias.partesDoBonus(pericia, bonusExtra, rotuloExtra);
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador), PacoteTeste.de(r, partes, motivo));
        return r;
    }
}
