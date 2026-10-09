package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * As perícias de um personagem: grau de cada uma, vagas de treino, melhorias de grau
 * e a rolagem dos testes. Java puro, sem nada do Minecraft aqui.
 *
 * Regras (ajuste as constantes para a sua mesa):
 *  - Teste: rola 1d20 por ponto do atributo e fica com o maior (atributo 0: rola 2 e fica com o menor),
 *    soma o bônus do grau e compara com a DT. 20 natural passa sozinho.
 *  - Vagas de treino: base da classe + Intelecto (Combatente 3, Ocultista 5, Especialista 7).
 *  - NEX 35% (nível 7): 2 + Intelecto perícias podem ir a Veterano. NEX 70% (nível 14): 2 + Intelecto podem ir a Expert.
 */
public class Pericias {
    public static final int DT_FACIL = 10;
    public static final int DT_MEDIA = 15;
    public static final int DT_DIFICIL = 20;
    public static final int DT_MUITO_DIFICIL = 25;

    /** 20 natural é sucesso automático. */
    public static final boolean CRITICO_AUTOMATICO = true;

    public static final int NIVEL_VETERANO = 7;
    public static final int NIVEL_EXPERT = 14;
    /** Melhorias de grau por marco: este número + o Intelecto do personagem no momento do marco. */
    public static final int MELHORIAS_POR_MARCO = 2;

    // Os números de dano, crítico, resistência etc. por patamar ficam em Beneficio.java.

    private final ClasseOP classe;
    private final Atributos atributos;
    private final EnumMap<Pericia, GrauTreinamento> graus = new EnumMap<>(Pericia.class);
    /** Perícias treinadas de graça pela origem: não gastam vaga de treino. */
    private final Set<Pericia> deOrigem = EnumSet.noneOf(Pericia.class);
    private Origem origem;
    private int melhoriasVeterano = 0;
    private int melhoriasExpert = 0;
    /** Perícias treinadas por poder (Treinamento em Perícia): não gastam vaga de treino. */
    private int treinosExtras = 0;

    public Pericias(ClasseOP classe, Atributos atributos) {
        this.classe = classe;
        this.atributos = atributos;
    }

    // ------------------------------------------------------------------ graus e vagas

    public GrauTreinamento getGrau(Pericia pericia) {
        return graus.getOrDefault(pericia, GrauTreinamento.DESTREINADO);
    }

    public boolean estaTreinada(Pericia pericia) {
        return getGrau(pericia) != GrauTreinamento.DESTREINADO;
    }

    /** Bônus de teste da perícia (0, 5, 10 ou 15). */
    public int bonus(Pericia pericia) {
        return getGrau(pericia).getBonus();
    }

    /** Quantas perícias o personagem pode ter treinadas: base da classe + Intelecto. */
    public int vagasDeTreino() {
        int base = switch (classe) {
            case COMBATENTE -> 3;
            case ESPECIALISTA -> 7;
            case OCULTISTA -> 5;
        };
        return base + atributos.get(Atributo.INTELECTO);
    }

    public int treinadas() {
        int total = 0;
        for (GrauTreinamento g : graus.values()) {
            if (g != GrauTreinamento.DESTREINADO) {
                total++;
            }
        }
        return total;
    }

    /** Vagas livres: as perícias da origem não contam. */
    public int vagasRestantes() {
        int gastas = 0;
        for (Map.Entry<Pericia, GrauTreinamento> entrada : graus.entrySet()) {
            if (entrada.getValue() != GrauTreinamento.DESTREINADO && !deOrigem.contains(entrada.getKey())) {
                gastas++;
            }
        }
        return Math.max(0, vagasDeTreino() + treinosExtras - gastas);
    }

    /** Quantas perícias foram treinadas por poder (somam às vagas de treino). */
    public int getTreinosExtras() {
        return treinosExtras;
    }

    public void setTreinosExtras(int valor) {
        treinosExtras = Math.max(0, valor);
    }

    /** Quantas melhorias de grau este personagem ganha em cada marco (2 + Intelecto). */
    public int melhoriasPorMarco() {
        return MELHORIAS_POR_MARCO + Math.max(0, atributos.get(Atributo.INTELECTO));
    }

