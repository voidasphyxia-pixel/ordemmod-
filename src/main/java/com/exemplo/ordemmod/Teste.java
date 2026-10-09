package com.exemplo.ordemmod;

import java.util.Set;

public class Teste {
    static int falhas = 0;
    static void ok(boolean c, String m) { if (!c) { falhas++; System.out.println("FALHOU: " + m); } else System.out.println("ok: " + m); }

    /** Um Random que sempre sorteia o mesmo valor, para o teste dar sempre o mesmo resultado. */
    static java.util.Random sorteio(double valor) {
        return new java.util.Random() {
            @Override
            public double nextDouble() {
                return valor;
            }
        };
    }

    /** Um Random cujos dados de 20 faces sempre saem com a face pedida (nextInt(20) + 1 == face). */
    static java.util.Random dado(int face) {
        return new java.util.Random() {
            @Override
            public int nextInt(int limite) {
                return face - 1;
            }
        };
    }

    static Habilidade hab(String id) { return CatalogoHabilidades.porId(id); }
    static boolean perto(double a, double b) { return Math.abs(a - b) < 1e-9; }

    static void testarTrilhas() {
        Atributos at = new Atributos();
        at.set(Atributo.FORCA, 3);
        at.set(Atributo.PRESENCA, 2);
        at.set(Atributo.AGILIDADE, 2);
        at.set(Atributo.VIGOR, 3);

        // --- catalogo: 4 habilidades por trilha de Combatente, nos niveis 2/8/13/20
        for (Trilha tr : Trilha.values()) {
            if (tr.getClasse() != ClasseOP.COMBATENTE) continue;
            var lista = CatalogoHabilidades.daTrilha(tr);
            ok(lista.size() == 4, tr.getNome() + ": 4 habilidades no catalogo");
            ok(lista.stream().map(Habilidade::getNivelMinimo).toList().equals(java.util.List.of(2, 8, 13, 20)),
                    tr.getNome() + ": niveis 2, 8, 13 e 20");
        }

        // --- escolha da trilha
        HabilidadesDoPersonagem p = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        ok(!p.escolherTrilha(Trilha.ANIQUILADOR, 1), "NEX 5% nao escolhe trilha");
        ok(!p.escolherTrilha(Trilha.INFILTRADOR, 2), "trilha de outra classe nao pode");
        ok(p.escolherTrilha(Trilha.ANIQUILADOR, 2), "NEX 10% escolhe Aniquilador");
        ok(!p.escolherTrilha(Trilha.GUERREIRO, 2), "nao da para trocar de trilha");
        ok(p.getTrilha() == Trilha.ANIQUILADOR, "trilha guardada");
        ok(p.temAprendida(hab("a_favorita")), "A Favorita liberada na hora (nivel 2)");
        ok(!p.temAprendida(hab("tecnica_secreta")), "Tecnica Secreta ainda nao (nivel 8)");
        ok(p.getPontos() == 1, "trilha nao gasta pontos de habilidade");
        ok(!p.temAprendida(hab("tecnica_letal")), "habilidade de outra trilha nao vem junto");

        HabilidadesDoPersonagem q = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        ok(!q.podeAprender(hab("tecnica_letal"), 20), "habilidade de trilha nao se aprende com ponto");
        ok(q.disponiveis(20).stream().noneMatch(Habilidade::isDeTrilha), "trilha nao aparece nas disponiveis");

        // --- Aniquilador
        p.aoSubirDeNivel(8);
        ok(p.temAprendida(hab("tecnica_secreta")), "Tecnica Secreta liberada no nivel 8");
        p.aoSubirDeNivel(13);
        ok(p.temAprendida(hab("tecnica_sublime")), "Tecnica Sublime (evolucao dentro da trilha) no nivel 13");
        ok(p.getAtivas().size() == 1 && p.getAtivas().get(0).getId().equals("tecnica_sublime"),
                "so a Sublime conta como ativa");
        PontosEsforco pe = new PontosEsforco(10);
        EfeitosAtivos ef = new EfeitosAtivos();
        ok(p.usar(hab("tecnica_secreta"), pe, ef), "usar Tecnica Secreta usa a versao Sublime");
        ok(pe.getAtual() == 7, "Sublime custa 3 PE");
        ok(ef.bonusDano(TipoDano.A_DISTANCIA) == 12 && ef.bonusDano(TipoDano.DESARMADO) == 0, "+12 corpo a corpo/distancia, nao desarmado");
        p.aoSubirDeNivel(20);
        ok(perto(p.multiplicadorDano(TipoDano.CORPO_A_CORPO), 1.25), "Maquina de Guerra: +25%");
        ok(perto(p.danoFinal(10, TipoDano.CORPO_A_CORPO, ef), 26.5), "10*1.25 + 2 (A Favorita) + 12 (Sublime) = 26.5");

        // --- Guerreiro
        HabilidadesDoPersonagem gu = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        gu.escolherTrilha(Trilha.GUERREIRO, 2);
        ok(perto(gu.chanceCritico(TipoDano.CORPO_A_CORPO), 0.10), "Tecnica Letal: 10% de critico");
        ok(gu.chanceCritico(TipoDano.DESARMADO) == 0, "sem critico desarmado");
        gu.aoSubirDeNivel(8);
        PontosEsforco peG = new PontosEsforco(5);
        EfeitosAtivos efG = new EfeitosAtivos();
        ok(gu.usar(hab("revidar"), peG, efG), "usar Revidar");
        ok(efG.bonusDano(TipoDano.CORPO_A_CORPO) == 4 && efG.bonusDefesa() == 3, "Revidar: +4 dano e defesa = Forca (3)");
        ok(efG.chanceResistirEfeitos() == 0, "Revidar nao da resistencia");
        gu.aoSubirDeNivel(13);
        ok(gu.bonusDanoPassivo(TipoDano.CORPO_A_CORPO) == 3 && gu.bonusDanoPassivo(TipoDano.DESARMADO) == 3, "Forca Opressora: +3");
        gu.aoSubirDeNivel(20);
        ok(perto(gu.chanceCritico(TipoDano.CORPO_A_CORPO), 0.20), "Potencia Maxima soma +10% de critico (total 20%)");
        ok(perto(gu.multiplicadorDano(TipoDano.CORPO_A_CORPO), 1.20), "Potencia Maxima: +20% de dano");

        // --- Comandante de Campo
        HabilidadesDoPersonagem co = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        co.escolherTrilha(Trilha.COMANDANTE_DE_CAMPO, 2);
        PontosEsforco peC = new PontosEsforco(4);
        EfeitosAtivos efC = new EfeitosAtivos();
        ok(co.usar(hab("inspirar_confianca"), peC, efC), "usar Inspirar Confianca");
        ok(efC.bonusDefesa() == 2 && perto(efC.chanceResistirEfeitos(), 0.10), "defesa 2 e resistencia 10% (Presenca 2)");
        ok(efC.segundosRestantes("inspirar_confianca") == 60, "dura 1 minuto");
        co.aoSubirDeNivel(8);
        ok(co.defesaTotal(efC) == 4, "defesa 2 (efeito) + 2 (Estrategista)");
        co.aoSubirDeNivel(20);
        EfeitosAtivos limpo = new EfeitosAtivos();
        ok(co.defesaTotal(limpo) == 6, "Estrategista 2 + Oficial Comandante 4");
        ok(perto(co.chanceResistirEfeitos(), 0.20), "resistencia 5% + 15%");
        ok(co.bonusDanoPassivo(TipoDano.A_DISTANCIA) == 3, "Oficial Comandante: +3 de dano");
        ok(co.temAprendida(hab("brecha_na_guarda")), "Brecha na Guarda liberada no caminho");

        // --- Operacoes Especiais
        HabilidadesDoPersonagem op = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        op.escolherTrilha(Trilha.OPERACOES_ESPECIAIS, 2);
        ok(perto(op.bonusVelocidade(), 0.06), "Iniciativa Aprimorada: +6% de velocidade");
        op.aoSubirDeNivel(20);
        ok(perto(op.bonusVelocidade(), 0.16), "Sempre Alerta soma +10% (total 16%)");
        ok(op.defesaTotal(new EfeitosAtivos()) == 3, "Sempre Alerta: +3 de defesa");
        PontosEsforco peO = new PontosEsforco(10);
        EfeitosAtivos efO = new EfeitosAtivos();
        ok(op.usar(hab("surto_de_adrenalina"), peO, efO), "usar Surto de Adrenalina");
        ok(peO.getAtual() == 7 && efO.bonusDefesa() == 2 && efO.bonusDano(TipoDano.DESARMADO) == 5,
                "Surto: 3 PE, defesa = Agilidade (2), +5 de dano");
        ok(perto(EfeitosAtributos.velocidadeTotal(at, op), 2 * 0.04 + 0.16), "velocidade total = Agilidade + habilidades");

        // --- Tropa de Choque
        HabilidadesDoPersonagem tr = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        tr.escolherTrilha(Trilha.TROPA_DE_CHOQUE, 2);
        ok(tr.bonusVida(2) == 2 && tr.bonusVida(8) == 8, "Casca Grossa: +1 de vida por nivel");
        tr.aoSubirDeNivel(8);
        PontosEsforco peT = new PontosEsforco(5);
        EfeitosAtivos efT = new EfeitosAtivos();
        ok(tr.usar(hab("cai_dentro"), peT, efT), "usar Cai Dentro");
        ok(efT.bonusDefesa() == 6 && perto(efT.chanceResistirEfeitos(), 0.15), "defesa 2x3 e resistencia 5%x3");
        tr.aoSubirDeNivel(13);
        ok(tr.reducaoDanoRecebido(false) == 1 && tr.reducaoDanoRecebido(true) == 2, "Duro de Matar: -1 (ou -2 com protecao pesada)");
        ok(tr.danoRecebidoFinal(5, true) == 3 && tr.danoRecebidoFinal(1, true) == 0, "dano recebido reduzido, nunca abaixo de 0");
        tr.aoSubirDeNivel(20);
        ok(tr.reducaoDanoRecebido(false) == 2 && tr.reducaoDanoRecebido(true) == 3, "Inquebravel soma +1 de reducao");
        ok(tr.defesaTotal(new EfeitosAtivos(), true) == 3, "Inquebravel: +3 de defesa");
        ok(EfeitosAtributos.vidaMaximaTotal(ClasseOP.COMBATENTE, 20, at, tr)
                        == ClasseOP.COMBATENTE.vidaMaxima(20) + 3 * 2 + 20,
                "vida total = classe + Vigor + Casca Grossa");

        // --- trilhas de outras classes ainda sem habilidades
        ok(CatalogoHabilidades.daTrilha(Trilha.INFILTRADOR).isEmpty(), "trilhas de Especialista/Ocultista ainda vazias");
    }

