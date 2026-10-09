package com.exemplo.ordemmod;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/** Servidor -> cliente: um texto curto para aparecer embaixo da mira. {@code cor} é RGB, ou -1 para a cor padrão. */
public record PacoteAviso(String texto, int cor) {

    public static void escrever(PacoteAviso m, FriendlyByteBuf buffer) {
        buffer.writeUtf(m.texto, 256);
        buffer.writeInt(m.cor);
    }

    public static PacoteAviso ler(FriendlyByteBuf buffer) {
        return new PacoteAviso(buffer.readUtf(256), buffer.readInt());
    }

    /** Roda no cliente quando a mensagem chega. */
    public static void tratar(PacoteAviso m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> AvisoOrdem.receber(m.texto(), m.cor())));
        ctx.setPacketHandled(true);
    }
}
