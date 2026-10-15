package com.exemplo.ordemmod;

import com.exemplo.ordemmod.marcado.TelaMarcado;

import net.minecraft.client.Minecraft;

/**
 * Só existe no cliente: abre a tela de criação. Fica separado para o servidor nunca carregá-lo.
 *
 * Fabric 26.2 (6d): como no Forge, quando comMarcado = true abre ANTES a cena do Marcado (TelaMarcado), que no fim manda as
 * escolhas ao servidor e abre a TelaCriacao; senão abre direto a TelaCriacao.
 */
public final class ClienteCriacao {
    private ClienteCriacao() {
    }

    public static void abrir(boolean comMarcado) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.gui.setScreen(comMarcado ? new TelaMarcado() : new TelaCriacao());
        }
    }
}
