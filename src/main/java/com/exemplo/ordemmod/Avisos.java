package com.exemplo.ordemmod;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

/**
 * Avisos curtos do mod: em vez de escrever no chat, o servidor manda o texto para o cliente, que o mostra embaixo da
 * mira com fade (veja {@link AvisoOrdem}). Os comandos /ordem continuam respondendo no chat normalmente.
 */
public final class Avisos {
    private Avisos() {
    }

    public static void enviar(ServerPlayer jogador, String texto) {
        enviar(jogador, texto, null);
    }

    /** {@code cor} pode ser null (cor padrão do aviso). */
    public static void enviar(ServerPlayer jogador, String texto, ChatFormatting cor) {
        Integer rgb = cor == null ? null : cor.getColor();
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador),
                new PacoteAviso(texto, rgb == null ? -1 : rgb));
    }
}
