package com.exemplo.ordemmod;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A árvore de habilidades do mod: todas as habilidades e seus pré-requisitos.
 * Os poderes de Combatente seguem a lista do livro (adaptados para o Minecraft em tempo real):
 *  - dano de combate é sempre FIXO (nada de dados de dano);
 *  - testes de perícia (investigação) continuam com dados (Incansável e Presteza Atlética);
 *  - bônus de defesa somam na Defesa do personagem (base 10 + Agilidade, veja HabilidadesDoPersonagem.defesa).
 * As habilidades de Especialista e Ocultista ainda são EXEMPLOS: troque pelas suas.
 * Os pré-requisitos são ids de outras habilidades.
 */
public final class CatalogoHabilidades {
    /** O poder repetível que treina / melhora perícias (tem tratamento especial na ficha). */
    public static final String ID_TREINAMENTO_PERICIA = "treinamento_pericia";
    /** A habilidade que veste a Armadura de Sangue no personagem (o EventosOrdem liga a armadura enquanto o efeito dura). */
    public static final String ID_ARMADURA_SANGUE = "armadura_de_sangue";

    // Ids com regra especial em outras classes (HabilidadesDoPersonagem, EventosOrdem, EventosDano, TesteNaTela).
    public static final String ID_ATAQUE_ESPECIAL = "ataque_especial";
    public static final String ID_TIRO_CERTEIRO = "tiro_certeiro";
    public static final String ID_INCANSAVEL = "incansavel";
    public static final String ID_PRESTEZA_ATLETICA = "presteza_atletica";
    public static final String ID_INSPIRAR_CONFIANCA = "inspirar_confianca";
    public static final String ID_ESTRATEGISTA = "estrategista";
    public static final String ID_BRECHA_NA_GUARDA = "brecha_na_guarda";
    public static final String ID_OFICIAL_COMANDANTE = "oficial_comandante";
    public static final String ID_POTENCIA_MAXIMA = "potencia_maxima";
    public static final String ID_DURO_DE_MATAR = "duro_de_matar";
    public static final String ID_INQUEBRAVEL = "inquebravel";

