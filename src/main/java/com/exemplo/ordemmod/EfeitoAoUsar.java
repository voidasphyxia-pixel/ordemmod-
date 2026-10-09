package com.exemplo.ordemmod;

/**
 * Algo que acontece quando uma habilidade ATIVA é usada (depois de gastar o PE).
 * Cada tipo de efeito é uma pecinha pequena (record); a Habilidade guarda uma lista delas.
 */
@FunctionalInterface
public interface EfeitoAoUsar {
    /**
     * Aplica o efeito. O id é o da habilidade (primeira versão da cadeia),
     * então usar de novo só reinicia o tempo.
     */
    void aplicar(String id, Atributos atributos, EfeitosAtivos efeitos);

    /** Ativa um efeito temporário de dano. */
    record Dano(EfeitoDano efeito) implements EfeitoAoUsar {
        @Override
        public void aplicar(String id, Atributos atributos, EfeitosAtivos efeitos) {
            efeitos.ativar(id, efeito);
        }
    }

    /**
     * Ativa um efeito temporário de defesa que depende de um atributo.
     * Os valores usam os atributos do momento em que a habilidade é usada.
     * resistenciaPorPonto: 0.05 = 5% por ponto do atributo.
     */
    record DefesaPorAtributo(Atributo atributo, double defesaPorPonto,
                             double resistenciaPorPonto, int duracaoSegundos) implements EfeitoAoUsar {
        @Override
        public void aplicar(String id, Atributos atributos, EfeitosAtivos efeitos) {
            int valor = atributos.get(atributo);
            efeitos.ativar(id, new EfeitoDefesa((int) Math.round(defesaPorPonto * valor),
                    resistenciaPorPonto * valor, duracaoSegundos));
        }
    }
}