    static void testarQuedaAcrobacia() {
        ok(perto(QuedaAcrobacia.reducao(14, false), 0.0), "queda: total 14 nao reduz nada");
        ok(perto(QuedaAcrobacia.reducao(15, false), 0.20), "queda: total 15 (minimo) reduz 20%");
        ok(perto(QuedaAcrobacia.reducao(19, false), 0.20), "queda: total 19 ainda 20%");
        ok(perto(QuedaAcrobacia.reducao(20, false), 0.40), "queda: total 20 reduz 40%");
        ok(perto(QuedaAcrobacia.reducao(25, false), 0.60), "queda: total 25 reduz 60%");
        ok(perto(QuedaAcrobacia.reducao(30, false), 0.80), "queda: total 30 reduz 80%");
        ok(perto(QuedaAcrobacia.reducao(35, false), 1.0), "queda: total 35 zera o dano");
        ok(perto(QuedaAcrobacia.reducao(99, false), 1.0), "queda: nunca passa de 100%");
        ok(perto(QuedaAcrobacia.reducao(3, true), 1.0), "queda: 20 natural zera o dano mesmo com total baixo");

        Atributos at = new Atributos();
        at.set(Atributo.AGILIDADE, 2);
        Pericias per = new Pericias(ClasseOP.COMBATENTE, at);
        ResultadoTeste baixo = per.testar(Pericia.ACROBACIA, QuedaAcrobacia.MINIMO, dado(1)); // dados = 1
        ok(!QuedaAcrobacia.rola(baixo) && QuedaAcrobacia.reducaoPercentual(baixo) == 0, "queda: dado 1 nao rola nem reduz");
        ResultadoTeste natural = per.testar(Pericia.ACROBACIA, QuedaAcrobacia.MINIMO, dado(20)); // dados = 20
        ok(natural.critico() && QuedaAcrobacia.rola(natural) && QuedaAcrobacia.reducaoPercentual(natural) == 100,
                "queda: 20 natural rola e zera (100%)");
    }