    /**
     * O poder Treinamento em Perícia pode subir essa perícia um grau agora?
     * Destreinada → Treinada; Treinada → Veterana a partir do NEX 35%; Veterana → Expert a partir do NEX 70%.
     */
    public boolean podeSubirPorPoder(Pericia pericia, int nivel) {
        return switch (getGrau(pericia)) {
            case DESTREINADO -> true;
            case TREINADO -> nivel >= NIVEL_VETERANO;
            case VETERANO -> nivel >= NIVEL_EXPERT;
            case EXPERT -> false;
        };
    }

    /** Sobe a perícia um grau pelo poder Treinamento em Perícia. Retorna false se não puder. */
    public boolean subirPorPoder(Pericia pericia, int nivel) {
        if (!podeSubirPorPoder(pericia, nivel)) {
            return false;
        }
        GrauTreinamento atual = getGrau(pericia);
        if (atual == GrauTreinamento.DESTREINADO) {
            treinosExtras++; // a vaga extra compensa a que esta perícia passa a ocupar
        }
        graus.put(pericia, atual.proximo());
        return true;
    }

    public Origem getOrigem() {
        return origem;
    }

    /** Escolhe a origem: suas 2 perícias viram Treinado (se já eram mais que isso, mantém) e ficam de graça. */
    public void definirOrigem(Origem nova) {
        origem = nova;
        deOrigem.clear();
        for (Pericia pericia : nova.getPericias()) {
            deOrigem.add(pericia);
            if (!estaTreinada(pericia)) {
                graus.put(pericia, GrauTreinamento.TREINADO);
            }
        }
    }

    /** Ao carregar o jogo salvo: só marca as perícias como de origem, sem mexer nos graus. */
    public void restaurarOrigem(Origem salva) {
        origem = salva;
        deOrigem.clear();
        if (salva != null) {
            deOrigem.addAll(salva.getPericias());
        }
    }

    public boolean veioDaOrigem(Pericia pericia) {
        return deOrigem.contains(pericia);
    }

    public int getMelhoriasVeterano() {
        return melhoriasVeterano;
    }

    public int getMelhoriasExpert() {
        return melhoriasExpert;
    }

    /** Gasta uma vaga para treinar a perícia (Destreinado → Treinado). */
    public boolean treinar(Pericia pericia) {
        if (estaTreinada(pericia) || vagasRestantes() <= 0) {
            return false;
        }
        graus.put(pericia, GrauTreinamento.TREINADO);
        return true;
    }

    /** Gasta uma melhoria: Treinado → Veterano (marco 35%) ou Veterano → Expert (marco 70%). */
    public boolean melhorar(Pericia pericia) {
        GrauTreinamento atual = getGrau(pericia);
        if (atual == GrauTreinamento.TREINADO && melhoriasVeterano > 0) {
            melhoriasVeterano--;
            graus.put(pericia, GrauTreinamento.VETERANO);
            return true;
        }
        if (atual == GrauTreinamento.VETERANO && melhoriasExpert > 0) {
            melhoriasExpert--;
            graus.put(pericia, GrauTreinamento.EXPERT);
            return true;
        }
        return false;
    }

    /** Chame a cada nível novo: nos marcos de 35% e 70% liberam melhorias de grau. */
    public void aoSubirDeNivel(int nivel) {
        if (nivel == NIVEL_VETERANO) {
            melhoriasVeterano += melhoriasPorMarco();
        } else if (nivel == NIVEL_EXPERT) {
            melhoriasExpert += melhoriasPorMarco();
        }
    }

    /** Para saves antigos, feitos antes das perícias existirem: dá as melhorias dos marcos já passados. */
    public void concederRetroativo(int nivelAtual) {
        if (nivelAtual >= NIVEL_VETERANO) {
            melhoriasVeterano += melhoriasPorMarco();
        }
        if (nivelAtual >= NIVEL_EXPERT) {
            melhoriasExpert += melhoriasPorMarco();
        }
    }

