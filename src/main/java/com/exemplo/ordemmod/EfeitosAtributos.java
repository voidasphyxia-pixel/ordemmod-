package com.exemplo.ordemmod;

/**
 * Calcula quanto cada atributo muda no jogo.
 * Ajuste as constantes abaixo para balancear o mod.
 */
public final class EfeitosAtributos {
    /** Cada ponto de Vigor dá 2 pontos de vida (1 coração). */
    public static final int VIDA_POR_VIGOR = 2;

    /** Cada ponto de Agilidade dá 4% a mais de velocidade de movimento. */
    public static final double VELOCIDADE_POR_AGILIDADE = 0.04;

    /** Cada ponto de Força dá 0,5 de dano corpo a corpo. */
    public static final double DANO_POR_FORCA = 0.5;

    private EfeitosAtributos() {
    }

    public static int bonusVida(Atributos atributos) {
        return atributos.get(Atributo.VIGOR) * VIDA_POR_VIGOR;
    }

    public static double bonusVelocidade(Atributos atributos) {
        return atributos.get(Atributo.AGILIDADE) * VELOCIDADE_POR_AGILIDADE;
    }

    public static double bonusDano(Atributos atributos) {
        return atributos.get(Atributo.FORCA) * DANO_POR_FORCA;
    }

    /** Vida máxima final: vida padrão + bônus da classe + bônus por nível + bônus de Vigor. */
    public static int vidaMaximaTotal(ClasseOP classe, int nivel, Atributos atributos) {
        return classe.vidaMaxima(nivel) + bonusVida(atributos);
    }

    /** Vida máxima final contando também a vida extra das habilidades (ex.: Casca Grossa). */
    public static int vidaMaximaTotal(ClasseOP classe, int nivel, Atributos atributos,
                                      HabilidadesDoPersonagem habilidades) {
        return vidaMaximaTotal(classe, nivel, atributos) + habilidades.bonusVida(nivel);
    }

    /** Velocidade extra total: Agilidade + habilidades (ex.: Iniciativa Aprimorada). */
    public static double velocidadeTotal(Atributos atributos, HabilidadesDoPersonagem habilidades) {
        return bonusVelocidade(atributos) + habilidades.bonusVelocidade();
    }
}
