package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * As habilidades que um personagem aprendeu na árvore.
 * A cada marco de NEX o personagem ganha pontos de habilidade e gasta esses pontos
 * para aprender habilidades. Versões aprimoradas (evoluções) são liberadas
 * automaticamente, sem gastar pontos.
 */
public class HabilidadesDoPersonagem {
    public static final int CUSTO_EM_PONTOS = 1;
    public static final int PONTOS_POR_NIVEL = 1;

    private final ClasseOP classe;
    private final Atributos atributos; // os atributos do personagem (usados nos requisitos)
    private final List<Habilidade> aprendidas = new ArrayList<>();
    private int pontos = PONTOS_POR_NIVEL; // o ponto do nível 1
    private Trilha trilha; // a trilha escolhida (null até o NEX 10%)
    private final Map<String, Integer> repeticoes = new HashMap<>(); // habilidades repetíveis: quantas vezes foram aprendidas

    public HabilidadesDoPersonagem(ClasseOP classe, Atributos atributos) {
        this.classe = classe;
        this.atributos = atributos;
    }

    public int getPontos() {
        return pontos;
    }

    /**
     * Chame quando o personagem chegar a um marco de NEX (passe o nível novo).
     * Dá os pontos de habilidade e libera as versões aprimoradas.
     * Retorna as habilidades liberadas automaticamente agora.
     */
    public List<Habilidade> aoSubirDeNivel(int nivel) {
        pontos += PONTOS_POR_NIVEL;
        return liberarAutomaticas(nivel);
    }

    public Trilha getTrilha() {
        return trilha;
    }

    /** Pode escolher essa trilha? Só uma vez, da própria classe, a partir do NEX 10%. */
    public boolean podeEscolherTrilha(Trilha nova, int nivel) {
        return trilha == null
                && nova != null
                && nova.getClasse() == classe
                && nivel >= Trilha.NIVEL_MINIMO;
    }

    /**
     * Escolhe a trilha (não dá para trocar depois) e já libera as habilidades dela
     * que o NEX atual permite. Não gasta pontos. Retorna false se não puder.
     */
    public boolean escolherTrilha(Trilha nova, int nivel) {
        if (!podeEscolherTrilha(nova, nivel)) {
            return false;
        }
        trilha = nova;
        liberarAutomaticas(nivel);
        return true;
    }

    public boolean temAprendida(Habilidade habilidade) {
        return aprendidas.contains(habilidade);
    }

    /** Quantas vezes uma habilidade repetível foi aprendida (0 se nunca; 1 para as comuns já aprendidas). */
    public int vezesAprendida(Habilidade habilidade) {
        if (!temAprendida(habilidade)) {
            return 0;
        }
        return Math.max(1, repeticoes.getOrDefault(habilidade.getId(), 1));
    }

    /** Confere se todos os pré-requisitos da habilidade já foram aprendidos. */
    private boolean temPrerequisitos(Habilidade habilidade) {
        for (String id : habilidade.getPrerequisitos()) {
            Habilidade requisito = CatalogoHabilidades.porId(id);
            if (requisito == null || !aprendidas.contains(requisito)) {
                return false;
            }
        }
        return true;
    }

    /** Os atributos do personagem atendem aos requisitos da habilidade? */
    private boolean temAtributos(Habilidade habilidade) {
        for (var requisito : habilidade.getRequisitosAtributo().entrySet()) {
            if (atributos.get(requisito.getKey()) < requisito.getValue()) {
                return false;
            }
        }
        return true;
    }

    /** O personagem tem proficiência com isso (por alguma habilidade aprendida)? */
    public boolean temProficiencia(Proficiencia proficiencia) {
        for (Habilidade habilidade : aprendidas) {
            if (habilidade.getProficiencia() == proficiencia) {
                return true;
            }
        }
        return false;
    }

    /** A versão anterior de uma versão aprimorada já foi aprendida? */
    private boolean temVersaoAnterior(Habilidade habilidade) {
        Habilidade anterior = CatalogoHabilidades.porId(habilidade.getEvolucaoDe());
        return anterior != null && aprendidas.contains(anterior);
    }

