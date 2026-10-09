package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Tabela de benefícios por patamar. Cada linha é um efeito de uma perícia, com um valor para
 * Treinado, Veterano e Expert. Destreinado sempre vale 0.
 *
 * Quer balancear? Mexa só nos números daqui: o resto do mod lê esta tabela.
 *
 * Percentuais estão em fração (0.10 = 10%).
 */
public enum Beneficio {
    // ---- combate
    LUTA_DANO(Pericia.LUTA, "Dano corpo a corpo e desarmado", Unidade.PERCENTUAL, 0.10, 0.20, 0.35),
    LUTA_CRITICO(Pericia.LUTA, "Chance de crítico corpo a corpo", Unidade.PERCENTUAL, 0.05, 0.10, 0.15),
    PONTARIA_DANO(Pericia.PONTARIA, "Dano à distância", Unidade.PERCENTUAL, 0.10, 0.20, 0.35),
    PONTARIA_CRITICO(Pericia.PONTARIA, "Chance de crítico à distância", Unidade.PERCENTUAL, 0.05, 0.10, 0.15),
    INVESTIGACAO_CRITICO(Pericia.INVESTIGACAO, "Ponto fraco: multiplicador de crítico", Unidade.PONTOS, 0.10, 0.20, 0.30),
    TATICA_ALIADOS(Pericia.TATICA, "Dano com outro jogador por perto", Unidade.PERCENTUAL, 0.05, 0.10, 0.15),
    OCULTISMO_MORTOS_VIVOS(Pericia.OCULTISMO, "Dano contra mortos-vivos", Unidade.PERCENTUAL, 0.10, 0.20, 0.35),
    INTIMIDACAO_FRAQUEZA(Pericia.INTIMIDACAO, "Chance de enfraquecer o monstro ao acertar", Unidade.PERCENTUAL, 0.10, 0.20, 0.30),
    ADESTRAMENTO_DANO(Pericia.ADESTRAMENTO, "Dano dos seus bichos domados", Unidade.PERCENTUAL, 0.10, 0.25, 0.50),
    ADESTRAMENTO_VIDA(Pericia.ADESTRAMENTO, "Vida extra dos bichos que você doma", Unidade.PONTOS, 4, 8, 12),

    // ---- defesa
    FORTITUDE_REDUCAO(Pericia.FORTITUDE, "Redução de dano recebido", Unidade.PERCENTUAL, 0.05, 0.10, 0.15),
    REFLEXOS_ESQUIVA(Pericia.REFLEXOS, "Dano evitado na esquiva parcial", Unidade.PERCENTUAL, 0.50, 0.60, 0.75),
    ACROBACIA_QUEDA(Pericia.ACROBACIA, "Redução de dano de queda", Unidade.PERCENTUAL, 0.20, 0.40, 0.60),
    CIENCIAS_AMBIENTE(Pericia.CIENCIAS, "Redução de dano de fogo, lava e explosão", Unidade.PERCENTUAL, 0.10, 0.20, 0.30),
    RELIGIAO_MORTOS_VIVOS(Pericia.RELIGIAO, "Redução de dano de mortos-vivos", Unidade.PERCENTUAL, 0.10, 0.20, 0.30),
    PILOTAGEM_MONTADO(Pericia.PILOTAGEM, "Redução de dano enquanto montado", Unidade.PERCENTUAL, 0.10, 0.20, 0.30),

    // ---- furtividade e percepção
    FURTIVIDADE_DANO(Pericia.FURTIVIDADE, "Dano do ataque furtivo", Unidade.PERCENTUAL, 0.50, 1.00, 1.50),
    FURTIVIDADE_TEMPO(Pericia.FURTIVIDADE, "Tempo despercebido", Unidade.SEGUNDOS, 5, 7, 10),
    ENGANACAO_DESVIO(Pericia.ENGANACAO, "Chance de o monstro perder o alvo", Unidade.PERCENTUAL, 0.10, 0.20, 0.30),
    INICIATIVA_RECARGA(Pericia.INICIATIVA, "Recarga do \"agir primeiro\"", Unidade.SEGUNDOS, 30, 20, 10),
    INICIATIVA_NIVEL(Pericia.INICIATIVA, "Nível da Velocidade ao agir primeiro", Unidade.NIVEL, 1, 1, 2),
    PERCEPCAO_RAIO(Pericia.PERCEPCAO, "Raio de detecção extra", Unidade.BLOCOS, 2, 5, 7),
    PERCEPCAO_BRILHO(Pericia.PERCEPCAO, "Tempo que os monstros brilham", Unidade.SEGUNDOS, 3, 5, 8),