    public static final List<Habilidade> TODAS = List.of(
        // ============================================================ COMBATENTE: Ataque Especial
        // NEX 5% (2 PE, +5), 25% (3 PE, +10), 55% (4 PE, +15) e 85% (5 PE, +20), como na tabela do livro.
        new Habilidade(ID_ATAQUE_ESPECIAL, "Ataque Especial",
                "Gasta 2 PE: +5 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 2)
                .comEfeito(new EfeitoDano(5, 30, fisicoEDistancia())),
        new Habilidade("ataque_especial_2", "Ataque Especial II",
                "Gasta 3 PE: +10 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 5, 3)          // NEX 25%
                .comEfeito(new EfeitoDano(10, 30, fisicoEDistancia()))
                .comEvolucaoDe("ataque_especial"),
        new Habilidade("ataque_especial_3", "Ataque Especial III",
                "Gasta 4 PE: +15 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 11, 4)         // NEX 55%
                .comEfeito(new EfeitoDano(15, 30, fisicoEDistancia()))
                .comEvolucaoDe("ataque_especial_2"),
        new Habilidade("ataque_especial_4", "Ataque Especial IV",
                "Gasta 5 PE: +20 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 17, 5)         // NEX 85%
                .comEfeito(new EfeitoDano(20, 30, fisicoEDistancia()))
                .comEvolucaoDe("ataque_especial_3"),

        // ============================================================ COMBATENTE: poderes (lista do livro)
        new Habilidade("armamento_pesado", "Armamento Pesado",
                "Você recebe proficiência com armas pesadas. Requer Força 2.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comRequisitoAtributo(Atributo.FORCA, 2)
                .concedeProficiencia(Proficiencia.ARMAS_PESADAS),
        new Habilidade("artista_marcial", "Artista Marcial",
                "Seus ataques desarmados causam +4 de dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comBonusDanoPassivo(4, desarmado()),
        new Habilidade("artista_marcial_2", "Artista Marcial II",
                "Seus ataques desarmados causam +6 de dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 7, 0)          // NEX 35%
                .comBonusDanoPassivo(6, desarmado())
                .comEvolucaoDe("artista_marcial"),
        new Habilidade("artista_marcial_3", "Artista Marcial III",
                "Seus ataques desarmados causam +8 de dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 14, 0)         // NEX 70%
                .comBonusDanoPassivo(8, desarmado())
                .comEvolucaoDe("artista_marcial_2"),
        // No livro é uma reação a quem se afasta; no jogo em tempo real vira uma postura curta de prontidão.
        new Habilidade("ataque_de_oportunidade", "Ataque de Oportunidade",
                "Gasta 1 PE: por 15 segundos você fica de prontidão e seus ataques corpo a corpo causam +3 de dano, "
                        + "punindo quem tenta se afastar de você.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 1)
                .comEfeito(new EfeitoDano(3, 15, corpoACorpo())),
        // O custo de empunhar duas armas no livro é uma penalidade nos testes de ataque; aqui é -2 de defesa.
        new Habilidade("combater_com_duas_armas", "Combater com Duas Armas",
                "Gasta 2 PE: por 20 segundos você ataca com as duas mãos e causa +4 de dano corpo a corpo, "
                        + "mas fica exposto e perde 2 de defesa. Requer Agilidade 3.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 2)
                .comRequisitoAtributo(Atributo.AGILIDADE, 3)
                .comEfeito(new EfeitoDano(4, 20, corpoACorpo()))
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(-2, 0.0, 20))),
        // No livro: +5 de Defesa e penalidade nos ataques. Aqui: +5 de defesa e -2 de dano (o dano nunca fica abaixo de 0).
        new Habilidade("combate_defensivo", "Combate Defensivo",
                "Não gasta PE: por 30 segundos você luta na defensiva e recebe +5 de defesa, "
                        + "mas seus ataques causam -2 de dano. Requer Intelecto 2.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 0)
                .comRequisitoAtributo(Atributo.INTELECTO, 2)
                .comEfeito(new EfeitoDano(-2, 30, fisicoEDistancia()))
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(5, 0.0, 30))),
        new Habilidade("golpe_demolidor", "Golpe Demolidor",
                "Gasta 1 PE: por 10 segundos seus ataques corpo a corpo quebram a guarda e causam +5 de dano. "
                        + "Requer Força 2.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 1)
                .comRequisitoAtributo(Atributo.FORCA, 2)
                .comEfeito(new EfeitoDano(5, 10, corpoACorpo())),
        // No livro é um dado de dano a mais; aqui é dano fixo.
        new Habilidade("golpe_pesado", "Golpe Pesado",
                "Enquanto empunha uma arma corpo a corpo, você causa +3 de dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comBonusDanoPassivo(3, corpoACorpo()),
        // Poder de investigação: continua com dados. O teste de perícia vem de TesteNaTela.rolar.
        new Habilidade(ID_INCANSAVEL, "Incansável",
                "Gasta 2 PE: seu próximo teste de perícia usa Força ou Agilidade (o maior) no lugar do atributo-base "
                        + "e você tenta duas vezes, ficando com o melhor resultado. Vale por 1 minuto.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 2)
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(0, 0.0, 60))),
        // Poder de investigação: continua com dados. O teste de perícia vem de TesteNaTela.rolar.
        new Habilidade(ID_PRESTEZA_ATLETICA, "Presteza Atlética",
                "Gasta 1 PE: seu próximo teste de perícia usa Força ou Agilidade (o maior) no lugar do atributo-base. "
                        + "Vale por 1 minuto.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 1)
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(0, 0.0, 60))),
        new Habilidade("protecao_pesada", "Proteção Pesada",
                "Você recebe proficiência com proteções pesadas. Requer NEX 30%.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 6, 0)          // NEX 30%
                .concedeProficiencia(Proficiencia.PROTECOES_PESADAS),
        new Habilidade("reflexos_defensivos", "Reflexos Defensivos",
                "Você recebe +2 de defesa e 5% de chance de resistir a efeitos. Requer Agilidade 2.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comRequisitoAtributo(Atributo.AGILIDADE, 2)
                .comBonusDefesaPassivo(2)
                .comChanceResistirEfeitos(5),
        // No livro é uma ação livre de sacar/guardar itens; aqui vira ritmo de combate em corpo a corpo e à distância.
        new Habilidade("saque_rapido", "Saque Rápido",
                "Gasta 1 PE: por 15 segundos você saca e troca de arma sem perder o ritmo e causa +2 de dano "
                        + "corpo a corpo e à distância.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 1)
                .comEfeito(new EfeitoDano(2, 15, corpoACorpoEDistancia())),
        // No livro: ataques extras com arma de fogo pagando 2 PE por ataque já feito. Aqui: uma rajada curta.
        new Habilidade("segurar_o_gatilho", "Segurar o Gatilho",
                "Gasta 2 PE: por 15 segundos você mantém o dedo no gatilho e causa +6 de dano à distância. "
                        + "Requer NEX 60%.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 12, 2)           // NEX 60%
                .comEfeito(new EfeitoDano(6, 15, aDistancia())),
        new Habilidade("sentido_tatico", "Sentido Tático",
                "Gasta 2 PE para analisar o ambiente: por 3 minutos, recebe defesa igual ao Intelecto "
                        + "e 5% de resistência a efeitos para cada ponto de Intelecto. Requer Intelecto 2.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 2)
                .comRequisitoAtributo(Atributo.INTELECTO, 2)
                .comEfeitoDefesaPorAtributo(Atributo.INTELECTO, 1, 5, 180),
        // No livro: +2 Defesa e +2 de resistência a dano com proteção pesada. Pré-requisito: Proteção Pesada.
        new Habilidade("tanque_de_guerra", "Tanque de Guerra",
                "Se estiver usando proteção pesada, sua defesa aumenta em 2 e cada golpe recebido causa 2 de dano a menos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0, List.of("protecao_pesada"))
                .comEfeitoPassivo(new EfeitoPassivo.BonusDefesa(2, true))
                .comReducaoDanoRecebido(2, true),
        // No livro soma Agilidade nas rolagens de dano; aqui soma a Agilidade no dano à distância (HabilidadesDoPersonagem).
        new Habilidade(ID_TIRO_CERTEIRO, "Tiro Certeiro",
                "Você firma a mira: seus ataques à distância (armas de fogo, arcos e projéteis) causam dano extra "
                        + "igual à sua Agilidade.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0),
        // No livro o alvo é forçado a se proteger (-5 nos ataques dele); aqui você ganha +5 de defesa por um tempo.
        new Habilidade("tiro_de_cobertura", "Tiro de Cobertura",
                "Gasta 1 PE: você dispara para forçar os inimigos a se proteger e, por 15 segundos, recebe +5 de defesa.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 1)
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(5, 0.0, 15))),
        // PROVISORIO: o mod ainda não tem poderes paranormais para escolher (livro, p. 114).
        new Habilidade("transcender", "Transcender",
                "Você abre sua mente ao Outro Lado. Os poderes paranormais ainda não existem no mod; por enquanto "
                        + "este poder só concede 5% de chance de resistir a efeitos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comChanceResistirEfeitos(5),
        new Habilidade(ID_TREINAMENTO_PERICIA, "Treinamento em Perícia",
                "Escolha duas perícias. Você se torna treinado nessas perícias. A partir de NEX 35%, você pode "
                        + "escolher perícias nas quais já é treinado para se tornar veterano. A partir de NEX 70%, "
                        + "pode escolher perícias nas quais já é veterano para se tornar expert. "
                        + "Você pode escolher este poder várias vezes.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .repetivel(),

        // Especialista
        new Habilidade("golpe_certeiro", "Golpe Certeiro", "Exemplo: ataque preciso.",
                TipoHabilidade.ATIVA, ClasseOP.ESPECIALISTA, 1, 3),
        new Habilidade("olho_treinado", "Olho Treinado", "Exemplo: percebe perigos.",
                TipoHabilidade.PASSIVA, ClasseOP.ESPECIALISTA, 2, 0),
        new Habilidade("golpe_mortal", "Golpe Mortal", "Exemplo: ataque preciso e letal.",
                TipoHabilidade.ATIVA, ClasseOP.ESPECIALISTA, 3, 6, List.of("golpe_certeiro")),

        // Ocultista
        new Habilidade("escudo_mental", "Escudo Mental", "Exemplo: protege a mente.",
                TipoHabilidade.ATIVA, ClasseOP.OCULTISTA, 1, 3),
        // PROVISORIO: custo, defesa e duracao NAO sao das regras oficiais; ajuste aqui (PE, bonus de defesa, segundos).
        new Habilidade(ID_ARMADURA_SANGUE, "Armadura de Sangue",
                "Gasta 3 PE: o sangue se forma em uma armadura sobre o corpo por 3 minutos e dá +5 de defesa.",
                TipoHabilidade.ATIVA, ClasseOP.OCULTISTA, 1, 3)
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(5, 0.0, 180))),
        new Habilidade("sentir_outro_lado", "Sentir o Outro Lado", "Exemplo: sente o paranormal por perto.",
                TipoHabilidade.PASSIVA, ClasseOP.OCULTISTA, 2, 0),
        new Habilidade("mente_blindada", "Mente Blindada", "Exemplo: mente mais resistente.",
                TipoHabilidade.PASSIVA, ClasseOP.OCULTISTA, 3, 0, List.of("escudo_mental")),

        // ============================================================ TRILHAS DE COMBATENTE
        // Liberadas sozinhas nos níveis 2, 8, 13 e 20 (NEX 10%, 40%, 65%, 99%) depois de escolher a trilha.

        // --- Aniquilador: especialista em dano contra um alvo (livro: a margem de ameaça da arma favorita cai
        // I / II / III; no jogo, cada passo vale +5% de chance de crítico)
        new Habilidade("a_favorita", "A Favorita",
                "Você domina sua arma favorita: +5% de chance de acerto crítico corpo a corpo e à distância.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comChanceCritico(5, corpoACorpoEDistancia())
                .daTrilha(Trilha.ANIQUILADOR),
        new Habilidade("tecnica_secreta", "Técnica Secreta",
                "Gasta 2 PE: sua técnica secreta destrói a defesa do alvo e causa +6 de dano corpo a corpo e à distância "
                        + "por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)            // NEX 40%
                .comEfeito(new EfeitoDano(6, 30, corpoACorpoEDistancia()))
                .daTrilha(Trilha.ANIQUILADOR),
        new Habilidade("tecnica_sublime", "Técnica Sublime",
                "Gasta 3 PE: golpes letais e perfurantes que causam +12 de dano corpo a corpo e à distância "
                        + "por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 3)           // NEX 65%
                .comEfeito(new EfeitoDano(12, 30, corpoACorpoEDistancia()))
                .comEvolucaoDe("tecnica_secreta")
                .daTrilha(Trilha.ANIQUILADOR),
        new Habilidade("maquina_de_guerra", "Máquina de Matar",
                "Sua arma favorita vira uma máquina de matar: +10% de chance de crítico (soma com A Favorita) "
                        + "e +3 de dano corpo a corpo e à distância.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)         // NEX 99%
                .comChanceCritico(10, corpoACorpoEDistancia())
                .comBonusDanoPassivo(3, corpoACorpoEDistancia())
                .daTrilha(Trilha.ANIQUILADOR),

        // --- Comandante de Campo: liderança. Estes poderes afetam também os aliados por perto
        // (EventosOrdem.compartilharComAliados). Brecha na Guarda dobra o alcance de Inspirar Confiança e Estrategista.
        new Habilidade(ID_INSPIRAR_CONFIANCA, "Inspirar Confiança",
                "Gasta 2 PE: sua liderança inspira você e os aliados num raio de 9 blocos: por 1 minuto recebem defesa "
                        + "igual à sua Presença e 5% de resistência a efeitos por ponto de Presença.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 2, 2)
                .comEfeitoDefesaPorAtributo(Atributo.PRESENCA, 1, 5, 60)
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),
        new Habilidade(ID_ESTRATEGISTA, "Estrategista",
                "Gasta 1 PE: você direciona você e os aliados num raio de 9 blocos para posições melhores: por 30 segundos "
                        + "recebem defesa igual ao seu Intelecto.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 1)
                .comEfeitoDefesaPorAtributo(Atributo.INTELECTO, 1, 0, 30)
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),
        new Habilidade(ID_BRECHA_NA_GUARDA, "Brecha na Guarda",
                "Gasta 2 PE: você e os aliados num raio de 9 blocos aproveitam a brecha e causam +8 de dano corpo a corpo, "
                        + "desarmado e à distância por 20 segundos. Dobra o alcance de Inspirar Confiança e Estrategista.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 2)
                .comEfeito(new EfeitoDano(8, 20, fisicoEDistancia()))
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),
        new Habilidade(ID_OFICIAL_COMANDANTE, "Oficial Comandante",
                "Gasta 5 PE: você e os aliados num raio de 18 blocos recebem uma ação extra de combate: "
                        + "+10 de dano corpo a corpo, desarmado e à distância e +4 de defesa por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 20, 5)
                .comEfeito(new EfeitoDano(10, 30, fisicoEDistancia()))
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(4, 0.0, 30)))
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),

        // --- Guerreiro: combate corpo a corpo, críticos e força bruta
        new Habilidade("tecnica_letal", "Técnica Letal",
                "Você aumenta a margem de ameaça dos seus ataques corpo a corpo: +10% de chance de acerto crítico.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comChanceCritico(10, corpoACorpo())
                .daTrilha(Trilha.GUERREIRO),
        new Habilidade("revidar", "Revidar",
                "Gasta 2 PE: por 30 segundos, recebe defesa igual à Força e +4 de dano corpo a corpo para contra-atacar.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)
                .comEfeito(new EfeitoDano(4, 30, corpoACorpo()))
                .comEfeitoDefesaPorAtributo(Atributo.FORCA, 1, 0, 30)
                .daTrilha(Trilha.GUERREIRO),
        new Habilidade("forca_opressora", "Força Opressora",
                "Gasta 1 PE: por 15 segundos seus golpes corpo a corpo e desarmados oprimem o alvo e causam +5 de dano.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 1)
                .comEfeito(new EfeitoDano(5, 15, corpoACorpoEDesarmado()))
                .daTrilha(Trilha.GUERREIRO),
        new Habilidade(ID_POTENCIA_MAXIMA, "Potência Máxima",
                "Quando você usa o Ataque Especial com armas corpo a corpo, os bônus de dano são dobrados.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .daTrilha(Trilha.GUERREIRO),

        // --- Operações Especiais: velocidade e agilidade
        new Habilidade("iniciativa_aprimorada", "Iniciativa Aprimorada",
                "Você reage antes de todos e se move 6% mais rápido.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comBonusVelocidade(6)
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),
        new Habilidade("ataque_extra", "Ataque Extra",
                "Gasta 2 PE: você encaixa um ataque adicional e causa +5 de dano corpo a corpo, desarmado e à distância "
                        + "por 20 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)
                .comEfeito(new EfeitoDano(5, 20, fisicoEDistancia()))
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),
        new Habilidade("surto_de_adrenalina", "Surto de Adrenalina",
                "Gasta 5 PE: uma ação adicional. Por 45 segundos, recebe defesa igual à Agilidade, 5% de resistência por "
                        + "ponto de Agilidade e +5 de dano corpo a corpo, desarmado e à distância.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 5)
                .comEfeitoDefesaPorAtributo(Atributo.AGILIDADE, 1, 5, 45)
                .comEfeito(new EfeitoDano(5, 45, fisicoEDistancia()))
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),
        new Habilidade("sempre_alerta", "Sempre Alerta",
                "Você começa todo combate em vantagem: se move 10% mais rápido e recebe +3 de defesa e 10% de resistência "
                        + "a efeitos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .comBonusVelocidade(10)
                .comBonusDefesaPassivo(3)
                .comChanceResistirEfeitos(10)
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),

        // --- Tropa de Choque: resistência e absorção de dano
        new Habilidade("casca_grossa", "Casca Grossa",
                "Seu corpo é treinado para aguentar o tranco: +1 ponto de vida máxima por nível de NEX.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comBonusVidaPorNivel(1)
                .daTrilha(Trilha.TROPA_DE_CHOQUE),
        new Habilidade("cai_dentro", "Cai Dentro",
                "Gasta 1 PE: você se coloca na linha de frente. Por 30 segundos, recebe 2 de defesa e 5% de resistência "
                        + "a efeitos por ponto de Vigor.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 1)
                .comEfeitoDefesaPorAtributo(Atributo.VIGOR, 2, 5, 30)
                .daTrilha(Trilha.TROPA_DE_CHOQUE),
        // No livro é uma reação que reduz um dano à metade; aqui é uma janela curta (EventosDano aplica a metade).
        new Habilidade(ID_DURO_DE_MATAR, "Duro de Matar",
                "Gasta 2 PE: por 6 segundos todo dano que você sofre é reduzido à metade.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 2)
                .comEfeitoAoUsar((id, atributos, efeitos) -> efeitos.ativar(id, new EfeitoDefesa(0, 0.0, 6)))
                .daTrilha(Trilha.TROPA_DE_CHOQUE),
        new Habilidade(ID_INQUEBRAVEL, "Inquebrável",
                "Você recebe +3 de defesa e 10% de resistência a efeitos. Enquanto estiver machucado (metade da vida ou "
                        + "menos), cada golpe recebido causa 3 de dano a menos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .comBonusDefesaPassivo(3)
                .comChanceResistirEfeitos(10)
                .daTrilha(Trilha.TROPA_DE_CHOQUE)
    );

    private CatalogoHabilidades() {
    }

    /** Todas as habilidades de uma classe. */
    public static List<Habilidade> daClasse(ClasseOP classe) {
        return TODAS.stream().filter(h -> h.getClasse() == classe).toList();
    }

    /** Todas as habilidades de uma trilha. */
    public static List<Habilidade> daTrilha(Trilha trilha) {
        return TODAS.stream().filter(h -> h.getTrilha() == trilha).toList();
    }

    /** Procura uma habilidade pelo id. Retorna null se não existir. */
    public static Habilidade porId(String id) {
        for (Habilidade habilidade : TODAS) {
            if (habilidade.getId().equals(id)) {
                return habilidade;
            }
        }
        return null;
    }

    /** Só o dano de ataques corpo a corpo com arma (o desarmado é outro tipo). */
    private static Set<TipoDano> corpoACorpo() {
        return EnumSet.of(TipoDano.CORPO_A_CORPO);
    }

    /** Corpo a corpo e à distância (sem o desarmado). */
    private static Set<TipoDano> corpoACorpoEDistancia() {
        return EnumSet.of(TipoDano.CORPO_A_CORPO, TipoDano.A_DISTANCIA);
    }

    /** Corpo a corpo com arma e desarmado. */
    private static Set<TipoDano> corpoACorpoEDesarmado() {
        return EnumSet.of(TipoDano.CORPO_A_CORPO, TipoDano.DESARMADO);
    }

    /** Só o dano à distância (arcos, projéteis e, quando entrarem, armas de fogo). */
    private static Set<TipoDano> aDistancia() {
        return EnumSet.of(TipoDano.A_DISTANCIA);
    }

    /** Só o dano de ataques desarmados (socos, chutes). */
    private static Set<TipoDano> desarmado() {
        return EnumSet.of(TipoDano.DESARMADO);
    }

    /** Os tipos de dano corpo a corpo, desarmado e à distância (armas de fogo entram aqui depois). */
    private static Set<TipoDano> fisicoEDistancia() {
        return EnumSet.of(TipoDano.CORPO_A_CORPO, TipoDano.DESARMADO, TipoDano.A_DISTANCIA);
    }
}
