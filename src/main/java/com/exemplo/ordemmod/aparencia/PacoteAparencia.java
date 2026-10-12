package com.exemplo.ordemmod.aparencia;

import java.util.UUID;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Vai nos dois sentidos:
 * cliente -> servidor: "esta é a minha aparência" (o servidor usa o remetente, o UUID do pacote é ignorado);
 * servidor -> cliente: "este jogador tem esta aparência".
 * {@code dados} nulo significa "sem aparência personalizada" (volta à skin normal).
 */
public record PacoteAparencia(UUID jogador, AparenciaData dados) {

    public static void escrever(PacoteAparencia m, FriendlyByteBuf buffer) {
        buffer.writeUUID(m.jogador);
        buffer.writeBoolean(m.dados != null);
        if (m.dados != null) {
            m.dados.escrever(buffer);
        }
    }

    public static PacoteAparencia ler(FriendlyByteBuf buffer) {
        UUID jogador = buffer.readUUID();
        AparenciaData dados = buffer.readBoolean() ? AparenciaData.ler(buffer) : null;
        return new PacoteAparencia(jogador, dados);
    }

    public static void tratar(PacoteAparencia m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ServerPlayer remetente = ctx.getSender();
        if (remetente != null) {
            // chegou no servidor
            ctx.enqueueWork(() -> AparenciaServidor.definir(remetente, m.dados));
        } else {
            // chegou no cliente
            ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> AparenciaCliente.receber(m.jogador, m.dados)));
        }
        ctx.setPacketHandled(true);
    }
}
