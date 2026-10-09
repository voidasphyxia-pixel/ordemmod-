package com.exemplo.ordemmod;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente -> servidor: "cliquei nesta perícia na ficha, role um teste". O cliente só diz QUAL perícia;
 * quem rola os dados e escolhe a DT é sempre o servidor.
 */
public record PacoteRolarPericia(int pericia) {
    private static final String CHAVE_RECARGA = "ordem_rolar_ate";
    /** Anti-spam: um clique por segundo. */
    private static final int RECARGA_TICKS = 20;

    public static void escrever(PacoteRolarPericia m, FriendlyByteBuf buffer) {
        buffer.writeVarInt(m.pericia);
    }

    public static PacoteRolarPericia ler(FriendlyByteBuf buffer) {
        return new PacoteRolarPericia(buffer.readVarInt());
    }

    /** Roda no servidor quando a mensagem chega. */
    public static void tratar(PacoteRolarPericia m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> {
            ServerPlayer jogador = ctx.getSender();
            if (jogador == null) {
                return;
            }
            Personagem p = EventosOrdem.de(jogador);
            Pericia[] todas = Pericia.values();
            if (!p.isCriado() || m.pericia < 0 || m.pericia >= todas.length) {
                return;
            }
            long agora = jogador.level().getGameTime();
            if (agora < jogador.getPersistentData().getLong(CHAVE_RECARGA)) {
                return;
            }
            jogador.getPersistentData().putLong(CHAVE_RECARGA, agora + RECARGA_TICKS);
            // clique na ficha não tem situação própria: usa a DT média. Outros sistemas chamam TesteNaTela com a DT deles.
            TesteNaTela.rolar(jogador, todas[m.pericia], Pericias.DT_MEDIA, null);
        });
        ctx.setPacketHandled(true);
    }
}
