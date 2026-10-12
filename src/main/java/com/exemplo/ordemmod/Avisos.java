package com.exemplo.ordemmod;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

/**
 * Avisos curtos do mod: em vez de escrever no chat, o servidor manda o texto para o cliente, que o mostra embaixo da
 * mira com fade (veja AvisoOrdem). Os comandos /ordem continuam respondendo no chat normalmente.
 */
public final class Avisos {
    private Avisos() {
    }

    public static void enviar(ServerPlayer jogador, String texto) {
        enviar(jogador, texto, null);
    }

    /** {@code cor} pode ser null (cor padrao do aviso). */
    public static void enviar(ServerPlayer jogador, String texto, ChatFormatting cor) {
        Rede.paraJogador(jogador, new PacoteAviso(texto, rgbDe(cor)));
    }

    /**
     * Na 26.2 o ChatFormatting perdeu o getColor() (as cores foram para outra classe do jogo). Como o mod so precisa
     * das 16 cores classicas do chat, a tabela fica aqui, indexada pelo nome da constante. Devolve -1 (cor padrao do
     * aviso) para null, para RESET e para os estilos (negrito, italico etc.).
     */
    static int rgbDe(ChatFormatting cor) {
        if (cor == null) {
            return -1;
        }
        return switch (cor.name()) {
            case "BLACK" -> 0x000000;
            case "DARK_BLUE" -> 0x0000AA;
            case "DARK_GREEN" -> 0x00AA00;
            case "DARK_AQUA" -> 0x00AAAA;
            case "DARK_RED" -> 0xAA0000;
            case "DARK_PURPLE" -> 0xAA00AA;
            case "GOLD" -> 0xFFAA00;
            case "GRAY" -> 0xAAAAAA;
            case "DARK_GRAY" -> 0x555555;
            case "BLUE" -> 0x5555FF;
            case "GREEN" -> 0x55FF55;
            case "AQUA" -> 0x55FFFF;
            case "RED" -> 0xFF5555;
            case "LIGHT_PURPLE" -> 0xFF55FF;
            case "YELLOW" -> 0xFFFF55;
            case "WHITE" -> 0xFFFFFF;
            default -> -1;
        };
    }
}
