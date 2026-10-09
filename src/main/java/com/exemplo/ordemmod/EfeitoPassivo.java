package com.exemplo.ordemmod;

import java.util.Set;

/**
 * Um efeito permanente de uma habilidade passiva.
 * Cada tipo de efeito é uma pecinha pequena (record); a Habilidade guarda uma lista delas,
 * então criar um efeito novo não exige mexer na classe Habilidade.
 * Os métodos devolvem 0 por padrão: cada efeito só sobrescreve o que realmente faz.
 */
public interface EfeitoPassivo {
    /** Bônus fixo de dano para um tipo de dano. */
    default double bonusDano(TipoDano tipo) {
        return 0;
    }

    /** Aumento percentual de dano para um tipo de dano (0.35 = +35%). */
    default double aumentoPercentual(TipoDano tipo) {
        return 0;
    }

    /** Bônus de defesa. */
    default int bonusDefesa(boolean usandoProtecaoPesada) {
        return 0;
    }

    /** Chance de resistir a efeitos (0.05 = 5%). */
    default double chanceResistir(boolean usandoProtecaoPesada) {
        return 0;
    }

    /** Vida extra por nível de NEX (2 pontos = 1 coração). */
    default int bonusVida(int nivel) {
        return 0;
    }

    /** Dano fixo que é descontado de cada golpe recebido. */
    default double reducaoDanoRecebido(boolean usandoProtecaoPesada) {
        return 0;
    }

    /** Chance de acerto crítico para um tipo de dano (0.10 = 10%). */
    default double chanceCritico(TipoDano tipo) {
        return 0;
    }

    /** Aumento de velocidade de movimento (0.06 = +6%). */
    default double bonusVelocidade() {
        return 0;
    }

    /** +X de dano fixo para certos tipos de dano. */
    record BonusDano(double bonus, Set<TipoDano> tipos) implements EfeitoPassivo {
        public BonusDano {
            tipos = Set.copyOf(tipos);
        }

        @Override
        public double bonusDano(TipoDano tipo) {
            return tipos.contains(tipo) ? bonus : 0;
        }
    }

    /** +X% de dano para certos tipos de dano (fracao: 0.35 = +35%). */
    record AumentoPercentualDano(double fracao, Set<TipoDano> tipos) implements EfeitoPassivo {
        public AumentoPercentualDano {
            tipos = Set.copyOf(tipos);
        }

        @Override
        public double aumentoPercentual(TipoDano tipo) {
            return tipos.contains(tipo) ? fracao : 0;
        }
    }

    /** +X de defesa (sempre, ou só enquanto usa proteção pesada). */
    record BonusDefesa(int bonus, boolean soComProtecaoPesada) implements EfeitoPassivo {
        @Override
        public int bonusDefesa(boolean usandoProtecaoPesada) {
            return (!soComProtecaoPesada || usandoProtecaoPesada) ? bonus : 0;
        }
    }

    /** +X de chance de resistir a efeitos (sempre, ou só com proteção pesada). */
    record ChanceResistir(double chance, boolean soComProtecaoPesada) implements EfeitoPassivo {
        @Override
        public double chanceResistir(boolean usandoProtecaoPesada) {
            return (!soComProtecaoPesada || usandoProtecaoPesada) ? chance : 0;
        }
    }

    /** +X de vida máxima por nível de NEX. */
    record BonusVidaPorNivel(int pontosPorNivel) implements EfeitoPassivo {
        @Override
        public int bonusVida(int nivel) {
            return pontosPorNivel * nivel;
        }
    }

    /** Desconta X pontos de cada golpe recebido (sempre, ou só com proteção pesada). */
    record ReducaoDanoRecebido(double pontos, boolean soComProtecaoPesada) implements EfeitoPassivo {
        @Override
        public double reducaoDanoRecebido(boolean usandoProtecaoPesada) {
            return (!soComProtecaoPesada || usandoProtecaoPesada) ? pontos : 0;
        }
    }

    /** Chance de crítico para certos tipos de dano (fracao: 0.10 = 10%). */
    record ChanceCritico(double fracao, Set<TipoDano> tipos) implements EfeitoPassivo {
        public ChanceCritico {
            tipos = Set.copyOf(tipos);
        }

        @Override
        public double chanceCritico(TipoDano tipo) {
            return tipos.contains(tipo) ? fracao : 0;
        }
    }

    /** +X% de velocidade de movimento (fracao: 0.06 = +6%). */
    record BonusVelocidade(double fracao) implements EfeitoPassivo {
        @Override
        public double bonusVelocidade() {
            return fracao;
        }
    }
}
