package com.exemplo.ordemmod;

import java.util.EnumMap;

/**
 * Guarda os valores dos cinco atributos de um personagem.
 * Regras de criação: todos começam em 1, o jogador distribui 4 pontos,
 * nenhum passa de 3, e um atributo pode cair para 0 para render 1 ponto extra.
 * Depois da criação, o máximo NATURAL (aprimoramento de nível) é 5; habilidades e rituais
 * podem passar disso, mas só temporariamente.
 */
public class Atributos {
    public static final int VALOR_INICIAL = 1;
    public static final int PONTOS_PARA_DISTRIBUIR = 4;
    public static final int MAXIMO_NA_CRIACAO = 3;
    /** Valor máximo de forma natural (o pentágono vai até aqui). Bônus temporários podem passar. */
    public static final int MAXIMO_NATURAL = 5;

    private final EnumMap<Atributo, Integer> valores = new EnumMap<>(Atributo.class);

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
