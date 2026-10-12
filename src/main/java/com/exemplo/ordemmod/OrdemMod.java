package com.exemplo.ordemmod;

import net.fabricmc.api.ModInitializer;

/**
 * Ponto de entrada do mod no Fabric (26.2). No Forge era o @Mod + event bus.
 *
 * Ainda NAO registra entidades, sons, rede nem eventos: isso volta aqui conforme cada
 * parte for portada (veja PORTE-FABRIC.md).
 */
public class OrdemMod implements ModInitializer {
    public static final String MOD_ID = "ordemmod";

    @Override
    public void onInitialize() {
        // Teste de fumaca do nucleo (pode apagar depois): se aparecer no log, o Java puro carregou.
        Personagem teste = new Personagem(ClasseOP.COMBATENTE);
        System.out.println("Ordem Mod carregado no Fabric! Nucleo OK (Combatente NEX " + teste.nex.getNex()
                + "%, vida maxima " + teste.vidaMaxima() + ").");
    }
}
