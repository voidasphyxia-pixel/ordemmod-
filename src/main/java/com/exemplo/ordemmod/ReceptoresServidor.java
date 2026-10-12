package com.exemplo.ordemmod;

import java.util.List;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

/**
 * O que o SERVIDOR faz quando chega cada pacote do cliente (antes era o metodo tratar() de cada Pacote*).
 * No Fabric o receptor ja roda na thread do servidor, entao nao existe enqueueWork.
 *
 * AINDA NAO esta na lista de compilacao do build.gradle: depende de EventosOrdem, Avisos,
 * TesteNaTela, CriacaoPersonagem e do ReceptorMarcado, que sao portados na etapa 4.
 * Quando entrarem, chame ReceptoresServidor.registrar() e ReceptorMarcado.registrar() no OrdemMod.
 */
public final class ReceptoresServidor {
    /** Anti-spam do clique de pericia: um por segundo. */
    private static final int RECARGA_ROLAR_TICKS = 20;

    private ReceptoresServidor() {
    }

    public static void registrar() {
        ServerPlayNetworking.registerGlobalReceiver(PacoteHabilidade.TYPE, (m, contexto) -> {
            ServerPlayer jogador = contexto.player();
            if (m.acao() == PacoteHabilidade.ABRIR_FICHA) {
                enviarFicha(jogador);
            } else {
                EventosOrdem.aoTeclaHabilidade(jogador, m.acao());
            }
        });

        ServerPlayNetworking.registerGlobalReceiver(PacoteCriarPersonagem.TYPE, (m, contexto) -> {
            ServerPlayer jogador = contexto.player();
            if (EventosOrdem.de(jogador).isCriado()) {
                return; // quem ja criou o personagem nao recria por aqui (use /ordem resetar)
            }
            Personagem novo = CriacaoPersonagem.montar(
                    daLista(ClasseOP.values(), m.classe()),
                    m.atributos(),
                    daLista(Origem.values(), m.origem()),
                    m.pericias().stream().map(i -> daLista(Pericia.values(), i)).toList());
            if (novo == null) {
                Avisos.enviar(jogador, "Criação não aceita. Tente de novo.", ChatFormatting.RED);
                EventosOrdem.abrirCriacao(jogador);
                return;
            }
            EventosOrdem.definir(jogador, novo);
        });

        ServerPlayNetworking.registerGlobalReceiver(PacoteRolarPericia.TYPE, (m, contexto) -> {
            ServerPlayer jogador = contexto.player();
            Personagem p = EventosOrdem.de(jogador);
            Pericia[] todas = Pericia.values();
            if (!p.isCriado() || m.pericia() < 0 || m.pericia() >= todas.length) {
                return;
            }
            long agora = jogador.level().getGameTime();
            if (agora < jogador.getAttachedOrElse(AnexosOrdem.RECARGA_ROLAR, 0L)) {
                return;
            }
            jogador.setAttached(AnexosOrdem.RECARGA_ROLAR, agora + RECARGA_ROLAR_TICKS);
            // clique na ficha nao tem situacao propria: usa a DT media. Outros sistemas chamam TesteNaTela com a DT deles.
            TesteNaTela.rolar(jogador, todas[m.pericia()], Pericias.DT_MEDIA, null);
        });

        ServerPlayNetworking.registerGlobalReceiver(PacoteArvore.TYPE, (m, contexto) -> {
            ServerPlayer jogador = contexto.player();
            Personagem p = EventosOrdem.de(jogador);
            if (!p.isCriado()) {
                return;
            }
            if (m.acao() == PacoteArvore.APRENDER) {
                aprender(jogador, p, m.id());
            } else if (m.acao() == PacoteArvore.TRILHA) {
                escolherTrilha(jogador, p, m.indice());
            } else if (m.acao() == PacoteArvore.MELHORAR) {
                melhorarPericia(jogador, p, m.indice());
            } else if (m.acao() == PacoteArvore.TREINAMENTO) {
                treinamento(jogador, p, m.indice(), m.indice2());
            }
            enviarFicha(jogador); // manda a ficha atualizada: a tela aberta se redesenha sozinha
        });
    }

    /** O servidor monta a ficha e manda para o jogador (quem ainda nao criou o personagem vai para a criacao). */
    public static void enviarFicha(ServerPlayer jogador) {
        Personagem p = EventosOrdem.de(jogador);
        if (!p.isCriado()) {
            EventosOrdem.abrirCriacao(jogador);
            return;
        }
        Rede.paraJogador(jogador, PacoteFicha.de(p));
    }

    /** Valor do enum pelo indice, ou null se o indice e invalido. */
    private static <T> T daLista(T[] valores, int indice) {
        return indice >= 0 && indice < valores.length ? valores[indice] : null;
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

    /** Treinamento em Pericia: gasta 1 ponto de habilidade e sobe duas pericias diferentes um grau cada. */
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
