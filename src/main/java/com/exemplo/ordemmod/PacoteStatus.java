package com.exemplo.ordemmod;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor -> cliente: os números que só o servidor sabe (sanidade, PE e NEX) para a HUD desenhar as barras.
 * A vida, a fome e a armadura o cliente já conhece sozinho.
 * É um record, então o {@code equals} vem pronto: o servidor só reenvia quando algum valor muda.
 */
public record PacoteStatus(int sanidade, int sanidadeMax, int pe, int peMax, int nex, int nivel) {

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

    /** Roda no cliente quando a mensagem chega. */
    public static void tratar(PacoteStatus m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> HudOrdem.receber(m)));
        ctx.setPacketHandled(true);
    }
}
