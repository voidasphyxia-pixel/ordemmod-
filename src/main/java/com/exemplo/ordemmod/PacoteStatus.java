package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor -> cliente: os numeros que so o servidor sabe (sanidade, PE e NEX) para a HUD desenhar as barras.
 * A vida, a fome e a armadura o cliente ja conhece sozinho.
 * E um record, entao o {@code equals} vem pronto: o servidor so reenvia quando algum valor muda.
 */
public record PacoteStatus(int sanidade, int sanidadeMax, int pe, int peMax, int nex, int nivel)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteStatus> TYPE = Rede.tipo("status");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteStatus> CODEC =
            CodecsOrdem.de(PacoteStatus::escrever, PacoteStatus::ler);

    public static PacoteStatus de(Personagem p) {
        return new PacoteStatus(p.getSanidade(), p.sanidadeMaxima(), p.pe.getAtual(), p.pe.getMaximo(),
                p.nex.getNex(), p.nivel());
    }

    public static void escrever(PacoteStatus m, FriendlyByteBuf buffer) {
        buffer.writeVarInt(m.sanidade);
        buffer.writeVarInt(m.sanidadeMax);
        buffer.writeVarInt(m.pe);
        buffer.writeVarInt(m.peMax);
        buffer.writeVarInt(m.nex);
        buffer.writeVarInt(m.nivel);
    }

    public static PacoteStatus ler(FriendlyByteBuf buffer) {
        return new PacoteStatus(buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
