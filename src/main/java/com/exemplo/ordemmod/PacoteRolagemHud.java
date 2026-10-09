package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

/**
 * Servidor -> cliente: uma rolagem para aparecer num painel pequeno logo acima da barra de sanidade (veja {@link RolagemHud}).
 * O servidor já rolou e já aplicou o efeito; o cliente só mostra, por estética.
 *
 * @param pericia   índice da perícia no enum {@link Pericia}
 * @param dados     todos os dados rolados
 * @param escolhido o dado que valeu
 * @param bonus     soma dos bônus
 * @param reducao   quanto do dano foi reduzido, de 0 a 100 (-1 = não mostrar nada sobre dano)
 * @param motivo    texto curto do que está sendo testado (ex.: "Queda")
 */
public record PacoteRolagemHud(int pericia, List<Integer> dados, int escolhido, int bonus, int total, int dt,
                               boolean sucesso, boolean critico, int reducao, String motivo) {

    public static PacoteRolagemHud de(ResultadoTeste r, int reducao, String motivo) {
        return new PacoteRolagemHud(r.pericia().ordinal(), List.copyOf(r.dados()), r.escolhido(), r.bonus(), r.total(),
                r.dt(), r.sucesso(), r.critico(), reducao, motivo == null ? "" : motivo);
    }

    public static void escrever(PacoteRolagemHud m, FriendlyByteBuf buffer) {
        buffer.writeVarInt(m.pericia);
        buffer.writeVarInt(m.dados.size());
        for (int d : m.dados) {
            buffer.writeVarInt(d);
        }
        buffer.writeVarInt(m.escolhido);
        buffer.writeInt(m.bonus);
        buffer.writeInt(m.total);
        buffer.writeVarInt(m.dt);
        buffer.writeBoolean(m.sucesso);
        buffer.writeBoolean(m.critico);
        buffer.writeInt(m.reducao);
        buffer.writeUtf(m.motivo, 64);
    }

    public static PacoteRolagemHud ler(FriendlyByteBuf buffer) {
        int pericia = buffer.readVarInt();
        int nDados = Math.min(buffer.readVarInt(), 16);
        List<Integer> dados = new ArrayList<>();
        for (int i = 0; i < nDados; i++) {
            dados.add(buffer.readVarInt());
        }
        int escolhido = buffer.readVarInt();
        int bonus = buffer.readInt();
        int total = buffer.readInt();
        int dt = buffer.readVarInt();
        boolean sucesso = buffer.readBoolean();
        boolean critico = buffer.readBoolean();
        int reducao = buffer.readInt();
        String motivo = buffer.readUtf(64);
        return new PacoteRolagemHud(pericia, List.copyOf(dados), escolhido, bonus, total, dt, sucesso, critico, reducao,
                motivo);
    }

    /** Roda no cliente quando a mensagem chega. */
    public static void tratar(PacoteRolagemHud m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> RolagemHud.receber(m)));
        ctx.setPacketHandled(true);
    }
}