    /**
     * Pode aprender? Precisa ser da classe, nova, ter nível, pontos, pré-requisitos
     * e os atributos mínimos que a habilidade exigir.
     * Versões aprimoradas não são aprendidas com pontos: são liberadas automaticamente.
     */
    public boolean podeAprender(Habilidade habilidade, int nivel) {
        return habilidade.getClasse() == classe
                && !habilidade.isEvolucao()
                && !habilidade.isDeTrilha()
                && (!temAprendida(habilidade) || habilidade.isRepetivel())
                && nivel >= habilidade.getNivelMinimo()
                && pontos >= CUSTO_EM_PONTOS
                && temPrerequisitos(habilidade)
                && temAtributos(habilidade);
    }

    /**
     * Por que ainda não dá para aprender essa habilidade? Devolve um texto curto para a tela
     * (ex.: "Requer NEX 25%") ou null se já dá para aprender agora.
     */
    public String motivoBloqueio(Habilidade habilidade, int nivel) {
        if (habilidade.getClasse() != classe || habilidade.isEvolucao() || habilidade.isDeTrilha()) {
            return "Não disponível";
        }
        if (temAprendida(habilidade) && !habilidade.isRepetivel()) {
            return "Já aprendida";
        }
        if (nivel < habilidade.getNivelMinimo()) {
            return "Requer NEX " + nexDoNivel(habilidade.getNivelMinimo()) + "%";
        }
        for (String id : habilidade.getPrerequisitos()) {
            Habilidade requisito = CatalogoHabilidades.porId(id);
            if (requisito == null || !aprendidas.contains(requisito)) {
                return "Requer " + (requisito == null ? id : requisito.getNome());
            }
        }
        for (var requisito : habilidade.getRequisitosAtributo().entrySet()) {
            if (atributos.get(requisito.getKey()) < requisito.getValue()) {
                return "Requer " + requisito.getKey().getSigla() + " " + requisito.getValue();
            }
        }
        if (pontos < CUSTO_EM_PONTOS) {
            return "Sem pontos de habilidade";
        }
        return null;
    }

    /** NEX (%) em que um nível é atingido: 5% por nível, e 99% no nível 20. */
    public static int nexDoNivel(int nivel) {
        return nivel >= NivelExposicao.NIVEL_MAXIMO ? NivelExposicao.NEX_MAXIMO : nivel * NivelExposicao.PASSO_MARCO;
    }

    /** Aprende a habilidade, gastando pontos. Retorna false se não puder. */
    public boolean aprender(Habilidade habilidade, int nivel) {
        if (!podeAprender(habilidade, nivel)) {
            return false;
        }
        pontos -= CUSTO_EM_PONTOS;
        if (temAprendida(habilidade)) { // repetível: só conta mais uma vez
            repeticoes.put(habilidade.getId(), vezesAprendida(habilidade) + 1);
            return true;
        }
        aprendidas.add(habilidade);
        if (habilidade.isRepetivel()) {
            repeticoes.put(habilidade.getId(), 1);
        }
        liberarAutomaticas(nivel); // se o NEX já for alto, libera as versões aprimoradas
        return true;
    }

    /**
     * Libera sozinhas: as versões aprimoradas cuja versão anterior já foi aprendida e as
     * habilidades da trilha escolhida, desde que o NEX já tenha sido atingido.
     * Retorna as que foram liberadas agora.
     */
    public List<Habilidade> liberarAutomaticas(int nivel) {
        List<Habilidade> novas = new ArrayList<>();
        boolean mudou = true;
        while (mudou) { // repete: uma liberação pode abrir a próxima versão
            mudou = false;
            for (Habilidade habilidade : CatalogoHabilidades.daClasse(classe)) {
                boolean liberavelSozinha = habilidade.isEvolucao() || habilidade.isDeTrilha();
                if (liberavelSozinha
                        && !temAprendida(habilidade)
                        && nivel >= habilidade.getNivelMinimo()
                        && (!habilidade.isEvolucao() || temVersaoAnterior(habilidade))
                        && (!habilidade.isDeTrilha() || habilidade.getTrilha() == trilha)
                        && temPrerequisitos(habilidade)) {
                    aprendidas.add(habilidade);
                    novas.add(habilidade);
                    mudou = true;
                }
            }
        }
        return novas;
    }

    /** Uma versão aprimorada já aprendida tomou o lugar desta habilidade? */
    private boolean estaSubstituida(Habilidade habilidade) {
        for (Habilidade aprendida : aprendidas) {
            if (habilidade.getId().equals(aprendida.getEvolucaoDe())) {
                return true;
            }
        }
        return false;
    }

