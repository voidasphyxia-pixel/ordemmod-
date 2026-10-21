package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Servidor -> cliente: a ficha do personagem (tudo o que o menu da tecla K mostra) e a ordem de abrir a tela.
 * Os dados vao num CompoundTag, entao acrescentar uma informacao nova e so uma linha em {@link #de} e uma na tela.
 *
 * O envio (que depende de EventosOrdem) fica em ReceptoresServidor#enviarFicha.
 * Atencao para a tela (etapa 6): na 1.21.5+ os getters do CompoundTag devolvem Optional;
 * use getIntOr / getStringOr / getBooleanOr / getCompoundOrEmpty / getListOrEmpty.
 */
public record PacoteFicha(CompoundTag dados) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteFicha> TYPE = Rede.tipo("ficha");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteFicha> CODEC =
            StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, PacoteFicha::dados, PacoteFicha::new);

    public static PacoteFicha de(Personagem p) {
        CompoundTag t = new CompoundTag();
        t.putInt("classe", p.classe.ordinal());
        Origem origem = p.getOrigem();
        t.putInt("origem", origem == null ? -1 : origem.ordinal());
        t.putInt("nex", p.nex.getNex());
        t.putInt("nivel", p.nivel());
        t.putInt("falta", p.nex.faltaParaProximoMarco());

        int[] atributos = new int[Atributo.values().length];
        for (Atributo a : Atributo.values()) {
            atributos[a.ordinal()] = p.atributos.get(a);
        }
        t.putIntArray("atributos", atributos);
        t.putInt("pontosAtributo", p.atributos.getPontosLivres());

        byte[] graus = new byte[Pericia.values().length];
        byte[] deOrigem = new byte[Pericia.values().length];
        for (Pericia pericia : Pericia.values()) {
            graus[pericia.ordinal()] = (byte) p.pericias.getGrau(pericia).ordinal();
            deOrigem[pericia.ordinal()] = (byte) (p.pericias.veioDaOrigem(pericia) ? 1 : 0);
        }
        t.putByteArray("graus", graus);
        t.putByteArray("deOrigem", deOrigem);
        t.putInt("vagasTotal", p.pericias.vagasDeTreino() + p.pericias.getTreinosExtras());
        t.putInt("vagasLivres", p.pericias.vagasRestantes());
        t.putInt("melhoriasVeterano", p.pericias.getMelhoriasVeterano());
        t.putInt("melhoriasExpert", p.pericias.getMelhoriasExpert());

        Trilha trilha = p.habilidades.getTrilha();
        t.putInt("trilha", trilha == null ? -1 : trilha.ordinal());
        t.putInt("pontosHabilidade", p.habilidades.getPontos());

        t.putInt("vida", p.vidaMaxima());
        t.putInt("sanidade", p.sanidadeMaxima());
        t.putInt("esforco", p.esforcoMaximo());
        t.putInt("defesa", p.habilidades.defesaTotal(p.efeitos));
        t.putInt("critCorpo", percentual(p.chanceCritico(TipoDano.CORPO_A_CORPO)));
        t.putInt("critDistancia", percentual(p.chanceCritico(TipoDano.A_DISTANCIA)));
        t.putInt("resistFisica", percentual(p.chanceResistencia(false)));
        t.putInt("resistMental", percentual(p.chanceResistencia(true)));
        t.putInt("velocidade", percentual(p.velocidadeExtra()));
        t.putDouble("danoForca", EfeitosAtributos.bonusDano(p.atributos));

        ListTag habilidades = new ListTag();
        for (Habilidade h : p.habilidades.getPassivas()) {
            habilidades.add(habilidade(h));
        }
        for (Habilidade h : p.habilidades.getAtivas()) {
            habilidades.add(habilidade(h));
        }
        t.put("habilidades", habilidades);
        t.put("arvore", arvore(p));
        t.put("trilhas", trilhas(p));
        return new PacoteFicha(t);
    }

    /** Estados de um no da arvore (a tela usa os mesmos numeros). */
    public static final int BLOQUEADA = 0;
    public static final int DISPONIVEL = 1;
    public static final int APRENDIDA = 2;

    /**
     * As habilidades de classe da arvore (as "raizes": sem as versoes aprimoradas e sem as de trilha).
     * Ja vao com o estado calculado pelo servidor, entao a tela so desenha e manda o clique de volta.
     */
    private static ListTag arvore(Personagem p) {
        ListTag lista = new ListTag();
        int nivel = p.nivel();
        List<Habilidade> raizes = new ArrayList<>();
        for (Habilidade h : CatalogoHabilidades.daClasse(p.classe)) {
            if (!h.isEvolucao() && !h.isDeTrilha()) {
                raizes.add(h);
            }
        }
        raizes.sort(Comparator.comparingInt(Habilidade::getNivelMinimo));
        for (Habilidade raiz : raizes) {
            Habilidade atual = p.habilidades.versaoEquipada(raiz); // a versao equipada (por padrao, a mais forte ja aprendida)
            boolean aprendida = p.habilidades.temAprendida(raiz);
            // repetivel (Treinamento em Pericia): continua a venda mesmo depois de aprendida
            String motivo = aprendida && !raiz.isRepetivel() ? null : p.habilidades.motivoBloqueio(raiz, nivel);

            CompoundTag c = new CompoundTag();
            c.putString("id", raiz.getId());
            c.putString("pai", raiz.getPrerequisitos().isEmpty() ? "" : raiz.getPrerequisitos().get(0));
            c.putString("nome", atual.getNome());
            c.putString("descricao", atual.getDescricao());
            c.putBoolean("ativa", atual.isAtiva());
            c.putInt("custo", atual.getCustoPe());
            c.putInt("nex", HabilidadesDoPersonagem.nexDoNivel(raiz.getNivelMinimo()));
            int estado;
            if (aprendida && !raiz.isRepetivel()) {
                estado = APRENDIDA;
            } else if (motivo == null) {
                estado = DISPONIVEL;
            } else {
                estado = aprendida ? APRENDIDA : BLOQUEADA; // repetivel ja aprendida, mas sem ponto agora
            }
            c.putInt("estado", estado);
            c.putString("motivo", motivo == null ? "" : motivo);
            c.putBoolean("repetivel", raiz.isRepetivel());
            c.putInt("vezes", p.habilidades.vezesAprendida(raiz));

            // versoes aprimoradas: em que NEX cada uma chega, e quantas ja estao liberadas
            StringBuilder evolucoes = new StringBuilder();
            int total = 1;
            int liberadas = aprendida ? 1 : 0;
            Habilidade cursor = raiz;
            boolean achou = true;
            while (achou) {
                achou = false;
                for (Habilidade h : CatalogoHabilidades.daClasse(p.classe)) {
                    if (cursor.getId().equals(h.getEvolucaoDe())) {
                        total++;
                        if (p.habilidades.temAprendida(h)) {
                            liberadas++;
                        }
                        if (evolucoes.length() > 0) {
                            evolucoes.append(", ");
                        }
                        evolucoes.append(HabilidadesDoPersonagem.nexDoNivel(h.getNivelMinimo())).append('%');
                        cursor = h;
                        achou = true;
                        break;
                    }
                }
            }
            c.putString("evolucoes", evolucoes.toString());
            c.putInt("versoes", total);
            c.putInt("versaoAtual", liberadas);

            // cada escalonamento (versao por % de NEX) vira um icone na tela; o jogador escolhe qual fica equipado,
            // porque cada versao gasta um PE diferente
            ListTag versoes = new ListTag();
            for (Habilidade v : p.habilidades.cadeia(raiz)) {
                CompoundTag vc = new CompoundTag();
                vc.putString("id", v.getId());
                vc.putString("nome", v.getNome());
                vc.putString("descricao", v.getDescricao());
                vc.putBoolean("ativa", v.isAtiva());
                vc.putInt("custo", v.getCustoPe());
                vc.putInt("nex", HabilidadesDoPersonagem.nexDoNivel(v.getNivelMinimo()));
                vc.putBoolean("liberada", p.habilidades.temAprendida(v));
                versoes.add(vc);
            }
            c.put("versaoLista", versoes);
            c.putInt("versaoEquipada", aprendida ? p.habilidades.indiceEquipado(raiz) : -1);
            c.putBoolean("equipavel", aprendida && !raiz.isRepetivel() && total > 1 && atual.isAtiva());
            lista.add(c);
        }
        return lista;
    }

    /** As trilhas da classe do personagem, cada uma com as habilidades que libera (e em que NEX). */
    private static ListTag trilhas(Personagem p) {
        ListTag lista = new ListTag();
        for (Trilha trilha : Trilha.values()) {
            if (trilha.getClasse() != p.classe) {
                continue;
            }
            CompoundTag c = new CompoundTag();
            c.putInt("indice", trilha.ordinal());
            c.putString("nome", trilha.getNome());
            c.putString("descricao", trilha.getDescricao());
            ListTag hs = new ListTag();
            List<Habilidade> daTrilha = new ArrayList<>(CatalogoHabilidades.daTrilha(trilha));
            daTrilha.sort(Comparator.comparingInt(Habilidade::getNivelMinimo));
            for (Habilidade h : daTrilha) {
                CompoundTag hc = new CompoundTag();
                hc.putString("id", h.getId());
                hc.putString("nome", h.getNome());
                hc.putString("descricao", h.getDescricao());
                hc.putInt("nex", HabilidadesDoPersonagem.nexDoNivel(h.getNivelMinimo()));
                hc.putBoolean("ativa", h.isAtiva());
                hc.putInt("custo", h.getCustoPe());
                hc.putBoolean("liberada", p.habilidades.temAprendida(h));
                hs.add(hc);
            }
            c.put("habilidades", hs);
            lista.add(c);
        }
        return lista;
    }

    private static int percentual(double fracao) {
        return (int) Math.round(fracao * 100);
    }

    private static CompoundTag habilidade(Habilidade h) {
        CompoundTag c = new CompoundTag();
        c.putString("nome", h.getNome());
        c.putString("descricao", h.getDescricao());
        c.putBoolean("ativa", h.isAtiva());
        c.putInt("custo", h.getCustoPe());
        c.putBoolean("trilha", h.isDeTrilha());
        return c;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
