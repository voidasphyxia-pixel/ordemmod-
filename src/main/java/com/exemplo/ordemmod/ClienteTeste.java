package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Só existe no cliente: abre a tela do dado por cima do que estiver aberto. Fica separado para o servidor nunca carregá-lo. */
public final class ClienteTeste {
    private ClienteTeste() {
    }

    public static void abrir(PacoteTeste dados) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Screen anterior = mc.screen;
        if (anterior instanceof TelaTeste outra) { // outro teste chegou com um ainda na tela: volta para o que estava antes
            anterior = outra.getAnterior();
        }
        mc.setScreen(new TelaTeste(anterior, dados));
    }
}
