package com.exemplo.ordemmod;

/**
 * Grau de treinamento de uma perícia. O bônus entra somando no teste (d20 + bônus).
 */
public enum GrauTreinamento {
    DESTREINADO("Destreinado", 0),
    TREINADO("Treinado", 5),
    VETERANO("Veterano", 10),
    EXPERT("Expert", 15);

    private final String nome;
    private final int bonus;

    GrauTreinamento(String nome, int bonus) {
        this.nome = nome;
        this.bonus = bonus;
    }

    public String getNome() {
        return nome;
    }

    public int getBonus() {
        return bonus;
    }

    /** Próximo grau, ou null se já é Expert. */
    public GrauTreinamento proximo() {
        return this == EXPERT ? null : values()[ordinal() + 1];
    }

    public boolean pelomenos(GrauTreinamento outro) {
        return ordinal() >= outro.ordinal();
    }
}
