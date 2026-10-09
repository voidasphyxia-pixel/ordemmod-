package com.exemplo.ordemmod;

/**
 * Guarda os Pontos de Esforço (PE) de um personagem: o máximo, o valor atual
 * e o cooldown da recuperação automática.
 */
public class PontosEsforco {
    /** O Minecraft roda 20 ticks por segundo, então 30 segundos = 600 ticks. */
    public static final int TICKS_POR_SEGUNDO = 20;
    public static final int COOLDOWN_SEGUNDOS = 30;
    public static final int COOLDOWN_TICKS = COOLDOWN_SEGUNDOS * TICKS_POR_SEGUNDO;

    private int maximo;
    private int atual;
    private int ticksDecorridos = 0;

    /** O personagem começa com o PE cheio. */
    public PontosEsforco(int maximo) {
        this.maximo = maximo;
        this.atual = maximo;
    }

    public int getAtual() {
        return atual;
    }

    public int getMaximo() {
        return maximo;
    }

    /** Muda o máximo (ex.: ao subir de NEX). Se o atual passar do novo máximo, ele é cortado. */
    public void setMaximo(int novoMaximo) {
        maximo = Math.max(0, novoMaximo);
        if (atual > maximo) {
            atual = maximo;
        }
    }

    /** Tenta gastar PE. Retorna false, sem gastar nada, se não houver PE suficiente. */
    public boolean gastar(int quantidade) {
        if (quantidade < 0 || quantidade > atual) {
            return false;
        }
        atual -= quantidade;
        return true;
    }

    /** Recupera PE, sem passar do máximo. */
    public void recuperar(int quantidade) {
        atual = Math.min(maximo, atual + Math.max(0, quantidade));
    }

    public void restaurarTudo() {
        atual = maximo;
    }

    /**
     * Chamado a cada tick do jogo (20 vezes por segundo).
     * Enquanto o PE não está cheio, conta o cooldown; ao completar 30 segundos,
     * recupera a quantidade da classe e reinicia a contagem.
     */
    public void tick(ClasseOP classe) {
        if (atual >= maximo) {
            ticksDecorridos = 0;
            return;
        }
        ticksDecorridos++;
        if (ticksDecorridos >= COOLDOWN_TICKS) {
            recuperar(classe.esforcoRecuperado());
            ticksDecorridos = 0;
        }
    }
}
