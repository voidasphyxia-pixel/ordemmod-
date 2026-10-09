package com.exemplo.ordemmod;

import java.text.Normalizer;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * As origens: quem o personagem era antes da Ordem. Cada uma dá 2 perícias treinadas de graça
 * (não gastam vagas de treino) e um poder. Os poderes aqui são adaptações para o Minecraft:
 * cada um é só uma lista de efeitos numéricos, que o resto do mod lê.
 *
 * Quer balancear? Mexa nos números abaixo.
 */
public enum Origem {
    ACADEMICO("Acadêmico", Pericia.CIENCIAS, Pericia.INVESTIGACAO, "Saber é Poder",
            "+3 em testes de perícias de Intelecto.",
            Efeito.TESTE_INTELECTO, 3),
    AGENTE_DE_SAUDE("Agente de Saúde", Pericia.INTUICAO, Pericia.MEDICINA, "Técnica Medicinal",
            "Ataduras curam vida extra igual ao seu Intelecto.",
            Efeito.CURA_POR_INTELECTO, 1),
    ARTISTA("Artista", Pericia.ARTES, Pericia.ENGANACAO, "Magnum Opus",
            "+10% de XP e +5% de chance de resistir a efeitos mentais.",
            Efeito.XP, 0.10, Efeito.RESISTENCIA_MENTAL, 0.05),
    ATLETA("Atleta", Pericia.ACROBACIA, Pericia.ATLETISMO, "110%",
            "+5% de velocidade e -25% de dano de queda.",
            Efeito.VELOCIDADE, 0.05, Efeito.REDUCAO_QUEDA, 0.25),
    CHEF("Chef", Pericia.FORTITUDE, Pericia.PROFISSAO, "Ingrediente Secreto",
            "Comer restaura +1 de fome e +2 de saturação.",
            Efeito.COMIDA_FOME, 1, Efeito.COMIDA_SATURACAO, 2),
    CRIMINOSO("Criminoso", Pericia.CRIME, Pericia.FURTIVIDADE, "O Crime Compensa",
            "15% de chance de monstros derrubarem pepitas de ouro.",
            Efeito.DROP_OURO, 0.15),
    CULTISTA_ARREPENDIDO("Cultista Arrependido", Pericia.OCULTISMO, Pericia.RELIGIAO, "Traços do Outro Lado",
            "Sustos paranormais tiram 1 SAN a menos e +10% de dano contra mortos-vivos.",
            Efeito.PERDA_SAN_PARANORMAL, 1, Efeito.DANO_MORTOS_VIVOS, 0.10),
    DESGARRADO("Desgarrado", Pericia.FORTITUDE, Pericia.SOBREVIVENCIA, "Calejado",
            "+1 de vida por nível (a cada 5% de NEX).",
            Efeito.VIDA_POR_NIVEL, 1),
    ENGENHEIRO("Engenheiro", Pericia.PROFISSAO, Pericia.TECNOLOGIA, "Ferramenta Favorita",
            "-25% de dano de explosão e +1 PE máximo.",
            Efeito.REDUCAO_EXPLOSAO, 0.25, Efeito.PE_EXTRA, 1),
    EXECUTIVO("Executivo", Pericia.DIPLOMACIA, Pericia.PROFISSAO, "Processo Otimizado",
            "+2 PE máximo.",
            Efeito.PE_EXTRA, 2),
    LUTADOR("Lutador", Pericia.LUTA, Pericia.REFLEXOS, "Mão Pesada",
            "+2 de dano corpo a corpo e desarmado.",
            Efeito.DANO_CORPO, 2),
    MAGNATA("Magnata", Pericia.DIPLOMACIA, Pericia.PILOTAGEM, "Patrocinador da Ordem",
            "Ao escolher a origem, recebe 16 esmeraldas.",
            Efeito.ESMERALDAS_INICIAIS, 16),
    MERCENARIO("Mercenário", Pericia.INICIATIVA, Pericia.INTIMIDACAO, "Posição de Combate",
            "+5% de chance de crítico.",
            Efeito.CRITICO, 0.05),
    MILITAR("Militar", Pericia.PONTARIA, Pericia.TATICA, "Para Bellum",
            "+2 de dano à distância.",
            Efeito.DANO_DISTANCIA, 2),
    OPERARIO("Operário", Pericia.FORTITUDE, Pericia.PROFISSAO, "Ferramenta de Trabalho",
            "+4 de vida (2 corações).",
            Efeito.VIDA_FIXA, 4),
    POLICIAL("Policial", Pericia.PERCEPCAO, Pericia.PONTARIA, "Patrulha",
            "+5 blocos no raio do pulso de Percepção.",
            Efeito.PERCEPCAO_RAIO, 5),
    RELIGIOSO("Religioso", Pericia.RELIGIAO, Pericia.VONTADE, "Acalentar",
            "Recupera 1 SAN a cada 60 segundos.",
            Efeito.SAN_A_CADA_60S, 1),
    SERVIDOR_PUBLICO("Servidor Público", Pericia.INTUICAO, Pericia.VONTADE, "Espírito Cívico",
            "+5% de chance de resistir a efeitos ruins.",
            Efeito.RESISTENCIA, 0.05),
    TI("T.I.", Pericia.INVESTIGACAO, Pericia.TECNOLOGIA, "Motor de Busca",
            "+0,15 no multiplicador de crítico (acha o ponto fraco).",
            Efeito.CRITICO_MULT, 0.15),
    TEORICO_DA_CONSPIRACAO("Teórico da Conspiração", Pericia.INVESTIGACAO, Pericia.OCULTISMO, "Eu Já Sabia",
            "+10% de chance de resistir a efeitos mentais.",
            Efeito.RESISTENCIA_MENTAL, 0.10),
    TRABALHADOR_RURAL("Trabalhador Rural", Pericia.ADESTRAMENTO, Pericia.SOBREVIVENCIA, "Desbravador",
            "Bichos domados ganham +4 de vida e causam +10% de dano.",
            Efeito.ADESTRAMENTO_VIDA, 4, Efeito.DANO_BICHOS, 0.10),
    TRAMBIQUEIRO("Trambiqueiro", Pericia.CRIME, Pericia.ENGANACAO, "Impostor",
            "+10% de chance de o monstro perder você de vista.",
            Efeito.ENGANACAO_DESVIO, 0.10),
    UNIVERSITARIO("Universitário", Pericia.ATUALIDADES, Pericia.INVESTIGACAO, "Dedicação",
            "+10% de XP e +1 PE máximo.",
            Efeito.XP, 0.10, Efeito.PE_EXTRA, 1),
    VITIMA("Vítima", Pericia.REFLEXOS, Pericia.VONTADE, "Cicatrizes Psicológicas",
            "-5% de dano recebido e +5% de chance de resistir a efeitos mentais.",
            Efeito.REDUCAO_DANO, 0.05, Efeito.RESISTENCIA_MENTAL, 0.05);

