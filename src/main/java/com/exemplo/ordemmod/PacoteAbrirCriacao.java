package com.exemplo.ordemmod;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor -> cliente: "abra a tela de criação de personagem".
 * Não carrega dados; só o aviso.
 */
public class PacoteAbrirCriacao {

    /** true = a cena do Marcado ainda não foi vista: abre ela antes da tela de criação. */
    private final boolean comMarcado;

    public PacoteAbrirCriacao(boolean comMarcado) {
        this.comMarcado = comMarcado;
    }

    public static void escrever(PacoteAbrirCriacao mensagem, FriendlyByteBuf buffer) {
        buffer.writeBoolean(mensagem.comMarcado);
    }

    public static PacoteAbrirCriacao ler(FriendlyByteBuf buffer) {
        return new PacoteAbrirCriacao(buffer.readBoolean());
    }

    /** Roda no cliente quando a mensagem chega. */
    public static void tratar(PacoteAbrirCriacao mensagem, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        // DistExecutor garante que o código de tela só é carregado no cliente
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClienteCriacao.abrir(mensagem.comMarcado)));
        ctx.setPacketHandled(true);
    }
}
