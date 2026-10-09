package com.exemplo.ordemmod;

/**
 * As trilhas (subclasses) de cada classe.
 * O personagem escolhe UMA trilha ao chegar no NEX 10% (nível 2). As habilidades da trilha
 * são liberadas sozinhas nos marcos de NEX 10%, 40%, 65% e 99% (níveis 2, 8, 13 e 20),
 * sem gastar pontos de habilidade.
 */
public enum Trilha {
    // Combatente
    ANIQUILADOR("Aniquilador", ClasseOP.COMBATENTE, "Especialista em derrubar um alvo de cada vez. Domina a própria arma e transforma cada golpe em dano concentrado."),
    COMANDANTE_DE_CAMPO("Comandante de Campo", ClasseOP.COMBATENTE, "Líder nato. Inspira confiança, lê o campo de batalha e encontra as brechas na guarda do inimigo."),
    GUERREIRO("Guerreiro", ClasseOP.COMBATENTE, "Mestre do combate corpo a corpo. Golpes críticos, contra-ataques e força bruta."),
    OPERACOES_ESPECIAIS("Operações Especiais", ClasseOP.COMBATENTE, "Soldado de elite, veloz e adaptável. Age rápido, se move mais que os outros e nunca baixa a guarda."),
    TROPA_DE_CHOQUE("Tropa de Choque", ClasseOP.COMBATENTE, "Muralha viva. Aguenta o tranco, protege quem está atrás e só cai quando não há mais jeito."),
    // Especialista (habilidades ainda não criadas)
    ATIRADOR_DE_ELITE("Atirador de Elite", ClasseOP.ESPECIALISTA, "Precisão à distância: um tiro, uma baixa."),
    INFILTRADOR("Infiltrador", ClasseOP.ESPECIALISTA, "Age nas sombras: furtividade, sabotagem e ataques de surpresa."),
    MEDICO_DE_CAMPO("Médico de Campo", ClasseOP.ESPECIALISTA, "Mantém o grupo de pé com primeiros socorros e tratamento sob pressão."),
    NEGOCIADOR("Negociador", ClasseOP.ESPECIALISTA, "Resolve na conversa o que os outros resolveriam na bala."),
    TECNICO("Técnico", ClasseOP.ESPECIALISTA, "Mãos hábeis: equipamentos, gambiarras e soluções improvisadas."),
    // Ocultista (habilidades ainda não criadas)
    CONDUITE("Conduíte", ClasseOP.OCULTISTA, "Canaliza o paranormal para fortalecer rituais e proteger a mente."),
    FLAGELADOR("Flagelador", ClasseOP.OCULTISTA, "Transforma a própria dor em poder paranormal."),
    GRADUADO("Graduado", ClasseOP.OCULTISTA, "Estudioso do Outro Lado: conhece rituais mais do que qualquer um."),
    INTUITIVO("Intuitivo", ClasseOP.OCULTISTA, "Confia no instinto: o paranormal responde sem treino formal."),
    LAMINA_PARANORMAL("Lâmina Paranormal", ClasseOP.OCULTISTA, "Une combate corpo a corpo e rituais em uma lâmina imbuída.");

    /** Nível mínimo para escolher a trilha (NEX 10%). */
    public static final int NIVEL_MINIMO = 2;

    private final String nome;
    private final ClasseOP classe;
    private final String descricao;

    Trilha(String nome, ClasseOP classe, String descricao) {
        this.nome = nome;
        this.classe = classe;
        this.descricao = descricao;
    }

    /** Texto curto sobre o estilo da trilha (aparece na aba Trilha da ficha). */
    public String getDescricao() {
        return descricao;
    }

    public String getNome() {
        return nome;
    }

    public ClasseOP getClasse() {
        return classe;
    }
}
