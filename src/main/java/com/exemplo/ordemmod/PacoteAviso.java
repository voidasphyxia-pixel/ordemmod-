package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Servidor -> cliente: um texto curto para aparecer embaixo da mira. {@code cor} e RGB, ou -1 para a cor padrao. */
public record PacoteAviso(String texto, int cor) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteAviso> TYPE = Rede.tipo("aviso");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteAviso> CODEC =
            CodecsOrdem.de(PacoteAviso::escrever, PacoteAviso::ler);

    public static void escrever(PacoteAviso m, FriendlyByteBuf buffer) {
        buffer.writeUtf(m.texto, 256);
        buffer.writeInt(m.cor);
    }

    public static PacoteAviso ler(FriendlyByteBuf buffer) {
        return new PacoteAviso(buffer.readUtf(256), buffer.readInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
