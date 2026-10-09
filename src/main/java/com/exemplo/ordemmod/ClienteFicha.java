package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Só existe no cliente: abre o menu da ficha. Fica separado para o servidor nunca carregá-lo. */
public final class ClienteFicha {
    private ClienteFicha() {
    }

    public static void abrir(CompoundTag dados) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        if (mc.screen instanceof TelaFicha aberta) {
            aberta.atualizar(dados); // depois de aprender algo: mantém a aba e a rolagem onde estavam
        } else {
            mc.setScreen(new TelaFicha(dados));
        }
    }
}
