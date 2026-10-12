package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente -> servidor: as escolhas feitas na tela de criacao.
 * O servidor NUNCA confia no que chega: refaz as contas em CriacaoPersonagem#montar (veja ReceptoresServidor).
 */
public class PacoteCriarPersonagem implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteCriarPersonagem> TYPE = Rede.tipo("criar_personagem");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteCriarPersonagem> CODEC =
            CodecsOrdem.de(PacoteCriarPersonagem::escrever, PacoteCriarPersonagem::ler);

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

    public int classe() {
        return classe;
    }

    public int[] atributos() {
        return atributos;
    }

    public int origem() {
        return origem;
    }

    public List<Integer> pericias() {
        return pericias;
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

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
