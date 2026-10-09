package com.exemplo.ordemmod;

/**
 * Um efeito temporário defensivo: soma defesa e chance de resistir a efeitos
 * por um tempo. Exemplo: Sentido Tático (defesa e resistência ligadas ao Intelecto).
 */
public class EfeitoDefesa {
    private final int bonusDefesa;
    private final double chanceResistir; // 0.15 = 15%
    private final int duracaoSegundos;

    public EfeitoDefesa(int bonusDefesa, double chanceResistir, int duracaoSegundos) {
        this.bonusDefesa = bonusDefesa;
        this.chanceResistir = chanceResistir;
        this.duracaoSegundos = duracaoSegundos;
    }

    public int getBonusDefesa() {
        return bonusDefesa;
    }

    public double getChanceResistir() {
        return chanceResistir;
    }

    public int getDuracaoSegundos() {
        return duracaoSegundos;
    }
}
