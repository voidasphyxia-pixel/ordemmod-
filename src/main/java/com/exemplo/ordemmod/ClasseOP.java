package com.exemplo.ordemmod;

/**
 * As classes de Ordem Paranormal.
 * Cada classe define SAN, vida, PE (Pontos de Esforço) e recuperação de PE.
 * Os bônus "por nível" são ganhos a cada marco de NEX (a cada nível).
 */
public enum ClasseOP {
    //            SAN ini, SAN/nív, bônus vida, vida/nív, PE ini, PE/nív, PE recuperado a cada 30s
    COMBATENTE(12, 3, 8, 3, 6, 2, 2),
    ESPECIALISTA(16, 4, 4, 2, 9, 3, 3),
    OCULTISTA(20, 5, 2, 1, 12, 4, 4);

    /** Vida padrão do Minecraft: 20 pontos = 10 corações. */
    public static final int VIDA_PADRAO = 20;

    private final int sanInicial;
    private final int sanPorNivel;
    private final int bonusVida;
    private final int vidaPorNivel;
    private final int peInicial;
    private final int pePorNivel;
    private final int peRecuperado;

    ClasseOP(int sanInicial, int sanPorNivel, int bonusVida, int vidaPorNivel,
             int peInicial, int pePorNivel, int peRecuperado) {
        this.sanInicial = sanInicial;
        this.sanPorNivel = sanPorNivel;
        this.bonusVida = bonusVida;
        this.vidaPorNivel = vidaPorNivel;
        this.peInicial = peInicial;
        this.pePorNivel = pePorNivel;
        this.peRecuperado = peRecuperado;
    }

    /** Sanidade máxima para um nível de NEX (1 = 5% ... 20 = 99%). */
    public int sanidadeMaxima(int nivel) {
        return sanInicial + sanPorNivel * (nivel - 1);
    }

    /**
     * Vida máxima: vida padrão do jogo + bônus da classe + bônus por nível.
     * (2 pontos de vida = 1 coração)
     */
    public int vidaMaxima(int nivel) {
        return VIDA_PADRAO + bonusVida + vidaPorNivel * (nivel - 1);
    }

    /** PE máximo: PE inicial da classe + Presença + ganho por nível de NEX. */
    public int esforcoMaximo(int nivel, int presenca) {
        return peInicial + presenca + pePorNivel * (nivel - 1);
    }

    /** Quanto PE a classe recupera a cada ciclo de 30 segundos. */
    public int esforcoRecuperado() {
        return peRecuperado;
    }
}
