package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente -> servidor: o jogador apertou a tecla de usar, a de trocar de habilidade ou a da ficha.
 * O tratamento no servidor fica em ReceptoresServidor.
 */
public record PacoteHabilidade(int acao) implements CustomPacketPayload {
    public static final int USAR = 0;
    public static final int TROCAR = 1;
    /** Tecla K: pede ao servidor a ficha do personagem para abrir o menu. */
    public static final int ABRIR_FICHA = 2;

    public static final CustomPacketPayload.Type<PacoteHabilidade> TYPE = Rede.tipo("habilidade");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteHabilidade> CODEC =
            CodecsOrdem.de(PacoteHabilidade::escrever, PacoteHabilidade::ler);

    public static void escrever(PacoteHabilidade m, FriendlyByteBuf buffer) {
        buffer.writeByte(m.acao);
    }

    public static PacoteHabilidade ler(FriendlyByteBuf buffer) {
        return new PacoteHabilidade(buffer.readByte());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