    /** Usado ao carregar o jogo salvo. */
    public void restaurar(Map<Pericia, GrauTreinamento> salvos, int melhoriasVeterano, int melhoriasExpert) {
        graus.clear();
        graus.putAll(salvos);
        this.melhoriasVeterano = Math.max(0, melhoriasVeterano);
        this.melhoriasExpert = Math.max(0, melhoriasExpert);
    }

    // ------------------------------------------------------------------ testes

    /**
     * O bônus de um teste separado por fonte (grau de treinamento, origem, extra), para a tela do dado somar um por um.
     * A soma das partes é sempre igual ao bônus usado em {@link #testar}.
     */
    public List<ParteBonus> partesDoBonus(Pericia pericia, int bonusExtra, String rotuloExtra) {
        List<ParteBonus> partes = new ArrayList<>();
        GrauTreinamento grau = getGrau(pericia);
        if (grau.getBonus() != 0) {
            partes.add(new ParteBonus(grau.getNome(), grau.getBonus()));
        }
        int daOrigem = origem == null ? 0 : origem.bonusTeste(pericia.getAtributo());
        if (daOrigem != 0) {
            partes.add(new ParteBonus(origem.getNome(), daOrigem));
        }
        if (bonusExtra != 0) {
            partes.add(new ParteBonus(rotuloExtra == null || rotuloExtra.isBlank() ? "Bônus" : rotuloExtra, bonusExtra));
        }
        return partes;
    }

    public ResultadoTeste testar(Pericia pericia, int dt, Random random) {
        return testar(pericia, dt, 0, random);
    }

    /** Rola o teste da perícia contra a DT. bonusExtra serve para situações (+5 de vantagem, -5 de pressa...). */
    public ResultadoTeste testar(Pericia pericia, int dt, int bonusExtra, Random random) {
        int valor = atributos.get(pericia.getAtributo());
        // atributo 0: 2 dados, fica com o menor; atributo N (1 ou mais): N dados, fica com o maior
        int quantidade = valor == 0 ? 2 : valor;
        List<Integer> dados = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            dados.add(random.nextInt(20) + 1);
        }
        int escolhido = valor == 0 ? Collections.min(dados) : Collections.max(dados);
        int bonus = bonus(pericia) + bonusExtra + (origem == null ? 0 : origem.bonusTeste(pericia.getAtributo()));
        int total = escolhido + bonus;
        boolean critico = CRITICO_AUTOMATICO && escolhido == 20;
        boolean sucesso = critico || total >= dt;
        return new ResultadoTeste(pericia, List.copyOf(dados), escolhido, bonus, total, dt, sucesso, critico);
    }

    // ------------------------------------------------------------------ benefícios por patamar

    /** Valor de um benefício para o grau atual da perícia dele (0 se destreinada). */
    public double valor(Beneficio beneficio) {
        return beneficio.valor(getGrau(beneficio.getPericia()));
    }

    // ------------------------------------------------------------------ efeitos no combate

    /** Multiplicador de dano de Luta (corpo a corpo e desarmado) ou Pontaria (à distância): 1.0, 1.10, 1.20... */
    public double multiplicadorDano(TipoDano tipo) {
        return 1.0 + switch (tipo) {
            case CORPO_A_CORPO, DESARMADO -> valor(Beneficio.LUTA_DANO);
            case A_DISTANCIA -> valor(Beneficio.PONTARIA_DANO);
            case OUTRO -> 0;
        };
    }

    /** Chance extra de crítico vinda de Luta ou Pontaria. */
    public double bonusCritico(TipoDano tipo) {
        return switch (tipo) {
            case CORPO_A_CORPO, DESARMADO -> valor(Beneficio.LUTA_CRITICO);
            case A_DISTANCIA -> valor(Beneficio.PONTARIA_CRITICO);
            case OUTRO -> 0;
        };
    }

    /** Soma ao multiplicador de crítico (Investigação: achar o ponto fraco). */
    public double bonusMultiplicadorCritico() {
        return valor(Beneficio.INVESTIGACAO_CRITICO);
    }

    /** Chance extra de resistir a efeitos: Vontade (mentais) ou Fortitude (físicos). */
    public double bonusResistencia(boolean mental) {
        return mental ? valor(Beneficio.VONTADE_RESISTENCIA) : valor(Beneficio.FORTITUDE_RESISTENCIA);
    }
}
