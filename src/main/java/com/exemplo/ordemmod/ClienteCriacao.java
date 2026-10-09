package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;

/** Só existe no cliente: abre a tela de criação. Fica separado para o servidor nunca carregá-lo. */
public final class ClienteCriacao {
    private ClienteCriacao() {
    }

    public static void abrir(boolean comMarcado) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.setScreen(comMarcado ? new com.exemplo.ordemmod.marcado.TelaMarcado() : new TelaCriacao());
        }
    }
}
