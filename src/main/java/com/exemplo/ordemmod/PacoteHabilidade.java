package com.exemplo.ordemmod;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * A mensagem que o teclado (cliente) manda para o jogo (servidor):
 * "o jogador apertou a tecla de usar" ou "a tecla de trocar de habilidade".
 */
public class PacoteHabilidade {
    public static final int USAR = 0;
    public static final int TROCAR = 1;
    /** Tecla K: pede ao servidor a ficha do personagem para abrir o menu. */
    public static final int ABRIR_FICHA = 2;

    private final int acao;

    public PacoteHabilidade(int acao) {
        this.acao = acao;
    }

    public static void escrever(PacoteHabilidade mensagem, FriendlyByteBuf buffer) {
        buffer.writeByte(mensagem.acao);
    }

    public static PacoteHabilidade ler(FriendlyByteBuf buffer) {
        return new PacoteHabilidade(buffer.readByte());
    }

    /** Roda no servidor quando a mensagem chega. */
    public static void tratar(PacoteHabilidade mensagem, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> {
            ServerPlayer jogador = ctx.getSender();
            if (jogador == null) {
                return;
            }
            if (mensagem.acao == ABRIR_FICHA) {
                PacoteFicha.enviar(jogador);
            } else {
                EventosOrdem.aoTeclaHabilidade(jogador, mensagem.acao);
            }
        });
        ctx.setPacketHandled(true);
    }
}
