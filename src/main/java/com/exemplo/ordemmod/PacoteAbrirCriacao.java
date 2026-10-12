package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor -> cliente: "abra a tela de criacao de personagem".
 * comMarcado = true: a cena do Marcado ainda nao foi vista, abre ela antes da tela de criacao.
 */
public record PacoteAbrirCriacao(boolean comMarcado) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteAbrirCriacao> TYPE = Rede.tipo("abrir_criacao");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteAbrirCriacao> CODEC =
            CodecsOrdem.de(PacoteAbrirCriacao::escrever, PacoteAbrirCriacao::ler);

    public static void escrever(PacoteAbrirCriacao m, FriendlyByteBuf buffer) {
        buffer.writeBoolean(m.comMarcado);
    }

    public static PacoteAbrirCriacao ler(FriendlyByteBuf buffer) {
        return new PacoteAbrirCriacao(buffer.readBoolean());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
