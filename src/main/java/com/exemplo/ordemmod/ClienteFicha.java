package com.exemplo.ordemmod;

import net.minecraft.nbt.CompoundTag;

/**
 * PROVISORIO (etapa 6a): so escreve no log. Na 6b isto volta a abrir/atualizar a TelaFicha, como no Forge.
 * So existe no cliente.
 */
public final class ClienteFicha {
    private ClienteFicha() {
    }

    public static void abrir(CompoundTag dados) {
        System.out.println("[Ordem cliente] ficha recebida (" + dados.size() + " campos) - a tela ainda nao foi portada (etapa 6b)");
    }
}
