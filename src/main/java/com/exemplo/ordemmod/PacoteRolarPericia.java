package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente -> servidor: "cliquei nesta pericia na ficha, role um teste". O cliente so diz QUAL pericia;
 * quem rola os dados e escolhe a DT e sempre o servidor (veja ReceptoresServidor).
 */
public record PacoteRolarPericia(int pericia) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteRolarPericia> TYPE = Rede.tipo("rolar_pericia");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteRolarPericia> CODEC =
            CodecsOrdem.de(PacoteRolarPericia::escrever, PacoteRolarPericia::ler);

    public static void escrever(PacoteRolarPericia m, FriendlyByteBuf buffer) {
        buffer.writeVarInt(m.pericia);
    }

    public static PacoteRolarPericia ler(FriendlyByteBuf buffer) {
        return new PacoteRolarPericia(buffer.readVarInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
