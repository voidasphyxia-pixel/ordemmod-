package com.exemplo.ordemmod;

/**
 * Proficiências que uma habilidade pode conceder (permissão para usar certos equipamentos).
 */
public enum Proficiencia {
    ARMAS_PESADAS("armas pesadas"),
    PROTECOES_PESADAS("proteções pesadas");

    private final String nome;

    Proficiencia(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }
}
