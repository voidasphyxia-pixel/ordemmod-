package com.exemplo.ordemmod;

import java.util.Collection;

/**
 * As regras da criação de personagem, num lugar só. A tela (cliente) usa para mostrar a prévia
 * e o servidor usa para conferir e montar o personagem de verdade. Java puro.
 */
public final class CriacaoPersonagem {
    private CriacaoPersonagem() {
    }

    /**
     * Pontos de atributo que ainda dá para gastar. {@code valores} segue a ordem de {@link Atributo}.
     * Regra oficial: todos começam em 1 e a soma final é sempre 5 + 4 = 9. Reduzir UM atributo a 0
     * rende +1 ponto para gastar em outro (a soma continua 9). Negativo = passou do limite.
     */
    public static int pontosRestantes(int[] valores) {
        int soma = 0;
        for (int v : valores) {
            soma += v;
        }
        int total = Atributos.VALOR_INICIAL * Atributo.values().length + Atributos.PONTOS_PARA_DISTRIBUIR;
        return total - soma;
    }

    /** Monta um personagem com as escolhas, sem conferir nada. Serve para a prévia da tela. */
    public static Personagem construir(ClasseOP classe, int[] valores, Origem origem) {
        Personagem p = new Personagem(classe);
        Atributo[] todos = Atributo.values();
        for (int i = 0; i < todos.length; i++) {
            p.atributos.set(todos[i], valores[i]);
        }
        if (origem != null) {
            p.escolherOrigem(origem);
        }
        p.pe.setMaximo(p.esforcoMaximo());
        p.pe.restaurarTudo();
        return p;
    }

    /**
     * Confere e monta o personagem final. Devolve null se qualquer escolha for inválida
     * (atributos fora da regra, origem/classe ausentes, perícias demais, repetidas ou da origem).
     */
    public static Personagem montar(ClasseOP classe, int[] valores, Origem origem, Collection<Pericia> pericias) {
        if (classe == null || origem == null || valores == null || pericias == null
                || valores.length != Atributo.values().length) {
            return null;
        }
        if (pontosRestantes(valores) != 0) {
            return null; // tem que gastar todos os pontos, sem passar
        }
        Personagem p = construir(classe, valores, origem);
        if (!p.atributos.distribuicaoValida()) {
            return null;
        }
        for (Pericia pericia : pericias) {
            if (pericia == null || !p.pericias.treinar(pericia)) {
                return null; // repetida, já vinha da origem ou sem vagas
            }
        }
        if (p.pericias.vagasRestantes() != 0) {
            return null; // tem que usar todas as vagas
        }
        p.setCriado(true);
        return p;
    }
}
