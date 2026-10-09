package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Uma habilidade (poder de classe) na árvore de habilidades.
 * Guarda os dados básicos dela (id, nome, descrição, tipo, classe, nível de NEX mínimo,
 * custo de PE e pré-requisitos) e duas listas de efeitos:
 * - efeitos passivos (EfeitoPassivo): valem o tempo todo, enquanto a habilidade estiver em uso;
 * - efeitos ao usar (EfeitoAoUsar): acontecem quando uma habilidade ativa é usada.
 * Habilidade "solta" não tem pré-requisitos.
 */
public class Habilidade {
    private final String id;
    private final String nome;
    private final String descricao;
    private final TipoHabilidade tipo;
    private final ClasseOP classe;
    private final int nivelMinimo;
    private final int custoPe;
    private final List<String> prerequisitos;
    private String evolucaoDe; // opcional: id da versão anterior, se for uma versão aprimorada
    private final Map<Atributo, Integer> requisitosAtributo = new EnumMap<>(Atributo.class); // opcional
    private Proficiencia proficiencia; // opcional: proficiência concedida ao aprender
    private Trilha trilha; // opcional: se preenchida, é habilidade de trilha (liberada sozinha)
    private boolean repetivel; // opcional: pode ser aprendida várias vezes (cada vez custa 1 ponto)
    private final List<EfeitoPassivo> efeitosPassivos = new ArrayList<>();
    private final List<EfeitoAoUsar> efeitosAoUsar = new ArrayList<>();

