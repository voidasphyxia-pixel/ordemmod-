package com.exemplo.ordemmod;

import java.util.Random;

/** Testes das origens (Java puro). Rode a main: deve imprimir TUDO OK. */
public class TesteOrigens {
    static int falhas = 0;

    static void ok(boolean condicao, String nome) {
        if (!condicao) {
            falhas++;
        }
        System.out.println((condicao ? "ok: " : "FALHOU: ") + nome);
    }

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
        // --- todas as origens têm 2 perícias diferentes, poder e descrição
        for (Origem o : Origem.values()) {
            ok(o.getPericias().size() == 2 && o.getPericias().get(0) != o.getPericias().get(1)
                    && !o.getPoder().isEmpty() && !o.getDescricao().isEmpty(), o.getNome() + ": dados completos");
        }
        ok(Origem.porTexto("Agente de Saúde") == Origem.AGENTE_DE_SAUDE && Origem.porTexto("T.I.") == Origem.TI
                && Origem.porTexto("teorico_da_conspiracao") == Origem.TEORICO_DA_CONSPIRACAO
                && Origem.porTexto("xyz") == null, "porTexto aceita acento, pontos e rejeita o que não existe");

        // --- escolher dá as perícias de graça
        Personagem p = new Personagem(ClasseOP.COMBATENTE);
        int vagas = p.pericias.vagasRestantes();
        ok(p.getOrigem() == null && p.origem(Origem.Efeito.DANO_CORPO) == 0, "começa sem origem e sem bônus");
        ok(p.escolherOrigem(Origem.LUTADOR), "escolhe Lutador");
        ok(p.pericias.estaTreinada(Pericia.LUTA) && p.pericias.estaTreinada(Pericia.REFLEXOS), "Lutador treina Luta e Reflexos");
        ok(p.pericias.vagasRestantes() == vagas, "perícias da origem não gastam vagas");
        ok(!p.escolherOrigem(Origem.MILITAR), "origem só se escolhe uma vez");
        ok(p.danoFixoDeOrigem(TipoDano.CORPO_A_CORPO) == 2 && p.danoFixoDeOrigem(TipoDano.DESARMADO) == 2
                && p.danoFixoDeOrigem(TipoDano.A_DISTANCIA) == 0, "Mão Pesada: +2 só no corpo a corpo");

        // --- se a perícia já estava treinada, a vaga é devolvida
        Personagem q = new Personagem(ClasseOP.COMBATENTE);
        q.pericias.treinar(Pericia.PONTARIA);
        int antes = q.pericias.vagasRestantes();
        q.escolherOrigem(Origem.MILITAR);
        ok(q.pericias.vagasRestantes() == antes + 1, "já treinada antes: a vaga volta");
        ok(q.danoFixoDeOrigem(TipoDano.A_DISTANCIA) == 2, "Para Bellum: +2 à distância");

        // --- vida, PE, crítico, resistência, velocidade
        Personagem d = new Personagem(ClasseOP.COMBATENTE);
        int vida = d.vidaMaxima();
        d.escolherOrigem(Origem.DESGARRADO);
        ok(d.vidaMaxima() == vida + d.nivel(), "Desgarrado: +1 de vida por nível");
        Personagem o1 = new Personagem(ClasseOP.COMBATENTE);
        int vidaO = o1.vidaMaxima();
        o1.escolherOrigem(Origem.OPERARIO);
        ok(o1.vidaMaxima() == vidaO + 4, "Operário: +4 de vida");

        Personagem ex = new Personagem(ClasseOP.COMBATENTE);
        int pe = ex.esforcoMaximo();
        ex.escolherOrigem(Origem.EXECUTIVO);
        ok(ex.esforcoMaximo() == pe + 2 && ex.pe.getMaximo() == pe + 2 && ex.pe.getAtual() == pe + 2, "Executivo: +2 PE máximo e cheio");

        Personagem m = new Personagem(ClasseOP.COMBATENTE);
        double crit = m.chanceCritico(TipoDano.CORPO_A_CORPO);
        m.escolherOrigem(Origem.MERCENARIO);
        ok(Math.abs(m.chanceCritico(TipoDano.CORPO_A_CORPO) - (crit + 0.05)) < 1e-9, "Mercenário: +5% de crítico");

