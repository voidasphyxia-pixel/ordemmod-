package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor -> cliente: os numeros que so o servidor sabe (sanidade, PE e NEX) para a HUD desenhar as barras, mais a
 * fila de habilidades ativas (para os pentagonos do canto inferior esquerdo).
 * A vida, a fome e a armadura o cliente ja conhece sozinho.
 * E um record, entao o {@code equals} vem pronto: o servidor so reenvia quando algum valor muda.
 */
public record PacoteStatus(int sanidade, int sanidadeMax, int pe, int peMax, int nex, int nivel,
        List<Slot> habilidades, int selecionada) implements CustomPacketPayload {

    /** Uma habilidade ativa na fila da HUD: id (da melhor versao, e o nome do icone), nome para exibir e custo em PE. */
    public record Slot(String id, String nome, int custo) {
    }

    public static final CustomPacketPayload.Type<PacoteStatus> TYPE = Rede.tipo("status");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteStatus> CODEC =
            CodecsOrdem.de(PacoteStatus::escrever, PacoteStatus::ler);

    public static PacoteStatus de(Personagem p) {
        List<Slot> fila = new ArrayList<>();
        for (Habilidade h : p.habilidades.getAtivas()) {
            Habilidade efetiva = p.habilidades.melhorVersao(h);
            fila.add(new Slot(efetiva.getId(), efetiva.getNome(), efetiva.getCustoPe()));
        }
        return new PacoteStatus(p.getSanidade(), p.sanidadeMaxima(), p.pe.getAtual(), p.pe.getMaximo(),
                p.nex.getNex(), p.nivel(), List.copyOf(fila), p.indiceSelecionada());
    }

    public static void escrever(PacoteStatus m, FriendlyByteBuf buffer) {
        buffer.writeVarInt(m.sanidade);
        buffer.writeVarInt(m.sanidadeMax);
        buffer.writeVarInt(m.pe);
        buffer.writeVarInt(m.peMax);
        buffer.writeVarInt(m.nex);
        buffer.writeVarInt(m.nivel);
        buffer.writeVarInt(m.habilidades.size());
        for (Slot s : m.habilidades) {
            buffer.writeUtf(s.id());
            buffer.writeUtf(s.nome());
            buffer.writeVarInt(s.custo());
        }
        buffer.writeVarInt(m.selecionada);
    }

    public static PacoteStatus ler(FriendlyByteBuf buffer) {
        int sanidade = buffer.readVarInt();
        int sanidadeMax = buffer.readVarInt();
        int pe = buffer.readVarInt();
        int peMax = buffer.readVarInt();
        int nex = buffer.readVarInt();
        int nivel = buffer.readVarInt();
        int n = buffer.readVarInt();
        List<Slot> fila = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            fila.add(new Slot(buffer.readUtf(), buffer.readUtf(), buffer.readVarInt()));
        }
        int selecionada = buffer.readVarInt();
        return new PacoteStatus(sanidade, sanidadeMax, pe, peMax, nex, nivel, List.copyOf(fila), selecionada);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