    /** Habilidade com pré-requisitos: ids das habilidades que precisam ser aprendidas antes. */
    public Habilidade(String id, String nome, String descricao, TipoHabilidade tipo,
                      ClasseOP classe, int nivelMinimo, int custoPe, List<String> prerequisitos) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.tipo = tipo;
        this.classe = classe;
        this.nivelMinimo = nivelMinimo;
        // Habilidade passiva nunca custa PE.
        this.custoPe = tipo == TipoHabilidade.PASSIVA ? 0 : Math.max(0, custoPe);
        this.prerequisitos = List.copyOf(prerequisitos);
    }

    /** Habilidade solta, sem pré-requisitos. */
    public Habilidade(String id, String nome, String descricao, TipoHabilidade tipo,
                      ClasseOP classe, int nivelMinimo, int custoPe) {
        this(id, nome, descricao, tipo, classe, nivelMinimo, custoPe, List.of());
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public TipoHabilidade getTipo() {
        return tipo;
    }

    public ClasseOP getClasse() {
        return classe;
    }

    public int getNivelMinimo() {
        return nivelMinimo;
    }

    public int getCustoPe() {
        return custoPe;
    }

    public List<String> getPrerequisitos() {
        return prerequisitos;
    }

    public boolean isSolta() {
        return prerequisitos.isEmpty();
    }

    public boolean isAtiva() {
        return tipo == TipoHabilidade.ATIVA;
    }

    public boolean isPassiva() {
        return tipo == TipoHabilidade.PASSIVA;
    }

    // ---------------------------------------------------------------- evolução, requisitos, proficiência

    /**
     * Marca esta habilidade como versão aprimorada de outra (pelo id).
     * Versões aprimoradas são liberadas automaticamente quando a classe e o NEX
     * batem e a versão anterior já foi aprendida. Retorna a própria habilidade.
     */
    public Habilidade comEvolucaoDe(String idAnterior) {
        this.evolucaoDe = idAnterior;
        return this;
    }

    /** O id da versão anterior, ou null se não é uma versão aprimorada. */
    public String getEvolucaoDe() {
        return evolucaoDe;
    }

    public boolean isEvolucao() {
        return evolucaoDe != null;
    }

    /** Exige um valor mínimo de um atributo para aprender. Retorna a própria habilidade. */
    public Habilidade comRequisitoAtributo(Atributo atributo, int minimo) {
        requisitosAtributo.put(atributo, minimo);
        return this;
    }

    /** Os requisitos de atributo (vazio se não tem). */
    public Map<Atributo, Integer> getRequisitosAtributo() {
        return Map.copyOf(requisitosAtributo);
    }

    /** Define a proficiência que a habilidade concede. Retorna a própria habilidade. */
    public Habilidade concedeProficiencia(Proficiencia proficiencia) {
        this.proficiencia = proficiencia;
        return this;
    }

    /** A proficiência concedida, ou null se não concede nenhuma. */
    public Proficiencia getProficiencia() {
        return proficiencia;
    }

    // ---------------------------------------------------------------- repetível

    /** Marca esta habilidade como repetível: dá para aprender várias vezes. Retorna a própria habilidade. */
    public Habilidade repetivel() {
        this.repetivel = true;
        return this;
    }

    public boolean isRepetivel() {
        return repetivel;
    }

    // ---------------------------------------------------------------- trilha

    /**
     * Marca esta habilidade como parte de uma trilha. Habilidades de trilha não são
     * aprendidas com pontos: são liberadas sozinhas quando o personagem escolheu a trilha
     * e atingiu o NEX. Retorna a própria habilidade.
     */
    public Habilidade daTrilha(Trilha trilha) {
        this.trilha = trilha;
        return this;
    }

    /** A trilha da habilidade, ou null se é uma habilidade comum da classe. */
    public Trilha getTrilha() {
        return trilha;
    }

    public boolean isDeTrilha() {
        return trilha != null;
    }

    // ---------------------------------------------------------------- efeitos passivos

    /** Adiciona um efeito passivo qualquer. Retorna a própria habilidade. */
    public Habilidade comEfeitoPassivo(EfeitoPassivo efeito) {
        efeitosPassivos.add(efeito);
        return this;
    }

    /** Dá um bônus de dano permanente a certos tipos de dano. Retorna a própria habilidade. */
    public Habilidade comBonusDanoPassivo(double bonus, Set<TipoDano> tipos) {
        return comEfeitoPassivo(new EfeitoPassivo.BonusDano(bonus, tipos));
    }

    /** Aumenta em % o dano de certos tipos, de forma permanente (ex.: 35 = +35%). */
    public Habilidade comAumentoPercentualDano(double percentual, Set<TipoDano> tipos) {
        return comEfeitoPassivo(new EfeitoPassivo.AumentoPercentualDano(percentual / 100.0, tipos));
    }

    /** Dá um bônus de defesa permanente. Retorna a própria habilidade. */
    public Habilidade comBonusDefesaPassivo(int bonus) {
        return comEfeitoPassivo(new EfeitoPassivo.BonusDefesa(bonus, false));
    }

    /** Dá uma chance permanente de resistir a efeitos (ex.: 5 = 5%). Retorna a própria habilidade. */
    public Habilidade comChanceResistirEfeitos(double percentual) {
        return comEfeitoPassivo(new EfeitoPassivo.ChanceResistir(percentual / 100.0, false));
    }

    /**
     * Bônus extra de defesa e de resistência (em %) que valem só enquanto o personagem
     * está usando proteção pesada. Retorna a própria habilidade.
     */
    public Habilidade comBonusComProtecaoPesada(int bonusDefesa, double percentualResistencia) {
        comEfeitoPassivo(new EfeitoPassivo.BonusDefesa(bonusDefesa, true));
        return comEfeitoPassivo(new EfeitoPassivo.ChanceResistir(percentualResistencia / 100.0, true));
    }

    /** Vida máxima extra por nível de NEX (2 pontos = 1 coração). */
    public Habilidade comBonusVidaPorNivel(int pontosPorNivel) {
        return comEfeitoPassivo(new EfeitoPassivo.BonusVidaPorNivel(pontosPorNivel));
    }

    /** Desconta pontos de cada golpe recebido (sempre, ou só com proteção pesada). */
    public Habilidade comReducaoDanoRecebido(double pontos, boolean soComProtecaoPesada) {
        return comEfeitoPassivo(new EfeitoPassivo.ReducaoDanoRecebido(pontos, soComProtecaoPesada));
    }

    /** Chance de crítico em % para certos tipos de dano (ex.: 10 = 10%). */
    public Habilidade comChanceCritico(double percentual, Set<TipoDano> tipos) {
        return comEfeitoPassivo(new EfeitoPassivo.ChanceCritico(percentual / 100.0, tipos));
    }

    /** Aumenta a velocidade de movimento em % (ex.: 6 = +6%). */
    public Habilidade comBonusVelocidade(double percentual) {
        return comEfeitoPassivo(new EfeitoPassivo.BonusVelocidade(percentual / 100.0));
    }

    /** A vida extra que esta habilidade dá para esse nível. */
    public int bonusVidaPara(int nivel) {
        return efeitosPassivos.stream().mapToInt(e -> e.bonusVida(nivel)).sum();
    }

    /** O dano descontado por golpe recebido (a parte de proteção pesada só conta se estiver usando). */
    public double reducaoDanoRecebidoPara(boolean usandoProtecaoPesada) {
        return efeitosPassivos.stream().mapToDouble(e -> e.reducaoDanoRecebido(usandoProtecaoPesada)).sum();
    }

    /** A chance de crítico desta habilidade para um tipo de dano (0.10 = 10%). */
    public double chanceCriticoPara(TipoDano tipo) {
        return efeitosPassivos.stream().mapToDouble(e -> e.chanceCritico(tipo)).sum();
    }

    /** O bônus de velocidade desta habilidade (0.06 = +6%). */
    public double bonusVelocidadePassivo() {
        return efeitosPassivos.stream().mapToDouble(EfeitoPassivo::bonusVelocidade).sum();
    }

    /** O bônus de dano permanente desta habilidade para um tipo de dano (0 se não afeta). */
    public double bonusDanoPassivoPara(TipoDano tipo) {
        return efeitosPassivos.stream().mapToDouble(e -> e.bonusDano(tipo)).sum();
    }

    /** O aumento percentual desta habilidade para um tipo de dano (0.35 = +35%; 0 se não afeta). */
    public double aumentoPercentualPara(TipoDano tipo) {
        return efeitosPassivos.stream().mapToDouble(e -> e.aumentoPercentual(tipo)).sum();
    }

    /** A defesa que esta habilidade dá (a parte de proteção pesada só conta se estiver usando). */
    public int bonusDefesaPara(boolean usandoProtecaoPesada) {
        return efeitosPassivos.stream().mapToInt(e -> e.bonusDefesa(usandoProtecaoPesada)).sum();
    }

    /** A chance de resistir a efeitos que esta habilidade dá (0.05 = 5%). */
    public double chanceResistirPara(boolean usandoProtecaoPesada) {
        return efeitosPassivos.stream().mapToDouble(e -> e.chanceResistir(usandoProtecaoPesada)).sum();
    }

    // ---------------------------------------------------------------- efeitos ao usar (ativas)

    /** Adiciona um efeito que acontece ao usar a habilidade. Retorna a própria habilidade. */
    public Habilidade comEfeitoAoUsar(EfeitoAoUsar efeito) {
        efeitosAoUsar.add(efeito);
        return this;
    }

    /** Define um efeito temporário de dano ao usar a habilidade. Retorna a própria habilidade. */
    public Habilidade comEfeito(EfeitoDano efeito) {
        return comEfeitoAoUsar(new EfeitoAoUsar.Dano(efeito));
    }

    /**
     * Efeito de defesa temporário que depende de um atributo do personagem.
     * Ex.: (INTELECTO, 1, 5, 180) = 1 de defesa e 5% de resistência por ponto, por 180 s.
     */
    public Habilidade comEfeitoDefesaPorAtributo(Atributo atributo, double defesaPorPonto,
                                                 double percentualResistenciaPorPonto,
                                                 int duracaoSegundos) {
        return comEfeitoAoUsar(new EfeitoAoUsar.DefesaPorAtributo(atributo, defesaPorPonto,
                percentualResistenciaPorPonto / 100.0, duracaoSegundos));
    }

    /** Aplica todos os efeitos ao usar desta habilidade (uma habilidade pode ter vários). */
    public void aplicarEfeitosAoUsar(String idDoEfeito, Atributos atributos, EfeitosAtivos efeitos) {
        for (EfeitoAoUsar efeito : efeitosAoUsar) {
            efeito.aplicar(idDoEfeito, atributos, efeitos);
        }
    }
}
