package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Só existe no cliente: abre a tela do dado por cima do que estiver aberto. Fica separado para o servidor nunca carregá-lo.
 * Fabric 26.2 (6b): a tela aberta fica em Minecraft.gui (screen() / setScreen).
 */
public final class ClienteTeste {
    private ClienteTeste() {
    }

    public static void abrir(PacoteTeste dados) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        Screen anterior = mc.gui.screen();
        if (anterior instanceof TelaTeste outra) { // outro teste chegou com um ainda na tela: volta para o que estava antes
            anterior = outra.getAnterior();
        }
        mc.gui.setScreen(new TelaTeste(anterior, dados));
    }
}
