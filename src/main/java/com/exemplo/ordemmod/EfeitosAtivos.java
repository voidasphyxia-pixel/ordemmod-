package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.Map;

/**
 * Os efeitos temporários que estão ativos em um personagem (de dano e de defesa).
 * Chame tick() a cada tick do jogo (20 vezes por segundo) para o tempo passar.
 * Ativar de novo o mesmo efeito reinicia o tempo, sem acumular.
 * Uma mesma habilidade pode ter efeito de dano E de defesa ao mesmo tempo:
 * cada tipo é guardado (e contado) separadamente, então um não apaga o outro.
 */
public class EfeitosAtivos {
    private static class Ativo<T> {
        final T efeito;
        int ticksRestantes;

        Ativo(T efeito, int ticksRestantes) {
            this.efeito = efeito;
            this.ticksRestantes = ticksRestantes;
        }
    }

    private final Map<String, Ativo<EfeitoDano>> danos = new HashMap<>();
    private final Map<String, Ativo<EfeitoDefesa>> defesas = new HashMap<>();

    /** Ativa um efeito de dano (o id é o da habilidade que o gerou). */
    public void ativar(String id, EfeitoDano efeito) {
        int ticks = efeito.getDuracaoSegundos() * PontosEsforco.TICKS_POR_SEGUNDO;
        danos.put(id, new Ativo<>(efeito, ticks));
    }

    /** Ativa um efeito de defesa (o id é o da habilidade que o gerou). */
    public void ativar(String id, EfeitoDefesa efeito) {
        int ticks = efeito.getDuracaoSegundos() * PontosEsforco.TICKS_POR_SEGUNDO;
        defesas.put(id, new Ativo<>(efeito, ticks));
    }

    /** Faz o tempo passar 1 tick e remove os efeitos que acabaram. */
    public void tick() {
        avancar(danos);
        avancar(defesas);
    }

    private static <T> void avancar(Map<String, Ativo<T>> ativos) {
        for (Ativo<T> ativo : ativos.values()) {
            ativo.ticksRestantes--;
        }
        ativos.values().removeIf(ativo -> ativo.ticksRestantes <= 0);
    }

    /** Tem algum efeito (de dano ou de defesa) ativo com esse id? */
    public boolean estaAtivo(String id) {
        return danos.containsKey(id) || defesas.containsKey(id);
    }

    /** Segundos que faltam para o último efeito desse id acabar (0 se não está ativo). */
    public int segundosRestantes(String id) {
        int ticks = Math.max(ticksRestantes(danos, id), ticksRestantes(defesas, id));
        return (ticks + PontosEsforco.TICKS_POR_SEGUNDO - 1) / PontosEsforco.TICKS_POR_SEGUNDO;
    }

    private static int ticksRestantes(Map<String, ? extends Ativo<?>> ativos, String id) {
        Ativo<?> ativo = ativos.get(id);
        return ativo == null ? 0 : ativo.ticksRestantes;
    }

    /** Bônus de dano total dos efeitos ativos para um tipo de dano (0 se nada afeta). */
    public double bonusDano(TipoDano tipo) {
        double total = 0;
        for (Ativo<EfeitoDano> ativo : danos.values()) {
            if (ativo.efeito.afeta(tipo)) {
                total += ativo.efeito.getBonusDano();
            }
        }
        return total;
    }

    /** Dano final depois dos efeitos ativos. Passe o dano já com os bônus de atributos. */
    public double danoFinal(double dano, TipoDano tipo) {
        return dano + bonusDano(tipo);
    }

    /** Bônus de defesa total dos efeitos ativos (0 se nada afeta). */
    public int bonusDefesa() {
        int total = 0;
        for (Ativo<EfeitoDefesa> ativo : defesas.values()) {
            total += ativo.efeito.getBonusDefesa();
        }
        return total;
    }

    /** Chance total de resistir a efeitos dos efeitos ativos (0.15 = 15%). */
    public double chanceResistirEfeitos() {
        double total = 0;
        for (Ativo<EfeitoDefesa> ativo : defesas.values()) {
            total += ativo.efeito.getChanceResistir();
        }
        return total;
    }
}
