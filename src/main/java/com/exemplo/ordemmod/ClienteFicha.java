package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/**
 * Só existe no cliente: abre (ou atualiza) o menu da ficha. Fica separado para o servidor nunca carregá-lo.
 * Fabric 26.2 (6b): a tela aberta fica em Minecraft.gui (screen() / setScreen), não mais em Minecraft.
 */
public final class ClienteFicha {
    private ClienteFicha() {
    }

    public static void abrir(CompoundTag dados) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.gui.screen() instanceof TelaFicha aberta) {
            aberta.atualizar(dados); // depois de aprender algo: mantém a aba e a rolagem onde estavam
        } else {
            mc.gui.setScreen(new TelaFicha(dados));
        }
    }
}