    static void testarEsquivaReflexos() {
        ok(EsquivaReflexos.DT_PADRAO == 20, "esquiva: DT padrao 20");
        ok(EsquivaReflexos.escolherPasso(1, 0) == EsquivaReflexos.Passo.TRAS, "esquiva: atacante na frente -> passo para tras");
        ok(EsquivaReflexos.escolherPasso(-1, 0) == EsquivaReflexos.Passo.FRENTE, "esquiva: atacante nas costas -> passo para frente");
        ok(EsquivaReflexos.escolherPasso(0.1, 0.9) == EsquivaReflexos.Passo.ESQUERDA, "esquiva: atacante na direita -> passo para a esquerda");
        ok(EsquivaReflexos.escolherPasso(0.1, -0.9) == EsquivaReflexos.Passo.DIREITA, "esquiva: atacante na esquerda -> passo para a direita");

        ok(EsquivaReflexos.dt(0) == 20 && EsquivaReflexos.dt(1) == 20, "esquiva: 1 atacante = DT 20");
        ok(EsquivaReflexos.dt(2) == 25 && EsquivaReflexos.dt(3) == 25, "esquiva: 2-3 inimigos = DT 25");
        ok(EsquivaReflexos.dt(4) == 30 && EsquivaReflexos.dt(5) == 30, "esquiva: 4-5 inimigos = DT 30");
        ok(EsquivaReflexos.dt(6) == 35, "esquiva: 6 inimigos = DT 35");
        ok(EsquivaReflexos.dt(7) == 35 && EsquivaReflexos.dt(10) == 35 && EsquivaReflexos.dt(40) == 35, "esquiva: bonus de cerco para em +15 (DT 35)");

        Atributos at = new Atributos();
        at.set(Atributo.AGILIDADE, 3);
        Pericias per = new Pericias(ClasseOP.COMBATENTE, at);
        ok(per.testar(Pericia.REFLEXOS, EsquivaReflexos.DT_PADRAO, dado(20)).sucesso(), "esquiva: 20 natural sempre esquiva");
        ok(!per.testar(Pericia.REFLEXOS, EsquivaReflexos.DT_PADRAO, dado(1)).sucesso(), "esquiva: dado 1 com bonus baixo nao esquiva");
    }

