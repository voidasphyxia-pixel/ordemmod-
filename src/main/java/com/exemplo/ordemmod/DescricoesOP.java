package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Os textos explicativos da tela de criação. Só texto: para mudar uma descrição, é aqui.
 * Os números do mod (vida por Vigor, velocidade por Agilidade...) são lidos das constantes
 * do próprio mod, então a tela não fica desatualizada quando você balancear.
 */
public final class DescricoesOP {
    private DescricoesOP() {
    }

    // ------------------------------------------------------------------ atributos

    public static String atributo(Atributo a) {
        return switch (a) {
            case AGILIDADE -> "Reflexos, coordenação e rapidez. Define sua mira, sua furtividade e o quanto você "
                    + "consegue escapar do perigo.";
            case FORCA -> "Potência muscular. Ajuda a golpear com força, escalar e vencer esforços físicos.";
            case INTELECTO -> "Raciocínio, estudo e memória. Quanto maior, mais perícias você pode treinar.";
            case PRESENCA -> "Personalidade e força de vontade. Alimenta sua energia (PE) e sua influência "
                    + "sobre os outros.";
            case VIGOR -> "Saúde e resistência do corpo. Aguenta mais dor, veneno e cansaço.";
        };
    }

    /** O que o atributo faz DENTRO do mod (lido das constantes do mod). */
    public static String atributoNoMod(Atributo a) {
        return switch (a) {
            case AGILIDADE -> "+" + Math.round(EfeitosAtributos.VELOCIDADE_POR_AGILIDADE * 100)
                    + "% de velocidade de movimento por ponto.";
            case FORCA -> "+" + String.format(new Locale("pt", "BR"), "%.1f", EfeitosAtributos.DANO_POR_FORCA)
                    + " de dano corpo a corpo por ponto.";
            case INTELECTO -> "+1 vaga de perícia treinada por ponto.";
            case PRESENCA -> "+1 PE máximo por ponto.";
            case VIGOR -> "+" + EfeitosAtributos.VIDA_POR_VIGOR + " de vida ("
                    + (EfeitosAtributos.VIDA_POR_VIGOR / 2) + " coração) por ponto.";
        };
    }

    /** "Acrobacia, Crime, Furtividade..." */
    public static String periciasDoAtributo(Atributo a) {
        List<String> nomes = new ArrayList<>();
        for (Pericia p : Pericia.values()) {
            if (p.getAtributo() == a) {
                nomes.add(p.getNome());
            }
        }
        return String.join(", ", nomes);
    }

    // ------------------------------------------------------------------ classes

    public static String classe(ClasseOP c) {
        return switch (c) {
            case COMBATENTE -> "Treinado para o confronto direto. Enfrenta o paranormal com armas, tática e "
                    + "muita resistência: é a classe com mais vida, mas com menos sanidade e energia.";
            case ESPECIALISTA -> "Versátil e cheio de recursos. Domina mais perícias que qualquer outra classe "
                    + "e cobre o que o grupo precisa: investigar, curar, se infiltrar ou negociar.";
            case OCULTISTA -> "Estudioso do Outro Lado. Tem a mente e a energia mais fortes, próprias de quem "
                    + "manipula o paranormal, mas o corpo é frágil.";
        };
    }

    public static String classePapel(ClasseOP c) {
        return switch (c) {
            case COMBATENTE -> "Linha de frente";
            case ESPECIALISTA -> "Apoio e versatilidade";
            case OCULTISTA -> "Conjurador";
        };
    }

    /** Nome da classe em português, com acento. */
    public static String nomeClasse(ClasseOP c) {
        return switch (c) {
            case COMBATENTE -> "Combatente";
            case ESPECIALISTA -> "Especialista";
            case OCULTISTA -> "Ocultista";
        };
    }

    /** As trilhas (subclasses) da classe: "Aniquilador, Guerreiro..." */
    public static String trilhasDaClasse(ClasseOP c) {
        List<String> nomes = new ArrayList<>();
        for (Trilha t : Trilha.values()) {
            if (t.getClasse() == c) {
                nomes.add(t.getNome());
            }
        }
        return String.join(", ", nomes);
    }

    // ------------------------------------------------------------------ perícias

    public static String pericia(Pericia p) {
        return switch (p) {
            case ACROBACIA -> "Equilibrar-se, saltar e amortecer quedas.";
            case ADESTRAMENTO -> "Lidar com animais: domar, acalmar e comandar.";
            case ARTES -> "Atuar, cantar, desenhar e se expressar com criatividade.";
            case ATLETISMO -> "Correr, nadar, escalar e mostrar força em esforços físicos.";
            case ATUALIDADES -> "Conhecimento geral sobre notícias, cultura e acontecimentos do mundo.";
            case CIENCIAS -> "Saber formal: matemática, química, física e biologia.";
            case CRIME -> "Arrombar fechaduras, bater carteiras e sabotar mecanismos.";
            case DIPLOMACIA -> "Convencer, negociar e causar boa impressão.";
            case ENGANACAO -> "Mentir, se disfarçar e blefar.";
            case FORTITUDE -> "Resistir a fadiga, veneno, doenças e dor.";
            case FURTIVIDADE -> "Mover-se sem ser notado e se esconder.";
            case INICIATIVA -> "Reagir rápido: decide quem age primeiro numa emergência.";
            case INTIMIDACAO -> "Assustar e coagir os outros pela ameaça.";
            case INTUICAO -> "Perceber mentiras, emoções e segundas intenções; ter palpites.";
            case INVESTIGACAO -> "Procurar pistas, examinar cenas e ligar os pontos.";
            case LUTA -> "Combate corpo a corpo, armado ou desarmado.";
            case MEDICINA -> "Primeiros socorros, cirurgias e tratamento de doenças.";
            case OCULTISMO -> "Saber sobre o paranormal, rituais e criaturas do Outro Lado.";
            case PERCEPCAO -> "Notar detalhes com os sentidos: ver, ouvir e farejar o que está errado.";
            case PILOTAGEM -> "Conduzir veículos, de carros e barcos a aeronaves.";
            case PONTARIA -> "Usar armas de disparo e de arremesso com precisão.";
            case PROFISSAO -> "Conhecimento de um ofício: a sua especialidade de trabalho.";
            case REFLEXOS -> "Esquivar de perigos súbitos e reagir por instinto.";
            case RELIGIAO -> "Conhecer crenças, ritos e teologia; acalmar pela fé.";
            case SOBREVIVENCIA -> "Viver na natureza: rastrear, se orientar, achar comida e abrigo.";
            case TATICA -> "Planejar e analisar situações de combate e estratégia.";
            case TECNOLOGIA -> "Operar e consertar computadores, eletrônicos e sistemas.";
            case VONTADE -> "Força mental: resistir a medo, pressão e influência paranormal.";
        };
    }
}
