package com.exemplo.ordemmod;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente -> servidor: o que o jogador clicou na aba "Árvore" da ficha.
 * - APRENDER: gasta 1 ponto de habilidade numa habilidade de classe (pelo id);
 * - TRILHA: escolhe a trilha (pelo índice no enum {@link Trilha}); não dá para trocar depois;
 * - MELHORAR: gasta uma melhoria de grau (2 + Intelecto nos NEX 35% e 70%) numa perícia (pelo índice em {@link Pericia});
 * - TREINAMENTO: poder Treinamento em Perícia: escolhe duas perícias (pelos índices) e sobe cada uma um grau.
 * O servidor NUNCA confia no que chega: confere tudo de novo antes de mexer no personagem.
 */
public class PacoteArvore {
    public static final int APRENDER = 0;
    public static final int TRILHA = 1;
    public static final int MELHORAR = 2;
    public static final int TREINAMENTO = 3;

    private final int acao;
    private final String id;    // id da habilidade (só na ação APRENDER)
    private final int indice;   // índice da trilha (TRILHA) ou da perícia (MELHORAR e TREINAMENTO)
    private final int indice2;  // segunda perícia (só na ação TREINAMENTO)

    private PacoteArvore(int acao, String id, int indice, int indice2) {
        this.acao = acao;
        this.id = id;
        this.indice = indice;
        this.indice2 = indice2;
    }

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

    /** Roda no servidor quando a mensagem chega. */
    public static void tratar(PacoteArvore m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> {
            ServerPlayer jogador = ctx.getSender();
            if (jogador == null) {
                return;
            }
            Personagem p = EventosOrdem.de(jogador);
            if (!p.isCriado()) {
                return;
            }
            if (m.acao == APRENDER) {
                aprender(jogador, p, m.id);
            } else if (m.acao == TRILHA) {
                escolherTrilha(jogador, p, m.indice);
            } else if (m.acao == MELHORAR) {
                melhorarPericia(jogador, p, m.indice);
            } else if (m.acao == TREINAMENTO) {
                treinamento(jogador, p, m.indice, m.indice2);
            }
            PacoteFicha.enviar(jogador); // manda a ficha atualizada: a tela aberta se redesenha sozinha
        });
        ctx.setPacketHandled(true);
    }

    private static void aprender(ServerPlayer jogador, Personagem p, String id) {
        Habilidade h = CatalogoHabilidades.porId(id);
        if (h == null) {
            Avisos.enviar(jogador, "Habilidade não encontrada.", ChatFormatting.RED);
            return;
        }
        String motivo = p.habilidades.motivoBloqueio(h, p.nivel());
        if (motivo != null || !p.habilidades.aprender(h, p.nivel())) {
            Avisos.enviar(jogador, motivo != null ? motivo + "." : "Não dá para aprender agora.",
                    ChatFormatting.RED);
            return;
        }
        EventosOrdem.aplicar(jogador, p);
        SalvamentoOrdem.salvar(jogador, p);
        Avisos.enviar(jogador, "Habilidade aprendida: " + h.getNome(), ChatFormatting.GREEN);
    }

    private static Pericia periciaDe(int indice) {
        Pericia[] todas = Pericia.values();
        return indice >= 0 && indice < todas.length ? todas[indice] : null;
    }

    private static void melhorarPericia(ServerPlayer jogador, Personagem p, int indice) {
        Pericia pericia = periciaDe(indice);
        if (pericia == null || !p.pericias.melhorar(pericia)) {
            Avisos.enviar(jogador, "Não dá para melhorar essa perícia agora.", ChatFormatting.RED);
            return;
        }
        EventosOrdem.aplicar(jogador, p);
        SalvamentoOrdem.salvar(jogador, p);
        GrauTreinamento grau = p.pericias.getGrau(pericia);
        Avisos.enviar(jogador, pericia.getNome() + " agora é " + grau.getNome() + " (+" + grau.getBonus() + ")",
                ChatFormatting.GREEN);
    }

    /** Treinamento em Perícia: gasta 1 ponto de habilidade e sobe duas perícias diferentes um grau cada. */
    private static void treinamento(ServerPlayer jogador, Personagem p, int indiceA, int indiceB) {
        Habilidade poder = CatalogoHabilidades.porId(CatalogoHabilidades.ID_TREINAMENTO_PERICIA);
        Pericia a = periciaDe(indiceA);
        Pericia b = periciaDe(indiceB);
        int nivel = p.nivel();
        if (poder == null || a == null || b == null || a == b
                || !p.pericias.podeSubirPorPoder(a, nivel) || !p.pericias.podeSubirPorPoder(b, nivel)) {
            Avisos.enviar(jogador, "Escolha duas perícias diferentes que possam subir de grau.", ChatFormatting.RED);
            return;
        }
        String motivo = p.habilidades.motivoBloqueio(poder, nivel);
        if (motivo != null || !p.habilidades.aprender(poder, nivel)) {
            Avisos.enviar(jogador, motivo != null ? motivo + "." : "Não dá para aprender agora.", ChatFormatting.RED);
            return;
        }
        p.pericias.subirPorPoder(a, nivel);
        p.pericias.subirPorPoder(b, nivel);
        EventosOrdem.aplicar(jogador, p);
        SalvamentoOrdem.salvar(jogador, p);
        Avisos.enviar(jogador, a.getNome() + " e " + b.getNome() + " subiram de grau", ChatFormatting.GREEN);
    }

    private static void escolherTrilha(ServerPlayer jogador, Personagem p, int indice) {
        Trilha[] todas = Trilha.values();
        if (indice < 0 || indice >= todas.length) {
            return;
        }
        Trilha alvo = todas[indice];
        if (!p.habilidades.podeEscolherTrilha(alvo, p.nivel())) {
            Avisos.enviar(jogador, "Não dá para escolher essa trilha.", ChatFormatting.RED);
            return;
        }
        List<Habilidade> antes = p.habilidades.getAprendidas();
        p.habilidades.escolherTrilha(alvo, p.nivel());
        EventosOrdem.aplicar(jogador, p);
        SalvamentoOrdem.salvar(jogador, p);
        int liberadas = 0;
        for (Habilidade h : p.habilidades.getAprendidas()) {
            if (!antes.contains(h)) {
                liberadas++;
            }
        }
        Avisos.enviar(jogador, "Trilha escolhida: " + alvo.getNome()
                + (liberadas > 0 ? " (" + liberadas + " habilidade(s) liberada(s))" : ""), ChatFormatting.GREEN);
    }
}
