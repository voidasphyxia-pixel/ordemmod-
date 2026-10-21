package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente -> servidor: o que o jogador clicou na aba "Arvore" da ficha.
 * - APRENDER: gasta 1 ponto de habilidade numa habilidade de classe (pelo id);
 * - TRILHA: escolhe a trilha (pelo indice no enum {@link Trilha}); nao da para trocar depois;
 * - MELHORAR: gasta uma melhoria de grau (2 + Intelecto nos NEX 35% e 70%) numa pericia (pelo indice em {@link Pericia});
 * - TREINAMENTO: poder Treinamento em Pericia: escolhe duas pericias (pelos indices) e sobe cada uma um grau;
 * - ATRIBUTO: gasta 1 ponto de atributo (NEX 20%, 50%, 80% e 95%) num atributo (pelo indice em {@link Atributo}), ate 5;
 * - EQUIPAR: poder que escalona por NEX; escolhe qual versao fica equipada (muda o gasto de PE). O id e o da primeira versao
 *   e o indice e a posicao da versao na cadeia.
 * O servidor NUNCA confia no que chega: confere tudo de novo antes de mexer no personagem (veja ReceptoresServidor).
 *
 * @param id      id da habilidade (so na acao APRENDER)
 * @param indice  indice da trilha (TRILHA), da pericia (MELHORAR e TREINAMENTO) ou do atributo (ATRIBUTO)
 * @param indice2 segunda pericia (so na acao TREINAMENTO)
 */
public record PacoteArvore(int acao, String id, int indice, int indice2) implements CustomPacketPayload {
    public static final int APRENDER = 0;
    public static final int TRILHA = 1;
    public static final int MELHORAR = 2;
    public static final int TREINAMENTO = 3;
    public static final int ATRIBUTO = 4;
    public static final int EQUIPAR = 5;

    public static final CustomPacketPayload.Type<PacoteArvore> TYPE = Rede.tipo("arvore");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteArvore> CODEC =
            CodecsOrdem.de(PacoteArvore::escrever, PacoteArvore::ler);

    public static PacoteArvore aprender(String idDaHabilidade) {
        return new PacoteArvore(APRENDER, idDaHabilidade, -1, -1);
    }

    public static PacoteArvore escolherTrilha(Trilha trilha) {
        return new PacoteArvore(TRILHA, "", trilha.ordinal(), -1);
    }

    public static PacoteArvore melhorarPericia(Pericia pericia) {
        return new PacoteArvore(MELHORAR, "", pericia.ordinal(), -1);
    }

    public static PacoteArvore treinamento(Pericia primeira, Pericia segunda) {
        return new PacoteArvore(TREINAMENTO, "", primeira.ordinal(), segunda.ordinal());
    }

    public static PacoteArvore subirAtributo(Atributo atributo) {
        return new PacoteArvore(ATRIBUTO, "", atributo.ordinal(), -1);
    }

    /** Poder que escalona por NEX: equipa a versao de posicao {@code versao} (0 = a primeira) da cadeia de {@code idDaRaiz}. */
    public static PacoteArvore equipar(String idDaRaiz, int versao) {
        return new PacoteArvore(EQUIPAR, idDaRaiz, versao, -1);
    }

    public static void escrever(PacoteArvore m, FriendlyByteBuf buffer) {
        buffer.writeByte(m.acao);
        buffer.writeUtf(m.id, 64);
        buffer.writeByte(m.indice);
        buffer.writeByte(m.indice2);
    }

    public static PacoteArvore ler(FriendlyByteBuf buffer) {
        int acao = buffer.readByte();
        String id = buffer.readUtf(64);
        int indice = buffer.readByte();
        int indice2 = buffer.readByte();
        return new PacoteArvore(acao, id, indice, indice2);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
