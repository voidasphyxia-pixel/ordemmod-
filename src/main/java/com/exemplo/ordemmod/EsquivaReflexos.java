package com.exemplo.ordemmod;

/**
 * A regra da esquiva por Reflexos. Java puro, sem nada do Minecraft aqui.
 *
 * Todo ataque que for atingir o jogador faz um teste de Reflexos contra {@link #dt(int)}. Passou, esquivou do
 * ataque inteiro (sem dano); 20 natural sempre esquiva. Não passou, leva o golpe normalmente.
 *
 * Cercado fica mais difícil: a cada {@link #INIMIGOS_POR_DEGRAU} inimigos te atacando a DT sobe
 * {@link #BONUS_POR_DEGRAU}, até no máximo {@link #BONUS_MAXIMO} (1 inimigo = DT 20; 2-3 = 25; 4-5 = 30...).
 */
public final class EsquivaReflexos {
    /** Dificuldade padrão do teste de esquiva (por enquanto igual para qualquer ataque). */
    public static final int DT_PADRAO = 20;

    /** Quanto a DT sobe a cada degrau de inimigos. */
    public static final int BONUS_POR_DEGRAU = 5;
    /** De quantos em quantos inimigos mirando no jogador a DT sobe um degrau. */
    public static final int INIMIGOS_POR_DEGRAU = 2;
    /** Teto do bônus de DT por estar cercado (DT 20 + 15 = 35: só 20 natural passa para quem tem pouco bônus). */
    public static final int BONUS_MAXIMO = 15;
    /** Raio, em blocos, em que um inimigo mirando no jogador conta como "te cercando". */
    public static final double RAIO_CERCO = 8.0;

    private EsquivaReflexos() {
    }

    /** DT da esquiva com {@code inimigos} atacantes por perto (o próprio atacante conta; menos de 1 vale 1). */
    public static int dt(int inimigos) {
        int degraus = Math.max(inimigos, 1) / INIMIGOS_POR_DEGRAU;
        return DT_PADRAO + Math.min(degraus * BONUS_POR_DEGRAU, BONUS_MAXIMO);
    }

    /** Para onde o personagem dá o passo da esquiva, sempre se afastando do atacante. */
    public enum Passo { TRAS, FRENTE, ESQUERDA, DIREITA }

    /**
     * @param frente quanto o atacante está à frente do jogador (+1 de frente, -1 nas costas)
     * @param lado   quanto o atacante está à direita do jogador (+1 na direita, -1 na esquerda)
     */
    public static Passo escolherPasso(double frente, double lado) {
        if (Math.abs(frente) >= Math.abs(lado)) {
            return frente >= 0 ? Passo.TRAS : Passo.FRENTE;
        }
        return lado >= 0 ? Passo.ESQUERDA : Passo.DIREITA;
    }
}