    public static void main(String[] a) {
        Atributos at = new Atributos();
        at.set(Atributo.FORCA, 3);
        at.set(Atributo.VIGOR, 2);
        at.set(Atributo.AGILIDADE, 2);
        ok(at.distribuicaoValida(), "distribuicao 3/2/2/1/1 valida (9 pontos)");
        at.set(Atributo.INTELECTO, 2);
        ok(!at.distribuicaoValida(), "10 pontos sem zero e invalida");
        at.set(Atributo.PRESENCA, 0);
        ok(at.distribuicaoValida(), "zerar um atributo da +1 ponto");

        NivelExposicao nex = new NivelExposicao();
        ok(nex.getNivel() == 1, "NEX 5% = nivel 1");
        nex.setNex(99);
        ok(nex.getNivel() == 20, "NEX 99% = nivel 20");

        at.set(Atributo.PRESENCA, 1);
        at.set(Atributo.INTELECTO, 1);
        HabilidadesDoPersonagem h = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        ok(h.getPontos() == 1, "comeca com 1 ponto");
        Habilidade ae = CatalogoHabilidades.porId("ataque_especial");
        ok(h.aprender(ae, 1), "aprende Ataque Especial");
        ok(h.getPontos() == 0, "gastou o ponto");
        h.aoSubirDeNivel(5);
        ok(h.temAprendida(CatalogoHabilidades.porId("ataque_especial_2")), "evolucao II liberada no nivel 5");
        h.aoSubirDeNivel(11);
        h.aoSubirDeNivel(17);
        ok(h.temAprendida(CatalogoHabilidades.porId("ataque_especial_4")), "evolucao IV liberada no nivel 17");
        ok(h.getAtivas().size() == 1, "so a melhor versao conta como ativa");

        PontosEsforco pe = new PontosEsforco(ClasseOP.COMBATENTE.esforcoMaximo(17, 1));
        EfeitosAtivos ef = new EfeitosAtivos();
        ok(h.usar(ae, pe, ef), "usar Ataque Especial");
        ok(ef.estaAtivo("ataque_especial"), "efeito ativo");
        ok(ef.bonusDano(TipoDano.DESARMADO) == 20, "bonus +20 desarmado (versao IV)");
        ok(ef.bonusDano(TipoDano.OUTRO) == 0, "sem bonus em OUTRO");
        for (int i = 0; i < 30 * 20; i++) ef.tick();
        ok(!ef.estaAtivo("ataque_especial"), "efeito acaba em 30s");

        Habilidade ap = CatalogoHabilidades.porId("armamento_pesado");
        ok(h.getPontos() >= 1, "tem ponto de habilidade sobrando para o teste de requisito");
        ok(h.podeAprender(ap, 20), "Forca 3 aprende Armamento Pesado");
        ok(!h.temProficiencia(Proficiencia.ARMAS_PESADAS), "antes de aprender: sem proficiencia em armas pesadas");
        ok(h.aprender(ap, 20), "aprende Armamento Pesado");
        ok(h.temProficiencia(Proficiencia.ARMAS_PESADAS), "Armamento Pesado da proficiencia em armas pesadas");
        ok(!h.temProficiencia(Proficiencia.PROTECOES_PESADAS), "mas nao em protecoes pesadas");
        HabilidadesDoPersonagem fraco = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, new Atributos());
        ok(!fraco.podeAprender(ap, 20), "Forca 1 nao aprende Armamento Pesado");
        Atributos forca2 = new Atributos();
        forca2.set(Atributo.FORCA, 2);
        ok(new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, forca2).podeAprender(ap, 20),
                "Forca 2 (o minimo exato) aprende Armamento Pesado");

        HabilidadesDoPersonagem m = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        m.aprender(CatalogoHabilidades.porId("artista_marcial"), 1);
        m.aoSubirDeNivel(7);
        ok(m.bonusDanoPassivo(TipoDano.DESARMADO) == 6, "Artista Marcial II = +6 (nao acumula com o I)");
        m.aoSubirDeNivel(14);
        ok(m.bonusDanoPassivo(TipoDano.DESARMADO) == 8, "Artista Marcial III = +8");

        HabilidadesDoPersonagem g = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        g.aprender(CatalogoHabilidades.porId("golpe_pesado"), 1);
        ok(Math.abs(g.danoFinal(10, TipoDano.CORPO_A_CORPO, new EfeitosAtivos()) - 13.5) < 1e-9, "Golpe Pesado: 10 -> 13.5");
        ok(g.danoFinal(10, TipoDano.DESARMADO, new EfeitosAtivos()) == 10, "Golpe Pesado nao afeta desarmado");

        HabilidadesDoPersonagem r = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        ok(r.bonusDefesa() == 0 && r.chanceResistirEfeitos() == 0, "sem Reflexos: defesa 0 e resistencia 0");
        ok(r.aprender(CatalogoHabilidades.porId("reflexos_defensivos"), 1), "aprende Reflexos Defensivos");
        ok(r.bonusDefesa() == 2, "Reflexos Defensivos: +2 de defesa");
        ok(Math.abs(r.chanceResistirEfeitos() - 0.05) < 1e-9, "Reflexos Defensivos: 5% de resistencia");
        ok(r.getPassivas().size() == 1, "conta como passiva");

        Habilidade st = CatalogoHabilidades.porId("sentido_tatico");
        HabilidadesDoPersonagem semIntelecto = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, new Atributos());
        ok(!semIntelecto.podeAprender(st, 1), "Intelecto 1 nao aprende Sentido Tatico");
        Atributos inteligente = new Atributos();
        inteligente.set(Atributo.INTELECTO, 3);
        HabilidadesDoPersonagem t = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, inteligente);
        ok(t.aprender(st, 1), "Intelecto 3 aprende Sentido Tatico");
        t.aoSubirDeNivel(1);
        ok(t.aprender(CatalogoHabilidades.porId("reflexos_defensivos"), 1), "aprende Reflexos Defensivos (com o ponto do nivel novo)");
        PontosEsforco pe2 = new PontosEsforco(3);
        EfeitosAtivos ef2 = new EfeitosAtivos();
        ok(t.usar(st, pe2, ef2), "usar Sentido Tatico");
        ok(pe2.getAtual() == 1, "gastou 2 PE");
        ok(ef2.estaAtivo("sentido_tatico"), "efeito de defesa ativo");
        ok(ef2.bonusDefesa() == 3, "defesa = intelecto (3)");
        ok(Math.abs(ef2.chanceResistirEfeitos() - 0.15) < 1e-9, "resistencia 5% x 3 = 15%");
        ok(ef2.segundosRestantes("sentido_tatico") == 180, "dura 3 minutos");
        ok(t.defesaTotal(ef2) == 5, "defesa total = 3 (Sentido) + 2 (Reflexos)");
        ok(Math.abs(t.chanceResistirEfeitosTotal(ef2) - 0.20) < 1e-9, "resistencia total = 15% + 5%");
        ok(!t.usar(st, pe2, ef2), "sem PE suficiente nao usa");
        ok(pe2.getAtual() == 1, "PE nao muda quando falha");
        for (int i = 0; i < 180 * 20; i++) ef2.tick();
        ok(!ef2.estaAtivo("sentido_tatico") && ef2.bonusDefesa() == 0, "efeito acaba em 3 minutos");
        ok(t.defesaTotal(ef2) == 2, "so os Reflexos sobram");

        HabilidadesDoPersonagem tg = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        ok(tg.aprender(CatalogoHabilidades.porId("tanque_de_guerra"), 1), "aprende Tanque de Guerra");
        tg.aoSubirDeNivel(1);
        ok(tg.aprender(CatalogoHabilidades.porId("reflexos_defensivos"), 1), "aprende Reflexos Defensivos junto do Tanque");
        EfeitosAtivos vazio = new EfeitosAtivos();
        ok(tg.defesaTotal(vazio, false) == 2, "sem protecao pesada: so os Reflexos (+2)");
        ok(tg.defesaTotal(vazio, true) == 4, "com protecao pesada: +2 do Tanque (total 4)");
        ok(Math.abs(tg.chanceResistirEfeitosTotal(vazio, false) - 0.05) < 1e-9, "sem protecao: 5%");
        ok(Math.abs(tg.chanceResistirEfeitosTotal(vazio, true) - 0.10) < 1e-9, "com protecao: 10%");
        ok(tg.getPassivas().size() == 2, "Tanque de Guerra e passiva");
        ok(tg.defesaTotal(vazio) == 2, "metodo antigo continua valendo (sem protecao)");

        // --- efeito de dano e de defesa com o MESMO id nao se apagam (bug corrigido)
        EfeitosAtivos misto = new EfeitosAtivos();
        misto.ativar("x", new EfeitoDano(5, 10, Set.of(TipoDano.DESARMADO)));
        misto.ativar("x", new EfeitoDefesa(2, 0.10, 20));
        ok(misto.bonusDano(TipoDano.DESARMADO) == 5 && misto.bonusDefesa() == 2, "dano e defesa com o mesmo id convivem");
        for (int i = 0; i < 10 * 20; i++) misto.tick();
        ok(misto.bonusDano(TipoDano.DESARMADO) == 0, "o efeito de dano acaba em 10s");
        ok(misto.bonusDefesa() == 2 && misto.estaAtivo("x"), "o de defesa continua depois do de dano acabar");
        ok(misto.segundosRestantes("x") == 10, "faltam 10s para o de defesa");
        for (int i = 0; i < 10 * 20; i++) misto.tick();
        ok(!misto.estaAtivo("x") && misto.bonusDefesa() == 0, "os dois acabaram");

        // --- habilidade ativa com dois efeitos ao usar
        Habilidade duplo = new Habilidade("duplo", "Duplo", "teste", TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 1)
                .comEfeito(new EfeitoDano(3, 30, Set.of(TipoDano.CORPO_A_CORPO)))
                .comEfeitoDefesaPorAtributo(Atributo.FORCA, 1, 5, 60);
        HabilidadesDoPersonagem d = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at); // Forca 3
        ok(d.aprender(duplo, 1), "aprende habilidade com dois efeitos");
        PontosEsforco peD = new PontosEsforco(5);
        EfeitosAtivos efD = new EfeitosAtivos();
        ok(d.usar(duplo, peD, efD), "usa habilidade com dois efeitos");
        ok(efD.bonusDano(TipoDano.CORPO_A_CORPO) == 3, "efeito de dano aplicado");
        ok(efD.bonusDefesa() == 3 && Math.abs(efD.chanceResistirEfeitos() - 0.15) < 1e-9, "efeito de defesa aplicado junto");

        // --- resistir a efeitos: a chance agora e usada
        HabilidadesDoPersonagem res = new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at);
        res.aprender(CatalogoHabilidades.porId("reflexos_defensivos"), 1); // 5%
        EfeitosAtivos nenhum = new EfeitosAtivos();
        ok(res.resistiuAoEfeito(nenhum, false, sorteio(0.04)), "sorteio 0.04 < 5%: resistiu");
        ok(!res.resistiuAoEfeito(nenhum, false, sorteio(0.05)), "sorteio 0.05 nao e menor que 5%: nao resistiu");
        ok(!res.resistiuAoEfeito(nenhum, false, sorteio(0.50)), "sorteio 0.50: nao resistiu");
        ok(!new HabilidadesDoPersonagem(ClasseOP.COMBATENTE, at).resistiuAoEfeito(nenhum, false, sorteio(0.0)),
                "chance 0%: nunca resiste (nem com sorteio 0)");

        testarQuedaAcrobacia();
        testarEsquivaReflexos();

        testarTrilhas();

        System.out.println(falhas == 0 ? "TUDO OK" : falhas + " falhas");
    }
}
