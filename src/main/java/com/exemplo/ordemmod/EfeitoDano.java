package com.exemplo.ordemmod;

import java.util.Set;

/**
 * Um efeito temporário de dano: soma um bônus de dano fixo, a certos tipos de dano,
 * por um tempo. Exemplo: +5 de dano corpo a corpo e à distância por 30 segundos.
 */
public class EfeitoDano {
    private final double bonusDano;
    private final int duracaoSegundos;
    private final Set<TipoDano> tiposAfetados;

    public EfeitoDano(double bonusDano, int duracaoSegundos, Set<TipoDano> tiposAfetados) {
        this.bonusDano = bonusDano;
        this.duracaoSegundos = duracaoSegundos;
        this.tiposAfetados = Set.copyOf(tiposAfetados);
    }

    public double getBonusDano() {
        return bonusDano;
    }

    public int getDuracaoSegundos() {
        return duracaoSegundos;
    }

    /** Esse efeito afeta esse tipo de dano? */
    public boolean afeta(TipoDano tipo) {
        return tiposAfetados.contains(tipo);
    }
}
