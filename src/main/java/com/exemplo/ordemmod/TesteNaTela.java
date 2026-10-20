package com.exemplo.ordemmod;

import java.util.List;
import java.util.Random;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

/**
 * A porta de entrada para "rolar um teste COM animacao na tela do jogador".
 * O servidor rola (Pericias.testar), separa o bonus por fonte e manda o pacote; o cliente toca a animacao.
 *
 * Use de qualquer lugar do servidor, por exemplo num ponto de investigacao ou numa conversa:
 *   TesteNaTela.rolar(jogador, Pericia.MEDICINA, 20, "Examinar o corpo");
 *
 * Poderes de investigacao do Combatente (os testes continuam com dados):
 *  - Presteza Atletica: o proximo teste usa Forca ou Agilidade (o maior) no lugar do atributo-base;
 *  - Incansavel: igual, e o teste e feito duas vezes, ficando com o melhor resultado.
 * Os dois poderes deixam um marcador ativo (EfeitosAtivos) que e gasto no proximo teste.
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

        boolean presteza = p.efeitos.estaAtivo(CatalogoHabilidades.ID_PRESTEZA_ATLETICA);
        boolean incansavel = p.efeitos.estaAtivo(CatalogoHabilidades.ID_INCANSAVEL);
        int atributoMinimo = 0;
        if (presteza || incansavel) {
            atributoMinimo = Math.max(p.atributos.get(Atributo.FORCA), p.atributos.get(Atributo.AGILIDADE));
            p.efeitos.remover(CatalogoHabilidades.ID_PRESTEZA_ATLETICA); // o marcador vale para um teste so
            p.efeitos.remover(CatalogoHabilidades.ID_INCANSAVEL);
        }

        ResultadoTeste r = p.pericias.testar(pericia, dt, bonusExtra, atributoMinimo, ALEATORIO);
        if (incansavel) { // acao adicional: segunda tentativa, fica com a melhor
            ResultadoTeste segunda = p.pericias.testar(pericia, dt, bonusExtra, atributoMinimo, ALEATORIO);
            boolean melhor = segunda.sucesso() == r.sucesso() ? segunda.total() > r.total() : segunda.sucesso();
            if (melhor) {
                r = segunda;
            }
        }
        if (presteza || incansavel) {
            Avisos.enviar(jogador, (incansavel ? "Incansável" : "Presteza Atlética")
                    + ": teste com Força/Agilidade.", ChatFormatting.GREEN);
        }

        List<ParteBonus> partes = p.pericias.partesDoBonus(pericia, bonusExtra, rotuloExtra);
        Rede.paraJogador(jogador, PacoteTeste.de(r, partes, motivo));
        return r;
    }
}
