package com.exemplo.ordemmod;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** Testes das perícias (Java puro). Rode o main, como o Teste.java. */
public class TestePericias {
    static int falhas = 0;

    static boolean perto(double a, double b) { return Math.abs(a - b) < 1e-9; }
    static void ok(boolean c, String m) {
        if (!c) {
            falhas++;
            System.out.println("FALHOU: " + m);
        } else {
            System.out.println("ok: " + m);
        }
    }

    /** Um Random que entrega os dados (1 a 20) na ordem que você mandar. */
    static Random dados(int... valores) {
        return new Random() {
            int i = 0;

            @Override
            public int nextInt(int limite) {
                return valores[i++ % valores.length] - 1;
            }
        };
    }

    public static void main(String[] args) {
        Atributos at = new Atributos();
        at.set(Atributo.AGILIDADE, 3);
        at.set(Atributo.INTELECTO, 2);
        at.set(Atributo.PRESENCA, 0);
        at.set(Atributo.FORCA, 1);

        Pericias per = new Pericias(ClasseOP.COMBATENTE, at);

        // --- as 28 perícias e seus atributos
        ok(Pericia.values().length == 28, "28 perícias");
        ok(Pericia.PERCEPCAO.getAtributo() == Atributo.PRESENCA && Pericia.LUTA.getAtributo() == Atributo.FORCA,
                "Percepção é PRE e Luta é FOR");
        ok(Pericia.porTexto("Percepção") == Pericia.PERCEPCAO && Pericia.porTexto("sobrevivencia") == Pericia.SOBREVIVENCIA
                && Pericia.porTexto("xyz") == null, "porTexto aceita acento, minúsculas e rejeita o que não existe");

        // --- vagas de treino
        ok(per.vagasDeTreino() == 5, "Combatente: 3 + Intelecto 2 = 5 vagas");
        ok(new Pericias(ClasseOP.ESPECIALISTA, at).vagasDeTreino() == 9, "Especialista: 7 + 2 = 9");
        ok(new Pericias(ClasseOP.OCULTISTA, at).vagasDeTreino() == 7, "Ocultista: 5 + 2 = 7");
        ok(per.getGrau(Pericia.LUTA) == GrauTreinamento.DESTREINADO && per.bonus(Pericia.LUTA) == 0, "começa destreinado");
        ok(per.treinar(Pericia.LUTA) && per.bonus(Pericia.LUTA) == 5, "treinar dá +5");
        ok(!per.treinar(Pericia.LUTA), "não treina duas vezes");
        per.treinar(Pericia.FORTITUDE);
        per.treinar(Pericia.REFLEXOS);
        per.treinar(Pericia.FURTIVIDADE);
        ok(per.vagasRestantes() == 1, "4 treinadas, sobra 1 vaga");
        per.treinar(Pericia.PERCEPCAO);
        ok(per.vagasRestantes() == 0 && !per.treinar(Pericia.MEDICINA), "sem vaga não treina");
        at.set(Atributo.INTELECTO, 3);
        ok(per.vagasRestantes() == 1 && per.treinar(Pericia.MEDICINA), "subir Intelecto libera mais uma vaga");

        // --- melhorias de grau só nos marcos
        ok(!per.melhorar(Pericia.LUTA), "sem marco, sem melhoria");
        per.aoSubirDeNivel(6);
        ok(per.getMelhoriasVeterano() == 0, "nível 6 não libera nada");
        per.aoSubirDeNivel(7);
        ok(per.getMelhoriasVeterano() == 5, "NEX 35% (nível 7): 2 + Intelecto (3) = 5 melhorias para Veterano");
        ok(per.melhorar(Pericia.LUTA) && per.getGrau(Pericia.LUTA) == GrauTreinamento.VETERANO, "Luta vira Veterano");
        ok(per.bonus(Pericia.LUTA) == 10, "Veterano dá +10");
        ok(!per.melhorar(Pericia.LUTA), "Veterano → Expert precisa do marco de 70%");
        ok(!per.melhorar(Pericia.ACROBACIA), "destreinada não melhora");
        per.aoSubirDeNivel(14);
        ok(per.melhorar(Pericia.LUTA) && per.bonus(Pericia.LUTA) == 15, "NEX 70%: Luta vira Expert (+15)");
        ok(!per.melhorar(Pericia.LUTA), "Expert é o teto");

        // --- rolagem de testes
        // Agilidade 3, Furtividade treinada (+5): rola 3 dados, fica com o maior
        ResultadoTeste r = per.testar(Pericia.FURTIVIDADE, 15, dados(4, 11, 7));
        ok(r.dados().size() == 3 && r.escolhido() == 11 && r.total() == 16 && r.sucesso(), "3 dados [4,11,7] → 11 + 5 = 16 passa DT 15");
        r = per.testar(Pericia.FURTIVIDADE, 20, dados(4, 11, 7));
        ok(!r.sucesso() && r.margem() == -4, "16 contra DT 20 falha, faltou 4");
        r = per.testar(Pericia.FURTIVIDADE, 30, dados(3, 20, 1));
        ok(r.critico() && r.sucesso(), "20 natural passa sozinho, mesmo contra DT 30");
        // Presença 0: rola 2 dados e fica com o MENOR
        r = per.testar(Pericia.PERCEPCAO, 10, dados(18, 6));
        ok(r.dados().size() == 2 && r.escolhido() == 6 && r.total() == 11, "atributo 0: 2 dados, fica com o menor (6) + 5 = 11");
        // Atributo 1: rola só 1 dado (antes rolava 2)
        Atributos um = new Atributos();
        um.set(Atributo.FORCA, 1);
        r = new Pericias(ClasseOP.COMBATENTE, um).testar(Pericia.LUTA, 10, dados(7, 15));
        ok(r.dados().size() == 1 && r.escolhido() == 7, "atributo 1: 1 dado só");
        // bônus extra
        r = per.testar(Pericia.LUTA, 25, 3, dados(9));
        ok(r.bonus() == 18 && r.total() == 27 && r.sucesso(), "Expert (+15) com bônus extra +3: 9 + 18 = 27");
        ok(r.toString().contains("SUCESSO") && r.toString().contains("Luta"), "o texto do resultado é legível");

        // --- partes do bônus (para a tela do dado somar uma a uma)
        {
            Pericias q = new Pericias(ClasseOP.COMBATENTE, at);
            q.treinar(Pericia.INVESTIGACAO);
            var partes = q.partesDoBonus(Pericia.INVESTIGACAO, 2, "Lupa");
            int soma = partes.stream().mapToInt(ParteBonus::valor).sum();
            ResultadoTeste rr = q.testar(Pericia.INVESTIGACAO, 15, 2, dados(10));
            ok(partes.size() == 2 && partes.get(0).rotulo().equals("Treinado") && partes.get(0).valor() == 5,
                    "partes: Treinado +5 primeiro");
            ok(partes.get(1).rotulo().equals("Lupa") && partes.get(1).valor() == 2, "partes: bônus extra com o nome dado");
            ok(soma == rr.bonus(), "a soma das partes é igual ao bônus do teste");
            ok(q.partesDoBonus(Pericia.MEDICINA, 0, null).isEmpty(), "destreinada e sem extra: nenhuma parte");
        }

        // --- combate e resistência
        ok(Math.abs(per.multiplicadorDano(TipoDano.CORPO_A_CORPO) - 1.35) < 1e-9, "Luta Expert: +35% de dano corpo a corpo");
        ok(per.multiplicadorDano(TipoDano.A_DISTANCIA) == 1.0 && per.multiplicadorDano(TipoDano.OUTRO) == 1.0, "sem Pontaria, sem bônus à distância");
        ok(Math.abs(per.bonusCritico(TipoDano.DESARMADO) - 0.15) < 1e-9, "Luta Expert: +15% de crítico");
        ok(Math.abs(per.bonusResistencia(false) - 0.05) < 1e-9 && per.bonusResistencia(true) == 0,
                "Fortitude treinada: +5% (físico); Vontade destreinada: 0");

        // --- restaurar (salvamento) e save antigo
        Map<Pericia, GrauTreinamento> salvo = new EnumMap<>(Pericia.class);
        salvo.put(Pericia.VONTADE, GrauTreinamento.VETERANO);
        Pericias outra = new Pericias(ClasseOP.OCULTISTA, at);
        outra.restaurar(salvo, 1, 2);
        ok(outra.bonus(Pericia.VONTADE) == 10 && outra.getMelhoriasVeterano() == 1 && outra.getMelhoriasExpert() == 2,
                "restaurar devolve graus e melhorias");
        Pericias antigo = new Pericias(ClasseOP.COMBATENTE, at);
        antigo.concederRetroativo(10);
        ok(antigo.getMelhoriasVeterano() == 5 && antigo.getMelhoriasExpert() == 0, "save antigo no nível 10: só as de Veterano (2 + Intelecto)");

        // --- integração com o Personagem
        Personagem p = new Personagem(ClasseOP.COMBATENTE);
        double critSem = p.chanceCritico(TipoDano.CORPO_A_CORPO);
        p.pericias.treinar(Pericia.LUTA);
        ok(Math.abs(p.chanceCritico(TipoDano.CORPO_A_CORPO) - (critSem + 0.05)) < 1e-9, "Personagem: Luta treinada soma 5% de crítico");
        double resSem = p.chanceResistencia(true);
        p.pericias.treinar(Pericia.VONTADE);
        ok(Math.abs(p.chanceResistencia(true) - (resSem + 0.05)) < 1e-9, "Personagem: Vontade soma na resistência mental");
        p.ganharNex(30); // NEX 5% → 35% = nível 7
        ok(p.nivel() == 7 && p.pericias.getMelhoriasVeterano() == 3, "Personagem: NEX 35% libera 2 + Intelecto (1) melhorias");

        // susto: Vontade treinada (+5), Presença 1 → 1 dado
        Personagem m = new Personagem(ClasseOP.COMBATENTE);
        ok(m.perdaDeSanidadeAjustada(3, true, dados(20)) == 2, "Vontade passa (20 natural): perde 2 em vez de 3");
        m.pericias.treinar(Pericia.OCULTISMO);
        ok(m.perdaDeSanidadeAjustada(3, true, dados(20)) == 1, "com Ocultismo treinado contra o paranormal: perde 1");
        ok(m.perdaDeSanidadeAjustada(1, false, dados(20)) == 0, "susto comum de 1 vira 0 se passar");
        ok(m.perdaDeSanidadeAjustada(3, true, dados(2)) == 3, "falhou no teste: perde tudo");


        // --- tabela de benefícios por patamar
        Pericias tab = new Pericias(ClasseOP.COMBATENTE, at);
        tab.treinar(Pericia.LUTA);
        ok(Math.abs(tab.multiplicadorDano(TipoDano.CORPO_A_CORPO) - 1.10) < 1e-9, "Luta Treinado: +10% de dano");
        tab.concederRetroativo(14);
        tab.melhorar(Pericia.LUTA);
        ok(Math.abs(tab.multiplicadorDano(TipoDano.DESARMADO) - 1.20) < 1e-9, "Luta Veterano: +20% de dano");
        tab.melhorar(Pericia.LUTA);
        ok(Math.abs(tab.multiplicadorDano(TipoDano.CORPO_A_CORPO) - 1.35) < 1e-9, "Luta Expert: +35% de dano");
        ok(tab.valor(Beneficio.PONTARIA_DANO) == 0, "destreinada não dá benefício");
        ok(Beneficio.LUTA_DANO.formatar(GrauTreinamento.VETERANO).equals("+20%"), "formata percentual");
        ok(Beneficio.INICIATIVA_RECARGA.formatar(GrauTreinamento.EXPERT).equals("10 s"), "formata segundos");
        ok(Beneficio.de(Pericia.LUTA).size() == 2 && Beneficio.de(Pericia.ARTES).isEmpty(), "lista por perícia");
        for (Beneficio b : Beneficio.values()) {
            double t1 = b.valor(GrauTreinamento.TREINADO), v1 = b.valor(GrauTreinamento.VETERANO), e1 = b.valor(GrauTreinamento.EXPERT);
            boolean menorMelhor = b == Beneficio.INICIATIVA_RECARGA || b == Beneficio.MEDICINA_RECARGA;
            ok(b.valor(GrauTreinamento.DESTREINADO) == 0 && (menorMelhor ? (t1 >= v1 && v1 >= e1) : (t1 <= v1 && v1 <= e1)),
                    b.name() + ": cresce com o patamar");
        }

        // Vontade Veterano regenera sanidade a cada 30 s
        Personagem v = new Personagem(ClasseOP.COMBATENTE);
        v.pericias.concederRetroativo(7);
        v.pericias.treinar(Pericia.VONTADE);
        v.pericias.melhorar(Pericia.VONTADE);
        v.setSanidade(v.getSanidade() - 5);
        int antes = v.getSanidade();
        for (int i = 0; i < 30; i++) {
            v.passarSegundo(false, false);
        }
        ok(v.getSanidade() == antes + 1, "Vontade Veterano: +1 SAN a cada 30 s");


        // --- poder Treinamento em Perícia (repetível) e perícias que sobem por poder
        Personagem tp = new Personagem(ClasseOP.COMBATENTE);
        Habilidade treino = CatalogoHabilidades.porId("treinamento_pericia");
        ok(treino != null && treino.isRepetivel(), "Treinamento em Perícia existe e é repetível");
        tp.habilidades.restaurar(3, null, java.util.List.of());
        ok(tp.habilidades.aprender(treino, 1) && tp.habilidades.vezesAprendida(treino) == 1, "1ª vez: aprendida");
        ok(tp.habilidades.aprender(treino, 1) && tp.habilidades.vezesAprendida(treino) == 2, "2ª vez: conta de novo");
        ok(tp.habilidades.getAprendidas().stream().filter(h -> h == treino).count() == 1, "aparece uma vez só na lista");
        ok(tp.habilidades.getPontos() == 1, "cada vez gasta 1 ponto");
        ok(tp.habilidades.motivoBloqueio(CatalogoHabilidades.porId("ataque_especial"), 1) == null
                && tp.habilidades.motivoBloqueio(treino, 1) == null, "repetível continua disponível");
        ok(CatalogoHabilidades.porId("pele_grossa").isRepetivel() == false, "as comuns não são repetíveis");

        Pericias pp = new Pericias(ClasseOP.COMBATENTE, new Atributos()); // Intelecto 1: 4 vagas
        int vagas = pp.vagasRestantes();
        ok(pp.subirPorPoder(Pericia.LUTA, 1) && pp.getGrau(Pericia.LUTA) == GrauTreinamento.TREINADO, "poder: Destreinada → Treinada");
        ok(pp.vagasRestantes() == vagas, "perícia treinada por poder não gasta vaga");
        ok(!pp.subirPorPoder(Pericia.LUTA, 6), "Treinada → Veterana só a partir do NEX 35% (nível 7)");
        ok(pp.subirPorPoder(Pericia.LUTA, 7) && pp.getGrau(Pericia.LUTA) == GrauTreinamento.VETERANO, "NEX 35%: Treinada → Veterana");
        ok(!pp.subirPorPoder(Pericia.LUTA, 13), "Veterana → Expert só a partir do NEX 70% (nível 14)");
        ok(pp.subirPorPoder(Pericia.LUTA, 14) && pp.getGrau(Pericia.LUTA) == GrauTreinamento.EXPERT, "NEX 70%: Veterana → Expert");
        ok(!pp.subirPorPoder(Pericia.LUTA, 20) && !pp.podeSubirPorPoder(Pericia.LUTA, 20), "Expert é o teto");

        // --- poderes de tiro novos
        Habilidade tiro = CatalogoHabilidades.porId("tiro_certeiro");
        ok(tiro != null && CatalogoHabilidades.porId("mira_treinada").getPrerequisitos().equals(java.util.List.of("tiro_certeiro"))
                && CatalogoHabilidades.porId("rajada").getPrerequisitos().equals(java.util.List.of("mira_treinada")),
                "cadeia Tiro Certeiro → Mira Treinada → Rajada");
        ok(tiro.bonusDanoPassivoPara(TipoDano.A_DISTANCIA) == 2 && tiro.bonusDanoPassivoPara(TipoDano.CORPO_A_CORPO) == 0,
                "Tiro Certeiro: +2 só à distância");
        ok(perto(CatalogoHabilidades.porId("mira_treinada").chanceCriticoPara(TipoDano.A_DISTANCIA), 0.10), "Mira Treinada: 10% de crítico à distância");
        ok(CatalogoHabilidades.porId("rajada").isAtiva() && CatalogoHabilidades.porId("fogo_de_cobertura").isAtiva(), "Rajada e Fogo de Cobertura são ativas");
        for (Trilha tr : Trilha.values()) {
            ok(tr.getDescricao() != null && !tr.getDescricao().isBlank(), tr.getNome() + ": tem descrição");
        }

        System.out.println(falhas == 0 ? "TUDO OK" : falhas + " falhas");
    }
}
