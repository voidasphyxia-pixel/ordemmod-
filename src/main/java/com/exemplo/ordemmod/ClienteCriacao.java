package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;

/**
 * Só existe no cliente: abre a tela de criação. Fica separado para o servidor nunca carregá-lo.
 *
 * Fabric 26.2 (6c): abre a TelaCriacao. A cena do Marcado (TelaMarcado, que no Forge vinha ANTES da criação quando
 * comMarcado = true) ainda não foi portada (etapa 6d); até lá, nos dois casos abre-se direto a TelaCriacao. Na 6d volta a ser
 * {@code comMarcado ? new TelaMarcado() : new TelaCriacao()}.
 */
public final class ClienteCriacao {
    private ClienteCriacao() {
    }

    public static void abrir(boolean comMarcado) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            if (comMarcado) {
                System.out.println("[Ordem cliente] cena do Marcado ainda nao portada (etapa 6d): abrindo direto a criacao");
            }
            mc.gui.setScreen(new TelaCriacao());
        }
    }
}