        Personagem t = new Personagem(ClasseOP.COMBATENTE);
        double mental = t.chanceResistencia(true), fisica = t.chanceResistencia(false);
        t.escolherOrigem(Origem.TEORICO_DA_CONSPIRACAO);
        ok(Math.abs(t.chanceResistencia(true) - (mental + 0.10)) < 1e-9
                && Math.abs(t.chanceResistencia(false) - fisica) < 1e-9, "Teórico: +10% só contra efeitos mentais");
        Personagem sv = new Personagem(ClasseOP.COMBATENTE);
        double fis = sv.chanceResistencia(false);
        sv.escolherOrigem(Origem.SERVIDOR_PUBLICO);
        ok(Math.abs(sv.chanceResistencia(false) - (fis + 0.05)) < 1e-9, "Servidor Público: +5% em qualquer efeito");

        Personagem at = new Personagem(ClasseOP.COMBATENTE);
        double vel = at.velocidadeExtra();
        at.escolherOrigem(Origem.ATLETA);
        ok(Math.abs(at.velocidadeExtra() - (vel + 0.05 + 0.03)) < 1e-9, "Atleta: +5% da origem + 3% de Atletismo treinado");

        // --- sanidade
        Personagem cu = new Personagem(ClasseOP.COMBATENTE);
        cu.escolherOrigem(Origem.CULTISTA_ARREPENDIDO);
        ok(cu.perdaDeSanidadeAjustada(3, true, dados(20)) == 0, "Cultista: Vontade -1, Ocultismo -1, origem -1 = 0");
        ok(cu.perdaDeSanidadeAjustada(3, true, dados(2)) == 2, "Cultista: falhou no teste, ainda perde 1 a menos");
        ok(cu.perdaDeSanidadeAjustada(1, false, dados(2)) == 1, "Cultista: susto comum não muda");

        Personagem re = new Personagem(ClasseOP.COMBATENTE);
        re.escolherOrigem(Origem.RELIGIOSO);
        re.setSanidade(re.getSanidade() - 5);
        int san = re.getSanidade();
        for (int i = 0; i < 60; i++) {
            re.passarSegundo(false, false);
        }
        ok(re.getSanidade() == san + 1, "Religioso: +1 SAN a cada 60 s");

        // --- testes de Intelecto
        Personagem ac = new Personagem(ClasseOP.COMBATENTE);
        ac.atributos.set(Atributo.INTELECTO, 2);
        ResultadoTeste sem = ac.pericias.testar(Pericia.MEDICINA, 15, dados(10, 10));
        ac.escolherOrigem(Origem.ACADEMICO);
        ResultadoTeste com = ac.pericias.testar(Pericia.MEDICINA, 15, dados(10, 10));
        ok(com.total() == sem.total() + 3, "Acadêmico: +3 em perícia de Intelecto");
        ResultadoTeste forca = ac.pericias.testar(Pericia.LUTA, 15, dados(10, 10));
        ok(forca.bonus() == 0, "Acadêmico: nada em perícia de outro atributo");

        // --- Vontade treinada pela origem: bônus vale
        Personagem vi = new Personagem(ClasseOP.COMBATENTE);
        vi.escolherOrigem(Origem.VITIMA);
        ok(vi.pericias.bonus(Pericia.VONTADE) == 5 && vi.pericias.bonus(Pericia.REFLEXOS) == 5, "Vítima: Reflexos e Vontade +5");

        // --- origem pode ser melhorada como qualquer perícia
        vi.pericias.concederRetroativo(7);
        ok(vi.pericias.melhorar(Pericia.VONTADE) && vi.pericias.bonus(Pericia.VONTADE) == 10, "perícia da origem vira Veterano");

        // --- restaurar ao carregar o jogo
        Personagem r = new Personagem(ClasseOP.COMBATENTE);
        r.restaurarOrigem(Origem.EXECUTIVO);
        ok(r.getOrigem() == Origem.EXECUTIVO && r.pericias.veioDaOrigem(Pericia.DIPLOMACIA)
                && r.pe.getMaximo() == r.esforcoMaximo(), "restaurar volta a origem sem mudar os graus");

        System.out.println(falhas == 0 ? "TUDO OK" : falhas + " falhas");
    }
}
