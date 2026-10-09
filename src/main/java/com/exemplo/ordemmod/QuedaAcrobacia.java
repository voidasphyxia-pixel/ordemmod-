package com.exemplo.ordemmod;

/**
 * A regra do teste de Acrobacia ao cair de um lugar alto. Java puro, sem nada do Minecraft aqui.
 *
 * O teste é sempre contra {@link #MINIMO} (15). Quem não chega nisso não reduz nada. A partir daí, cada
 * {@link #PASSO} pontos a mais (15, 20, 25, 30...) tira mais {@link #PERCENTUAL_POR_PASSO}% do dano:
 *
 *   total  < 15  →   0%        total 15-19 →  20%      total 20-24 →  40%
 *   total 25-29  →  60%        total 30-34 →  80%      total 35+   → 100%
 *
 * 20 natural no dado zera o dano, qualquer que seja o total.
 */
public final class QuedaAcrobacia {
    /** Total mínimo do teste para começar a reduzir o dano (e para o personagem rolar no chão). */
    public static final int MINIMO = 15;
    /** De quantos em quantos pontos a redução sobe. */
    public static final int PASSO = 5;
    /** Quanto cada degrau tira do dano, em pontos percentuais. */
    public static final int PERCENTUAL_POR_PASSO = 20;

    private QuedaAcrobacia() {
    }

    /** Redução do dano de 0.0 (nada) a 1.0 (zera). */
    public static double reducao(int total, boolean vinteNatural) {
        if (vinteNatural) {
            return 1.0;
        }
        if (total < MINIMO) {
            return 0.0;
        }
        int degraus = (total - MINIMO) / PASSO + 1;
        return Math.min(1.0, degraus * PERCENTUAL_POR_PASSO / 100.0);
    }

    public static double reducao(ResultadoTeste r) {
        return reducao(r.total(), r.critico());
    }

    /** Redução em porcentagem inteira (0 a 100), para mostrar na tela. */
    public static int reducaoPercentual(ResultadoTeste r) {
        return (int) Math.round(reducao(r) * 100.0);
    }

    /** O personagem rola no chão sempre que o teste reduziu algum dano. */
    public static boolean rola(ResultadoTeste r) {
        return reducao(r) > 0.0;
    }
}