    /** A versão mais forte já aprendida de uma habilidade (ela mesma, se não evoluiu). */
    public Habilidade melhorVersao(Habilidade habilidade) {
        Habilidade atual = habilidade;
        boolean achou = true;
        while (achou) {
            achou = false;
            for (Habilidade aprendida : aprendidas) {
                if (atual.getId().equals(aprendida.getEvolucaoDe())) {
                    atual = aprendida;
                    achou = true;
                    break;
                }
            }
        }
        return atual;
    }

    /** A primeira versão da cadeia (ela mesma, se não for uma versão aprimorada). */
    private Habilidade raiz(Habilidade habilidade) {
        Habilidade atual = habilidade;
        while (atual.getEvolucaoDe() != null) {
            Habilidade anterior = CatalogoHabilidades.porId(atual.getEvolucaoDe());
            if (anterior == null) {
                break;
            }
            atual = anterior;
        }
        return atual;
    }

    /** As habilidades que o personagem pode aprender agora. */
    public List<Habilidade> disponiveis(int nivel) {
        return CatalogoHabilidades.daClasse(classe).stream()
                .filter(h -> podeAprender(h, nivel))
                .toList();
    }

    /** Tudo o que foi aprendido, inclusive as versões antigas. */
    public List<Habilidade> getAprendidas() {
        return List.copyOf(aprendidas);
    }

    /** Só as passivas em uso (sem as versões já substituídas). */
    public List<Habilidade> getPassivas() {
        return aprendidas.stream()
                .filter(h -> h.isPassiva() && !estaSubstituida(h))
                .toList();
    }

