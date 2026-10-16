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

    // ------------------------------------------------------------------ lore (texto de ambientação)

    /** Quem era essa pessoa antes de a Realidade ceder: uma frase ou duas de ambientação. */
    public static String origemLore(Origem o) {
        return switch (o) {
            case ACADEMICO -> "Passou anos entre bibliotecas e laboratórios, acostumado a achar que tudo tem explicação. Até encontrar algo que não tinha.";
            case AGENTE_DE_SAUDE -> "Viu de perto o que o corpo aguenta e o que ele não aguenta. Cuidar dos outros ensinou a olhar para o que está errado antes de todo mundo.";
            case ARTISTA -> "Vive de emocionar quem assiste. Sabe que uma boa performance engana a plateia, e que uma plateia assustada é a mais difícil de enganar.";
            case ATLETA -> "Treinou a vida inteira para ir além do próprio limite. O corpo responde antes mesmo de a mente entender o que está acontecendo.";
            case CHEF -> "Cozinhas profissionais são calor, pressa e faca afiada. Quem sobrevive a elas aprende a trabalhar sob pressão e a nunca revelar a receita.";
            case CRIMINOSO -> "Aprendeu na rua que regras são para quem pode pagar por elas. Conhece cada fechadura, cada atalho e cada sombra útil.";
            case CULTISTA_ARREPENDIDO -> "Esteve do outro lado do rito e viu o que ele cobra. Fugiu com marcas que ninguém mais enxerga e com um conhecimento que preferia não ter.";
            case DESGARRADO -> "Sem casa, sem nome nos papéis, sem ninguém para avisar se sumir. Sobreviver virou ofício, e o desconforto virou rotina.";
            case ENGENHEIRO -> "Acredita que tudo é um sistema, e que todo sistema tem um ponto de falha. Para ele, o paranormal é só um problema ainda sem projeto.";
            case EXECUTIVO -> "Salas de reunião, metas e contratos. Sabe que quase tudo se resolve com a pessoa certa na mesa certa, até o que não devia.";
            case LUTADOR -> "Aprendeu que a dor é uma linguagem e que o ringue não mente. Cada cicatriz é uma lição que não precisa ser repetida.";
            case MAGNATA -> "Cresceu cercado de recursos, contatos e portas que se abrem sozinhas. Descobriu que dinheiro compra quase tudo, menos o silêncio do Outro Lado.";
            case MERCENARIO -> "Luta por quem paga, sem perguntar o motivo. A experiência ensinou a chegar primeiro e a deixar o medo para depois.";
            case MILITAR -> "Disciplina, hierarquia e missão cumprida. Foi treinado para obedecer e para liderar, mas ninguém o treinou para isto.";
            case OPERARIO -> "Mãos calejadas, turnos longos e ferramentas que conhece de olhos fechados. Constrói e conserta o mundo que os outros só usam.";
            case POLICIAL -> "Patrulhou ruas onde algo sempre parecia fora do lugar. Aprendeu a confiar no instinto quando o relatório não explica o que viu.";
            case RELIGIOSO -> "Tem fé, e a fé foi posta à prova. Sabe consolar quem sofre e sabe o peso de acreditar em algo maior que o mundo.";
            case SERVIDOR_PUBLICO -> "Anos atrás de um balcão, entre processos e filas. Conhece as pessoas de verdade e acredita que alguém precisa continuar de pé.";
            case TI -> "Vive de achar o que está escondido em meio a milhões de linhas. Descobriu que algumas coisas na rede não deveriam ter endereço.";
            case TEORICO_DA_CONSPIRACAO -> "Todo mundo riu dele, por anos. Agora os sinais que ele juntava em paredes de recortes estão do lado de fora da janela.";
            case TRABALHADOR_RURAL -> "Criou-se no campo, entre bichos, clima e trabalho duro. Entende a natureza e sabe quando ela fica quieta demais.";
            case TRAMBIQUEIRO -> "Vive de contar a história certa para a pessoa certa. Já enganou muita gente; agora precisa descobrir quem pode enganá-lo.";
            case UNIVERSITARIO -> "Noites em claro, prazos e curiosidade demais. Está acostumado a procurar respostas, mesmo quando não deveria fazer a pergunta.";
            case VITIMA -> "Sobreviveu a algo que não devia ter sobrevivido. Carrega o trauma, e também o reflexo de quem aprendeu a nunca baixar a guarda.";
        };
    }

    /** O que essa perícia significa no universo: uma frase de ambientação. */
    public static String periciaLore(Pericia p) {
        return switch (p) {
            case ACROBACIA -> "Quando o chão some, quem sabe cair sai andando. O corpo aprende o caminho antes da cabeça.";
            case ADESTRAMENTO -> "Os animais sentem o Outro Lado antes de nós. Quem os entende sabe quando eles recuam, e por quê.";
            case ARTES -> "Uma música, um traço, um gesto no palco. A arte atravessa o medo e faz a plateia sentir o que as palavras não alcançam.";
            case ATLETISMO -> "Correr mais, subir mais alto, aguentar mais. Quando fugir é a única saída, o corpo treinado decide quem volta.";
            case ATUALIDADES -> "Notícias, boatos, cultura e o que anda se falando por aí. Quem acompanha o mundo percebe quando algo não bate.";
            case CIENCIAS -> "Fórmulas, métodos e provas. A ciência explica quase tudo, e o que ela não explica é justamente o que importa.";
            case CRIME -> "Fechaduras cedem, bolsos esvaziam e alarmes calam. Um conhecimento de rua que abre portas que o dinheiro não abre.";
            case DIPLOMACIA -> "Uma conversa bem conduzida vale mais que um tiro. Saber ouvir, ceder e convencer mantém o grupo vivo.";
            case ENGANACAO -> "A mentira certa, no tom certo, protege mais que um colete. Mas quem mente muito acaba perdendo a conta do que é verdade.";
            case FORTITUDE -> "O corpo cobra tudo: dor, veneno, cansaço. Quem resiste mais continua em pé quando os outros já caíram.";
            case FURTIVIDADE -> "Ficar quieto, ficar fora da luz, ficar fora do radar. Nem tudo precisa ser enfrentado, e muita coisa não deve ser.";
            case INICIATIVA -> "O primeiro segundo decide o resto. Quem reage antes pensa depois, e geralmente sobrevive para pensar.";
            case INTIMIDACAO -> "Um olhar, uma pausa, uma ameaça sem pressa. O medo dos outros também é uma arma, e custa menos que munição.";
            case INTUICAO -> "Aquela sensação de que algo está errado, sem saber o quê. Aprender a escutá-la é aprender a sobreviver.";
            case INVESTIGACAO -> "Cada cena guarda um detalhe fora do lugar. Quem investiga junta as peças que os outros preferem ignorar.";
            case LUTA -> "Punhos, lâminas e o que estiver à mão. O corpo a corpo não perdoa hesitação, e a prática é o que separa quem fica de quem cai.";
            case MEDICINA -> "Estancar, suturar, diagnosticar. Entre um golpe e o próximo, o médico decide quem ainda tem tempo.";
            case OCULTISMO -> "Símbolos, rituais e nomes que não se pronunciam. Saber demais atrai a atenção do Outro Lado, e saber de menos também.";
            case PERCEPCAO -> "Um ruído no corredor, um cheiro que não devia estar ali. Os sentidos alertam antes de a razão aceitar o perigo.";
            case PILOTAGEM -> "Carros, barcos, aeronaves. Chegar rápido, ou escapar mais rápido ainda, pode ser tudo que separa a vida da morte.";
            case PONTARIA -> "Respiração, mira e o tiro certo. A distância é a melhor aliada contra aquilo que não se deve tocar.";
            case PROFISSAO -> "Um ofício vivido por anos. Cada trabalho ensina um jeito de ver o mundo, e às vezes é esse jeito que resolve o problema.";
            case REFLEXOS -> "Quando o perigo vem de repente, não há tempo para pensar. O corpo desvia, e só depois vem o susto.";
            case RELIGIAO -> "Preces, ritos e crenças antigas. Quem conhece a fé sabe consolar, e também sabe do que as pessoas têm medo.";
            case SOBREVIVENCIA -> "Rastros na lama, abrigo na chuva, água para beber. Longe da cidade, quem entende o ambiente é quem volta para casa.";
            case TATICA -> "Posição, terreno e tempo. Em um combate, quem vê o quadro inteiro ganha a luta antes de ela começar.";
            case TECNOLOGIA -> "Redes, sistemas e aparelhos que fazem mais do que deviam. Quem os domina abre portas, câmeras e segredos.";
            case VONTADE -> "A mente também é um campo de batalha. Quem não cede ao medo, à dor ou à voz que sussurra continua sendo quem é.";
        };
    }
}
