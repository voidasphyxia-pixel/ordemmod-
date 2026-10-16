package com.exemplo.ordemmod;

import java.util.EnumMap;

/**
 * Guarda os valores dos cinco atributos de um personagem.
 * Regras de criação: todos começam em 1, o jogador distribui 4 pontos,
 * nenhum passa de 3, e um atributo pode cair para 0 para render 1 ponto extra.
 * Depois da criação, o máximo NATURAL (aprimoramento de nível) é 5; habilidades e rituais
 * podem passar disso, mas só temporariamente.
 * Os pontos do aprimoramento chegam nos NEX de {@link #MARCOS_NEX} (1 ponto em cada) e ficam guardados em
 * {@link #getPontosLivres()} até o jogador gastar cada um num atributo, até o limite de {@link #MAXIMO_NATURAL}.
 */
public class Atributos {
    public static final int VALOR_INICIAL = 1;
    public static final int PONTOS_PARA_DISTRIBUIR = 4;
    public static final int MAXIMO_NA_CRIACAO = 3;
    /** Valor máximo de forma natural (o pentágono vai até aqui). Bônus temporários podem passar. */
    public static final int MAXIMO_NATURAL = 5;
    /** NEX em que o personagem ganha pontos para distribuir nos atributos. */
    public static final int[] MARCOS_NEX = {20, 50, 80, 95};
    /** Pontos de atributo que cada marco de {@link #MARCOS_NEX} dá. */
    public static final int PONTOS_POR_MARCO = 1;

    private final EnumMap<Atributo, Integer> valores = new EnumMap<>(Atributo.class);
    /** Pontos de aprimoramento ainda não gastos (chegam nos NEX de {@link #MARCOS_NEX}). */
    private int pontosLivres = 0;

    public Atributos() {
        for (Atributo atributo : Atributo.values()) {
            valores.put(atributo, VALOR_INICIAL);
        }
    }

    public int get(Atributo atributo) {
        return valores.get(atributo);
    }

    public void set(Atributo atributo, int valor) {
        valores.put(atributo, valor);
    }

    // ------------------------------------------------------- aprimoramento por NEX

    /** Esse NEX é um dos marcos que dão pontos de atributo? */
    public static boolean ehMarco(int nex) {
        for (int marco : MARCOS_NEX) {
            if (marco == nex) {
                return true;
            }
        }
        return false;
    }

    /** Quantos pontos de atributo o personagem já deveria ter recebido até esse NEX (para saves antigos). */
    public static int pontosAteNex(int nex) {
        int total = 0;
        for (int marco : MARCOS_NEX) {
            if (nex >= marco) {
                total += PONTOS_POR_MARCO;
            }
        }
        return total;
    }

    public int getPontosLivres() {
        return pontosLivres;
    }

    /** Dá pontos para gastar (ao chegar num marco de NEX). */
    public void conceder(int pontos) {
        pontosLivres += Math.max(0, pontos);
    }

    /** Usado ao carregar o jogo salvo. */
    public void restaurarPontos(int pontos) {
        pontosLivres = Math.max(0, pontos);
    }

    /** Dá para gastar um ponto nesse atributo agora? (tem ponto e o atributo ainda não chegou ao máximo natural). */
    public boolean podeSubir(Atributo atributo) {
        return pontosLivres > 0 && get(atributo) < MAXIMO_NATURAL;
    }

    /** Gasta 1 ponto livre e sobe o atributo em 1, até {@link #MAXIMO_NATURAL}. Retorna false, sem mudar nada, se não der. */
    public boolean subir(Atributo atributo) {
        if (!podeSubir(atributo)) {
            return false;
        }
        valores.put(atributo, get(atributo) + 1);
        pontosLivres--;
        return true;
    }

    /** Confere se a distribuição segue as regras de criação de personagem. */
    public boolean distribuicaoValida() {
        int total = 0;
        int zeros = 0;
        for (int valor : valores.values()) {
            if (valor < 0 || valor > MAXIMO_NA_CRIACAO) {
                return false;
            }
            if (valor == 0) {
                zeros++;
            }
            total += valor;
        }
        int totalPermitido = VALOR_INICIAL * Atributo.values().length + PONTOS_PARA_DISTRIBUIR;
        return zeros <= 1 && total <= totalPermitido + zeros;
    }
}
