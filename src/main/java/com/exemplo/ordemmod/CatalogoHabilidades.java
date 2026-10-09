package com.exemplo.ordemmod;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A árvore de habilidades do mod: todas as habilidades e seus pré-requisitos.
 * As que estão aqui são EXEMPLOS: troque pelas suas.
 * Os pré-requisitos são ids de outras habilidades.
 */
public final class CatalogoHabilidades {
    /** O poder repetível que treina / melhora perícias (tem tratamento especial na ficha). */
    public static final String ID_TREINAMENTO_PERICIA = "treinamento_pericia";

    public static final List<Habilidade> TODAS = List.of(
        // Combatente
        new Habilidade("ataque_especial", "Ataque Especial",
                "+5 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 2)
                .comEfeito(new EfeitoDano(5, 30, fisicoEDistancia())),
        new Habilidade("ataque_especial_2", "Ataque Especial II",
                "+10 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 5, 3)          // NEX 25%
                .comEfeito(new EfeitoDano(10, 30, fisicoEDistancia()))
                .comEvolucaoDe("ataque_especial"),
        new Habilidade("ataque_especial_3", "Ataque Especial III",
                "+15 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 11, 4)         // NEX 55%
                .comEfeito(new EfeitoDano(15, 30, fisicoEDistancia()))
                .comEvolucaoDe("ataque_especial_2"),
        new Habilidade("ataque_especial_4", "Ataque Especial IV",
                "+20 de dano corpo a corpo, desarmado e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 17, 5)         // NEX 85%
                .comEfeito(new EfeitoDano(20, 30, fisicoEDistancia()))
                .comEvolucaoDe("ataque_especial_3"),
        new Habilidade("pele_grossa", "Pele Grossa", "Exemplo: resiste melhor a dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0),
        new Habilidade("ataque_devastador", "Ataque Devastador", "Exemplo: golpe muito mais forte.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 3, 5, List.of("ataque_especial")),
        new Habilidade("armamento_pesado", "Armamento Pesado",
                "Você recebe proficiência com armas pesadas. Requer Força 2.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comRequisitoAtributo(Atributo.FORCA, 2)
                .concedeProficiencia(Proficiencia.ARMAS_PESADAS),
        new Habilidade("protecao_pesada", "Proteção Pesada",
                "Você recebe proficiência com proteções pesadas. Requer NEX 30%.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 6, 0)          // NEX 30%
                .concedeProficiencia(Proficiencia.PROTECOES_PESADAS),
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
        new Habilidade("reflexos_defensivos", "Reflexos Defensivos",
                "Você recebe +2 de defesa e 5% de chance de resistir a efeitos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comBonusDefesaPassivo(2)
                .comChanceResistirEfeitos(5),
        new Habilidade("sentido_tatico", "Sentido Tático",
                "Gasta 2 PE para analisar o ambiente: por 3 minutos, recebe defesa igual ao Intelecto "
                        + "e 5% de resistência a efeitos para cada ponto de Intelecto. Requer Intelecto 3.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 1, 2)
                .comRequisitoAtributo(Atributo.INTELECTO, 3)
                .comEfeitoDefesaPorAtributo(Atributo.INTELECTO, 1, 5, 180),
        new Habilidade("tanque_de_guerra", "Tanque de Guerra",
                "Se estiver usando proteção pesada, sua defesa aumenta em 2 e sua resistência a efeitos em 5%.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comBonusComProtecaoPesada(2, 5),
        new Habilidade("golpe_pesado", "Golpe Pesado",
                "Enquanto empunha uma arma corpo a corpo, você causa 35% a mais de dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comAumentoPercentualDano(35, corpoACorpo()),
        new Habilidade("treinamento_pericia", "Treinamento em Perícia",
                "Escolha duas perícias. Você se torna treinado nessas perícias. A partir de NEX 35%, você pode "
                        + "escolher perícias nas quais já é treinado para se tornar veterano. A partir de NEX 70%, "
                        + "pode escolher perícias nas quais já é veterano para se tornar expert. "
                        + "Você pode escolher este poder várias vezes.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .repetivel(),

        // Combatente: tiro (as armas de fogo entram em "à distância" até existirem de verdade no mod)
        new Habilidade("tiro_certeiro", "Tiro Certeiro",
                "Você firma a mira: +2 de dano à distância (armas de fogo entram aqui). Requer Agilidade 2.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 1, 0)
                .comRequisitoAtributo(Atributo.AGILIDADE, 2)
                .comBonusDanoPassivo(2, aDistancia()),
        new Habilidade("mira_treinada", "Mira Treinada",
                "Seus ataques à distância têm 10% de chance de acerto crítico.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 3, 0, List.of("tiro_certeiro"))   // NEX 15%
                .comChanceCritico(10, aDistancia()),
        new Habilidade("rajada", "Rajada",
                "Gasta 3 PE: +8 de dano à distância por 20 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 6, 3, List.of("mira_treinada"))     // NEX 30%
                .comEfeito(new EfeitoDano(8, 20, aDistancia())),
        new Habilidade("fogo_de_cobertura", "Fogo de Cobertura",
                "Gasta 2 PE: por 30 segundos, recebe defesa igual à Agilidade e +4 de dano à distância. "
                        + "Requer Agilidade 2.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 2, 2)                              // NEX 10%
                .comRequisitoAtributo(Atributo.AGILIDADE, 2)
                .comEfeito(new EfeitoDano(4, 30, aDistancia()))
                .comEfeitoDefesaPorAtributo(Atributo.AGILIDADE, 1, 0, 30),

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
        new Habilidade("sentir_outro_lado", "Sentir o Outro Lado", "Exemplo: sente o paranormal por perto.",
                TipoHabilidade.PASSIVA, ClasseOP.OCULTISTA, 2, 0),
        new Habilidade("mente_blindada", "Mente Blindada", "Exemplo: mente mais resistente.",
                TipoHabilidade.PASSIVA, ClasseOP.OCULTISTA, 3, 0, List.of("escudo_mental")),

        // ============================================================ TRILHAS DE COMBATENTE
        // Liberadas sozinhas nos níveis 2, 8, 13 e 20 (NEX 10%, 40%, 65%, 99%) depois de escolher a trilha.

        // --- Aniquilador: especialista em dano contra um alvo
        new Habilidade("a_favorita", "A Favorita",
                "Você domina sua arma: +2 de dano corpo a corpo e à distância.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comBonusDanoPassivo(2, corpoACorpoEDistancia())
                .daTrilha(Trilha.ANIQUILADOR),
        new Habilidade("tecnica_secreta", "Técnica Secreta",
                "Gasta 2 PE: +6 de dano corpo a corpo e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)            // NEX 40%
                .comEfeito(new EfeitoDano(6, 30, corpoACorpoEDistancia()))
                .daTrilha(Trilha.ANIQUILADOR),
        new Habilidade("tecnica_sublime", "Técnica Sublime",
                "Gasta 3 PE: +12 de dano corpo a corpo e à distância por 30 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 3)           // NEX 65%
                .comEfeito(new EfeitoDano(12, 30, corpoACorpoEDistancia()))
                .comEvolucaoDe("tecnica_secreta")
                .daTrilha(Trilha.ANIQUILADOR),
        new Habilidade("maquina_de_guerra", "Máquina de Guerra",
                "Você causa 25% a mais de dano corpo a corpo e à distância.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)         // NEX 99%
                .comAumentoPercentualDano(25, corpoACorpoEDistancia())
                .daTrilha(Trilha.ANIQUILADOR),

        // --- Comandante de Campo: liderança e presença (por enquanto os efeitos valem só para o próprio personagem)
        new Habilidade("inspirar_confianca", "Inspirar Confiança",
                "Gasta 1 PE: por 1 minuto, recebe defesa igual à Presença e 5% de resistência a efeitos por ponto de Presença.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 2, 1)
                .comEfeitoDefesaPorAtributo(Atributo.PRESENCA, 1, 5, 60)
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),
        new Habilidade("estrategista", "Estrategista",
                "Você recebe +2 de defesa e 5% de chance de resistir a efeitos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 8, 0)
                .comBonusDefesaPassivo(2)
                .comChanceResistirEfeitos(5)
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),
        new Habilidade("brecha_na_guarda", "Brecha na Guarda",
                "Gasta 3 PE: +8 de dano corpo a corpo, desarmado e à distância por 20 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 3)
                .comEfeito(new EfeitoDano(8, 20, fisicoEDistancia()))
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),
        new Habilidade("oficial_comandante", "Oficial Comandante",
                "Você recebe +4 de defesa, 15% de resistência a efeitos e +3 de dano corpo a corpo, desarmado e à distância.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .comBonusDefesaPassivo(4)
                .comChanceResistirEfeitos(15)
                .comBonusDanoPassivo(3, fisicoEDistancia())
                .daTrilha(Trilha.COMANDANTE_DE_CAMPO),

        // --- Guerreiro: combate corpo a corpo, críticos e força bruta
        new Habilidade("tecnica_letal", "Técnica Letal",
                "Seus ataques corpo a corpo têm 10% de chance de acerto crítico.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comChanceCritico(10, corpoACorpo())
                .daTrilha(Trilha.GUERREIRO),
        new Habilidade("revidar", "Revidar",
                "Gasta 2 PE: por 30 segundos, recebe defesa igual à Força e +4 de dano corpo a corpo.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)
                .comEfeito(new EfeitoDano(4, 30, corpoACorpo()))
                .comEfeitoDefesaPorAtributo(Atributo.FORCA, 1, 0, 30)
                .daTrilha(Trilha.GUERREIRO),
        new Habilidade("forca_opressora", "Força Opressora",
                "Seus ataques corpo a corpo e desarmados causam +3 de dano.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 13, 0)
                .comBonusDanoPassivo(3, corpoACorpoEDesarmado())
                .daTrilha(Trilha.GUERREIRO),
        new Habilidade("potencia_maxima", "Potência Máxima",
                "Seus ataques corpo a corpo causam 20% a mais de dano e ganham +10% de chance de crítico.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .comAumentoPercentualDano(20, corpoACorpo())
                .comChanceCritico(10, corpoACorpo())
                .daTrilha(Trilha.GUERREIRO),

        // --- Operações Especiais: velocidade e agilidade
        new Habilidade("iniciativa_aprimorada", "Iniciativa Aprimorada",
                "Você se move 6% mais rápido.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comBonusVelocidade(6)
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),
        new Habilidade("ataque_extra", "Ataque Extra",
                "Gasta 2 PE: +5 de dano corpo a corpo, desarmado e à distância por 20 segundos.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)
                .comEfeito(new EfeitoDano(5, 20, fisicoEDistancia()))
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),
        new Habilidade("surto_de_adrenalina", "Surto de Adrenalina",
                "Gasta 3 PE: por 45 segundos, recebe defesa igual à Agilidade, 5% de resistência por ponto de Agilidade e +5 de dano corpo a corpo, desarmado e à distância.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 13, 3)
                .comEfeitoDefesaPorAtributo(Atributo.AGILIDADE, 1, 5, 45)
                .comEfeito(new EfeitoDano(5, 45, fisicoEDistancia()))
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),
        new Habilidade("sempre_alerta", "Sempre Alerta",
                "Você se move 10% mais rápido e recebe +3 de defesa e 10% de resistência a efeitos.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .comBonusVelocidade(10)
                .comBonusDefesaPassivo(3)
                .comChanceResistirEfeitos(10)
                .daTrilha(Trilha.OPERACOES_ESPECIAIS),

        // --- Tropa de Choque: resistência e absorção de dano
        new Habilidade("casca_grossa", "Casca Grossa",
                "Você recebe +1 ponto de vida máxima por nível de NEX.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 2, 0)
                .comBonusVidaPorNivel(1)
                .daTrilha(Trilha.TROPA_DE_CHOQUE),
        new Habilidade("cai_dentro", "Cai Dentro",
                "Gasta 2 PE: por 30 segundos, recebe 2 de defesa e 5% de resistência a efeitos por ponto de Vigor.",
                TipoHabilidade.ATIVA, ClasseOP.COMBATENTE, 8, 2)
                .comEfeitoDefesaPorAtributo(Atributo.VIGOR, 2, 5, 30)
                .daTrilha(Trilha.TROPA_DE_CHOQUE),
        new Habilidade("duro_de_matar", "Duro de Matar",
                "Cada golpe recebido causa 1 de dano a menos (2 a menos se estiver usando proteção pesada).",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 13, 0)
                .comReducaoDanoRecebido(1, false)
                .comReducaoDanoRecebido(1, true)
                .daTrilha(Trilha.TROPA_DE_CHOQUE),
        new Habilidade("inquebravel", "Inquebrável",
                "Você recebe +3 de defesa, 10% de resistência a efeitos e mais 1 de redução de dano por golpe.",
                TipoHabilidade.PASSIVA, ClasseOP.COMBATENTE, 20, 0)
                .comBonusDefesaPassivo(3)
                .comChanceResistirEfeitos(10)
                .comReducaoDanoRecebido(1, false)
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
