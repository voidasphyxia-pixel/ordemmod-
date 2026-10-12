package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor -> cliente: o resultado de um teste de pericia, para a tela do dado tocar a animacao.
 * O servidor ja rolou tudo (ninguem trapaceia); o cliente so mostra.
 *
 * @param pericia   indice da pericia no enum {@link Pericia}
 * @param dados     todos os dados rolados (um por ponto de atributo)
 * @param escolhido o dado que valeu (o maior, ou o menor com atributo 0)
 * @param partes    de onde vem o bonus, uma linha por fonte
 * @param motivo    texto opcional sobre o que esta sendo testado (pode ser vazio)
 */
public record PacoteTeste(int pericia, List<Integer> dados, int escolhido, List<ParteBonus> partes,
                          int total, int dt, boolean sucesso, boolean critico, String motivo)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteTeste> TYPE = Rede.tipo("teste");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteTeste> CODEC =
            CodecsOrdem.de(PacoteTeste::escrever, PacoteTeste::ler);

    public static PacoteTeste de(ResultadoTeste r, List<ParteBonus> partes, String motivo) {
        return new PacoteTeste(r.pericia().ordinal(), r.dados(), r.escolhido(), List.copyOf(partes),
                r.total(), r.dt(), r.sucesso(), r.critico(), motivo == null ? "" : motivo);
    }

    public static void escrever(PacoteTeste m, FriendlyByteBuf buffer) {
        buffer.writeVarInt(m.pericia);
        buffer.writeVarInt(m.dados.size());
        for (int d : m.dados) {
            buffer.writeVarInt(d);
        }
        buffer.writeVarInt(m.escolhido);
        buffer.writeVarInt(m.partes.size());
        for (ParteBonus p : m.partes) {
            buffer.writeUtf(p.rotulo(), 64);
            buffer.writeInt(p.valor());
        }
        buffer.writeInt(m.total);
        buffer.writeVarInt(m.dt);
        buffer.writeBoolean(m.sucesso);
        buffer.writeBoolean(m.critico);
        buffer.writeUtf(m.motivo, 128);
    }

    public static PacoteTeste ler(FriendlyByteBuf buffer) {
        int pericia = buffer.readVarInt();
        int nDados = Math.min(buffer.readVarInt(), 16);
        List<Integer> dados = new ArrayList<>();
        for (int i = 0; i < nDados; i++) {
            dados.add(buffer.readVarInt());
        }
        int escolhido = buffer.readVarInt();
        int nPartes = Math.min(buffer.readVarInt(), 16);
        List<ParteBonus> partes = new ArrayList<>();
        for (int i = 0; i < nPartes; i++) {
            partes.add(new ParteBonus(buffer.readUtf(64), buffer.readInt()));
        }
        int total = buffer.readInt();
        int dt = buffer.readVarInt();
        boolean sucesso = buffer.readBoolean();
        boolean critico = buffer.readBoolean();
        String motivo = buffer.readUtf(128);
        return new PacoteTeste(pericia, List.copyOf(dados), escolhido, List.copyOf(partes), total, dt, sucesso,
                critico, motivo);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
