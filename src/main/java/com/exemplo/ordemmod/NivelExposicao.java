package com.exemplo.ordemmod;

/**
 * O Nível de Exposição paranormal (NEX) de um personagem.
 * O NEX sobe de 1% em 1% (por exemplo, ao matar criaturas), de 5% até 99%.
 * A cada marco de 5% (5, 10, 15 ... 95) e no 99% o personagem sobe de nível
 * e ganha os bônus de nível. Por dentro, o nível vai de 1 a 20.
 */
public class NivelExposicao {
    public static final int NEX_INICIAL = 5;
    public static final int NEX_MAXIMO = 99;     // 100% não é alcançado em casos normais
    public static final int PASSO_MARCO = 5;
    public static final int NIVEL_MAXIMO = 20;   // NEX 99%

    private int nex = NEX_INICIAL;

    /** NEX em porcentagem (5 a 99). */
    public int getNex() {
        return nex;
    }

    /** Nível de 1 a 20. É esse número que entra nas contas de SAN, PE e vida. */
    public int getNivel() {
        return nex >= NEX_MAXIMO ? NIVEL_MAXIMO : nex / PASSO_MARCO;
    }

    /**
     * Sobe 1% de NEX.
     * Retorna true se esse 1% fez o personagem chegar a um novo marco (subiu de nível),
     * e false se ainda não chegou ou se já está no máximo.
     */
    public boolean ganharUmPorCento() {
        if (nex >= NEX_MAXIMO) {
            return false;
        }
        int nivelAntes = getNivel();
        nex++;
        return getNivel() > nivelAntes;
    }

    /** Quantos % faltam para o próximo marco (0 se já está no máximo). */
    public int faltaParaProximoMarco() {
        if (nex >= NEX_MAXIMO) {
            return 0;
        }
        int proximoMarco = nex >= 95 ? NEX_MAXIMO : (nex / PASSO_MARCO + 1) * PASSO_MARCO;
        return proximoMarco - nex;
    }

    /** Define o NEX direto, mantendo-o entre 5% e 99%. */
    public void setNex(int novoNex) {
        nex = Math.max(NEX_INICIAL, Math.min(NEX_MAXIMO, novoNex));
    }

    @Override
    public String toString() {
        return "NEX " + nex + "%";
    }
}
