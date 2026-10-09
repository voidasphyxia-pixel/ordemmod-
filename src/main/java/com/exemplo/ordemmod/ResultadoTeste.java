package com.exemplo.ordemmod;

import java.util.List;

/**
 * O que saiu num teste de perícia: os dados, o escolhido, o bônus, o total e se passou.
 */
public record ResultadoTeste(Pericia pericia, List<Integer> dados, int escolhido, int bonus,
                             int total, int dt, boolean sucesso, boolean critico) {

    /** Quanto o total passou (ou faltou) em relação à DT. */
    public int margem() {
        return total - dt;
    }

    @Override
    public String toString() {
        String veredito = critico ? "CRÍTICO!" : sucesso ? "SUCESSO" : "FALHA";
        return pericia.getNome() + " " + dados + " → " + escolhido + " + " + bonus
                + " = " + total + " (DT " + dt + "): " + veredito;
    }
}
