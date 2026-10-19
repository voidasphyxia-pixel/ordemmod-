package com.exemplo.ordemmod;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * As teclas do mod. Só existe no cliente (no computador do jogador).
 *
 * Fabric 26.2: no Forge o RegisterKeyMappingsEvent registrava as teclas e um TickEvent.ClientTickEvent as lia.
 * Aqui {@link KeyMappingHelper#registerKeyMapping} registra (e cria a categoria nos Controles) e um
 * ClientTickEvents.END_CLIENT_TICK lê. A tradução da categoria é "key.category.ordemmod.principal" (veja o lang).
 */
public final class TeclasOrdem {
    private static final KeyMapping.Category CATEGORIA = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "principal"));

    public static final KeyMapping USAR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.ordemmod.usar_habilidade", InputConstants.Type.KEYSYM, InputConstants.KEY_R, CATEGORIA));
    public static final KeyMapping TROCAR = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.ordemmod.trocar_habilidade", InputConstants.Type.KEYSYM, InputConstants.KEY_G, CATEGORIA));
    public static final KeyMapping MENU = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.ordemmod.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_K, CATEGORIA));

    private TeclasOrdem() {
    }

    /** Chamar do entrypoint do cliente: o carregamento da classe registra as teclas, e aqui liga a leitura. */
    public static void registrar() {
        ClientTickEvents.END_CLIENT_TICK.register(TeclasOrdem::aoTick);
    }

    /** A cada tick do cliente, vê se a tecla foi apertada e manda a mensagem ao servidor. */
    private static void aoTick(Minecraft cliente) {
        if (cliente.player == null) {
            return;
        }
        while (USAR.consumeClick()) {
            HabilidadesHud.aoApertarUsar(); // anima o slot: uso ou "sem PE"
            ReceptoresCliente.enviar(new PacoteHabilidade(PacoteHabilidade.USAR));
        }
        while (TROCAR.consumeClick()) {
            ReceptoresCliente.enviar(new PacoteHabilidade(PacoteHabilidade.TROCAR));
        }
        while (MENU.consumeClick()) {
            if (cliente.gui.screen() == null) { // 26.2: a tela aberta agora fica em Gui, nao em Minecraft
                ReceptoresCliente.enviar(new PacoteHabilidade(PacoteHabilidade.ABRIR_FICHA));
            }
        }
    }
}
