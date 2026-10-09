package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente -> servidor: as escolhas feitas na tela de criação.
 * O servidor NUNCA confia no que chega: refaz as contas em {@link CriacaoPersonagem#montar}.
 */
public class PacoteCriarPersonagem {
    private final int classe;
    private final int[] atributos;
    private final int origem;
    private final List<Integer> pericias;

    public PacoteCriarPersonagem(ClasseOP classe, int[] atributos, Origem origem, Iterable<Pericia> pericias) {
        this.classe = classe.ordinal();
        this.atributos = atributos.clone();
        this.origem = origem.ordinal();
        this.pericias = new ArrayList<>();
        for (Pericia p : pericias) {
            this.pericias.add(p.ordinal());
        }
    }

    private PacoteCriarPersonagem(int classe, int[] atributos, int origem, List<Integer> pericias) {
        this.classe = classe;
        this.atributos = atributos;
        this.origem = origem;
        this.pericias = pericias;
    }

    public static void escrever(PacoteCriarPersonagem m, FriendlyByteBuf buffer) {
        buffer.writeByte(m.classe);
        for (int i = 0; i < Atributo.values().length; i++) {
            buffer.writeByte(m.atributos[i]);
        }
        buffer.writeByte(m.origem);
        buffer.writeVarInt(m.pericias.size());
        for (int p : m.pericias) {
            buffer.writeByte(p);
        }
    }

    public static PacoteCriarPersonagem ler(FriendlyByteBuf buffer) {
        int classe = buffer.readByte();
        int[] atributos = new int[Atributo.values().length];
        for (int i = 0; i < atributos.length; i++) {
            atributos[i] = buffer.readByte();
        }
        int origem = buffer.readByte();
        int quantidade = Math.min(buffer.readVarInt(), Pericia.values().length); // limite contra lixo
        List<Integer> pericias = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            pericias.add((int) buffer.readByte());
        }
        return new PacoteCriarPersonagem(classe, atributos, origem, pericias);
    }

    /** Roda no servidor quando a mensagem chega. */
    public static void tratar(PacoteCriarPersonagem m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> {
            ServerPlayer jogador = ctx.getSender();
            if (jogador == null || EventosOrdem.de(jogador).isCriado()) {
                return; // quem já criou o personagem não recria por aqui (use /ordem resetar)
            }
            Personagem novo = CriacaoPersonagem.montar(
                    daLista(ClasseOP.values(), m.classe),
                    m.atributos,
                    daLista(Origem.values(), m.origem),
                    m.pericias.stream().map(i -> daLista(Pericia.values(), i)).toList());
            if (novo == null) {
                Avisos.enviar(jogador, "Criação não aceita. Tente de novo.", net.minecraft.ChatFormatting.RED);
                EventosOrdem.abrirCriacao(jogador);
                return;
            }
            EventosOrdem.definir(jogador, novo);
        });
        ctx.setPacketHandled(true);
    }

    /** Valor do enum pelo índice, ou null se o índice é inválido. */
    private static <T> T daLista(T[] valores, int indice) {
        return indice >= 0 && indice < valores.length ? valores[indice] : null;
    }
}