    /**
     * Bônus de dano permanente das passivas em uso para um tipo de dano.
     * Versões substituídas não contam, então as evoluções não acumulam com as antigas.
     */
    public double bonusDanoPassivo(TipoDano tipo) {
        double total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.bonusDanoPassivoPara(tipo);
        }
        return total;
    }

    /** Multiplicador de dano das passivas em uso para um tipo de dano (1.35 = +35%; 1 se nada afeta). */
    public double multiplicadorDano(TipoDano tipo) {
        double aumento = 0;
        for (Habilidade passiva : getPassivas()) {
            aumento += passiva.aumentoPercentualPara(tipo);
        }
        return 1 + aumento;
    }

    /**
     * Dano final de um ataque. Passe o dano base já com o bônus de atributos (Força).
     * Ordem: multiplica o dano base pelas passivas em %, depois soma os bônus fixos
     * (passivas e efeitos ativos como o Ataque Especial).
     */
    public double danoFinal(double danoBase, TipoDano tipo, EfeitosAtivos efeitos) {
        return danoBase * multiplicadorDano(tipo) + bonusDanoPassivo(tipo) + efeitos.bonusDano(tipo);
    }

    /** Vida extra das passivas em uso para esse nível (2 pontos = 1 coração). */
    public int bonusVida(int nivel) {
        int total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.bonusVidaPara(nivel);
        }
        return total;
    }

    /** Dano descontado de cada golpe recebido pelas passivas em uso. */
    public double reducaoDanoRecebido(boolean usandoProtecaoPesada) {
        double total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.reducaoDanoRecebidoPara(usandoProtecaoPesada);
        }
        return total;
    }

    /** Dano que realmente entra depois das reduções (nunca abaixo de 0). */
    public double danoRecebidoFinal(double dano, boolean usandoProtecaoPesada) {
        return Math.max(0, dano - reducaoDanoRecebido(usandoProtecaoPesada));
    }

    /** Chance de acerto crítico das passivas em uso para um tipo de dano (0.10 = 10%, máx. 100%). */
    public double chanceCritico(TipoDano tipo) {
        double total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.chanceCriticoPara(tipo);
        }
        return Math.min(1.0, total);
    }

    /** Bônus de velocidade de movimento das passivas em uso (0.06 = +6%). */
    public double bonusVelocidade() {
        double total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.bonusVelocidadePassivo();
        }
        return total;
    }

    /** Só as ativas em uso (sem as versões já substituídas). */
    public List<Habilidade> getAtivas() {
        return aprendidas.stream()
                .filter(h -> h.isAtiva() && !estaSubstituida(h))
                .toList();
    }

    /**
     * Tenta usar uma habilidade ativa: usa sempre a melhor versão aprendida,
     * gasta o PE dela e, se tiver efeito temporário, ativa esse efeito.
     * Retorna false se não estiver aprendida, não for ativa ou faltar PE
     * (nesse caso nada é gasto).
     */
    public boolean usar(Habilidade habilidade, PontosEsforco pe, EfeitosAtivos efeitos) {
        if (!temAprendida(habilidade)) {
            return false;
        }
        Habilidade efetiva = melhorVersao(habilidade);
        if (!efetiva.isAtiva()) {
            return false;
        }
        if (!pe.gastar(efetiva.getCustoPe())) {
            return false;
        }
        // o id do efeito é o da primeira versão, então usar de novo só reinicia o tempo;
        // os valores que dependem de atributo usam os atributos do momento do uso
        efetiva.aplicarEfeitosAoUsar(raiz(efetiva).getId(), atributos, efeitos);
        return true;
    }

    /** Bônus de defesa das passivas em uso, sem contar bônus de proteção pesada. */
    public int bonusDefesa() {
        return bonusDefesa(false);
    }

    /**
     * Bônus de defesa das passivas em uso (versões substituídas não contam).
     * Passe true em usandoProtecaoPesada se o personagem está com proteção pesada equipada.
     */
    public int bonusDefesa(boolean usandoProtecaoPesada) {
        int total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.bonusDefesaPara(usandoProtecaoPesada);
        }
        return total;
    }

    /** Chance de resistir a efeitos das passivas em uso, sem contar bônus de proteção pesada. */
    public double chanceResistirEfeitos() {
        return chanceResistirEfeitos(false);
    }

    /**
     * Chance total de resistir a efeitos, das passivas em uso (0.05 = 5%), no máximo 100%.
     * Passe true em usandoProtecaoPesada se o personagem está com proteção pesada equipada.
     * Para sortear se o personagem resistiu a um efeito, use resistiuAoEfeito().
     */
    public double chanceResistirEfeitos(boolean usandoProtecaoPesada) {
        double total = 0;
        for (Habilidade passiva : getPassivas()) {
            total += passiva.chanceResistirPara(usandoProtecaoPesada);
        }
        return Math.min(1.0, total);
    }

    /** Defesa total (passivas + efeitos ativos), sem proteção pesada. */
    public int defesaTotal(EfeitosAtivos efeitos) {
        return defesaTotal(efeitos, false);
    }

    /** Defesa total: passivas em uso + efeitos temporários ativos. */
    public int defesaTotal(EfeitosAtivos efeitos, boolean usandoProtecaoPesada) {
        return bonusDefesa(usandoProtecaoPesada) + efeitos.bonusDefesa();
    }

    /** Chance total de resistir a efeitos (passivas + efeitos ativos), sem proteção pesada. */
    public double chanceResistirEfeitosTotal(EfeitosAtivos efeitos) {
        return chanceResistirEfeitosTotal(efeitos, false);
    }

    /** Chance total de resistir a efeitos (0.20 = 20%): passivas + efeitos ativos, no máximo 100%. */
    public double chanceResistirEfeitosTotal(EfeitosAtivos efeitos, boolean usandoProtecaoPesada) {
        return Math.min(1.0, chanceResistirEfeitos(usandoProtecaoPesada) + efeitos.chanceResistirEfeitos());
    }

    /**
     * Sorteia se o personagem resistiu a um efeito negativo (veneno, medo, etc.).
     * Chame isto quando um efeito tentar atingir o personagem: a chance usada é a
     * total (passivas + efeitos ativos). Passe o Random do jogo (ou um fixo, nos testes).
     */
    public boolean resistiuAoEfeito(EfeitosAtivos efeitos, boolean usandoProtecaoPesada, Random random) {
        return random.nextDouble() < chanceResistirEfeitosTotal(efeitos, usandoProtecaoPesada);
    }
    public void restaurar(int pontos, Trilha trilha, List<Habilidade> lista) {
        this.pontos = pontos;
        this.trilha = trilha;
        aprendidas.clear();
        aprendidas.addAll(lista);
        repeticoes.clear();
    }

    /** Ao carregar o jogo salvo: quantas vezes cada habilidade repetível foi aprendida. */
    public void restaurarRepeticoes(Map<String, Integer> salvas) {
        repeticoes.clear();
        for (Map.Entry<String, Integer> e : salvas.entrySet()) {
            repeticoes.put(e.getKey(), Math.max(1, e.getValue()));
        }
    }

    /** As repetições guardadas (para salvar o jogo). */
    public Map<String, Integer> getRepeticoes() {
        return Map.copyOf(repeticoes);
    }
}