    // Não incluída ainda: Amnésico (o jogador escolheria 2 perícias livres).

    /** O que um poder de origem pode mexer. Frações: 0.10 = 10%. */
    public enum Efeito {
        TESTE_INTELECTO, CURA_POR_INTELECTO, XP, VELOCIDADE, REDUCAO_QUEDA, COMIDA_FOME, COMIDA_SATURACAO,
        DROP_OURO, PERDA_SAN_PARANORMAL, DANO_MORTOS_VIVOS, VIDA_POR_NIVEL, VIDA_FIXA, REDUCAO_EXPLOSAO,
        REDUCAO_DANO, PE_EXTRA, DANO_CORPO, DANO_DISTANCIA, CRITICO, CRITICO_MULT, ESMERALDAS_INICIAIS,
        PERCEPCAO_RAIO, SAN_A_CADA_60S, RESISTENCIA, RESISTENCIA_MENTAL, ADESTRAMENTO_VIDA, DANO_BICHOS,
        ENGANACAO_DESVIO
    }

    private final String nome;
    private final List<Pericia> pericias;
    private final String poder;
    private final String descricao;
    private final Map<Efeito, Double> efeitos = new EnumMap<>(Efeito.class);

    Origem(String nome, Pericia primeira, Pericia segunda, String poder, String descricao, Object... pares) {
        this.nome = nome;
        this.pericias = List.of(primeira, segunda);
        this.poder = poder;
        this.descricao = descricao;
        for (int i = 0; i < pares.length; i += 2) {
            efeitos.put((Efeito) pares[i], ((Number) pares[i + 1]).doubleValue());
        }
    }

    public String getNome() {
        return nome;
    }

    /** As 2 perícias treinadas de graça. */
    public List<Pericia> getPericias() {
        return pericias;
    }

    public String getPoder() {
        return poder;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Valor de um efeito (0 se esta origem não mexe nele). */
    public double valor(Efeito efeito) {
        return efeitos.getOrDefault(efeito, 0.0);
    }

    /** Bônus fixo nos testes de perícias deste atributo (hoje só o Intelecto do Acadêmico). */
    public int bonusTeste(Atributo atributo) {
        return atributo == Atributo.INTELECTO ? (int) valor(Efeito.TESTE_INTELECTO) : 0;
    }

    /** Id para comandos: minúsculo e sem acento (ex.: "agente_de_saude"). */
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Aceita "Agente de Saúde", "agente_de_saude", "T.I."... Retorna null se não existe. */
    public static Origem porTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace(".", "")
                .replace(' ', '_')
                .replace('-', '_');
        try {
            return valueOf(limpo);
        } catch (IllegalArgumentException erro) {
            return null;
        }
    }
}