    // ---- corpo e mundo
    ATLETISMO_VELOCIDADE(Pericia.ATLETISMO, "Velocidade de movimento", Unidade.PERCENTUAL, 0.03, 0.06, 0.10),
    SOBREVIVENCIA_SATURACAO(Pericia.SOBREVIVENCIA, "Saturação extra ao comer", Unidade.PONTOS, 0.5, 1.0, 1.5),
    SOBREVIVENCIA_FOME(Pericia.SOBREVIVENCIA, "Fome extra restaurada ao comer", Unidade.PONTOS, 0, 1, 2),
    ATUALIDADES_XP(Pericia.ATUALIDADES, "XP ganho", Unidade.PERCENTUAL, 0.10, 0.20, 0.30),
    MEDICINA_CURA(Pericia.MEDICINA, "Vida curada pela atadura", Unidade.PONTOS, 4, 6, 8),
    MEDICINA_RECARGA(Pericia.MEDICINA, "Recarga entre ataduras", Unidade.SEGUNDOS, 5, 4, 3),
    CRIME_GAZUA(Pericia.CRIME, "Chance da gazua não quebrar", Unidade.PERCENTUAL, 0.25, 0.50, 0.75),

    // ---- mente
    VONTADE_SANIDADE(Pericia.VONTADE, "Sanidade recuperada a cada 30 s", Unidade.PONTOS, 0, 1, 2),
    VONTADE_RESISTENCIA(Pericia.VONTADE, "Chance de resistir a efeitos mentais", Unidade.PERCENTUAL, 0.05, 0.10, 0.15),
    FORTITUDE_RESISTENCIA(Pericia.FORTITUDE, "Chance de resistir a efeitos físicos", Unidade.PERCENTUAL, 0.05, 0.10, 0.15);

    // Sem efeito de jogo ainda: Artes, Diplomacia, Intuição, Profissão, Tecnologia.

    public enum Unidade {
        PERCENTUAL, PONTOS, SEGUNDOS, BLOCOS, NIVEL
    }

    private final Pericia pericia;
    private final String descricao;
    private final Unidade unidade;
    private final double treinado;
    private final double veterano;
    private final double expert;

    Beneficio(Pericia pericia, String descricao, Unidade unidade, double treinado, double veterano, double expert) {
        this.pericia = pericia;
        this.descricao = descricao;
        this.unidade = unidade;
        this.treinado = treinado;
        this.veterano = veterano;
        this.expert = expert;
    }

    public Pericia getPericia() {
        return pericia;
    }

    public String getDescricao() {
        return descricao;
    }

    public Unidade getUnidade() {
        return unidade;
    }

    /** Valor do benefício num grau. Destreinado = 0. */
    public double valor(GrauTreinamento grau) {
        return switch (grau) {
            case DESTREINADO -> 0;
            case TREINADO -> treinado;
            case VETERANO -> veterano;
            case EXPERT -> expert;
        };
    }

    /** Texto do valor, já com a unidade: "+20%", "+4", "10 s"... */
    public String formatar(GrauTreinamento grau) {
        double v = valor(grau);
        return switch (unidade) {
            case PERCENTUAL -> "+" + Math.round(v * 100) + "%";
            case PONTOS -> "+" + texto(v);
            case SEGUNDOS -> texto(v) + " s";
            case BLOCOS -> "+" + texto(v) + " blocos";
            case NIVEL -> "nível " + texto(v);
        };
    }

    /** Linha completa para o chat: "Dano corpo a corpo: T +10% | V +20% | E +35%". */
    public String linha() {
        return descricao + ": T " + formatar(GrauTreinamento.TREINADO)
                + " | V " + formatar(GrauTreinamento.VETERANO)
                + " | E " + formatar(GrauTreinamento.EXPERT);
    }

    /** Todos os benefícios de uma perícia (lista vazia se ela ainda não tem efeito de jogo). */
    public static List<Beneficio> de(Pericia pericia) {
        List<Beneficio> lista = new ArrayList<>();
        for (Beneficio b : values()) {
            if (b.pericia == pericia) {
                lista.add(b);
            }
        }
        return lista;
    }

    private static String texto(double v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }
}
