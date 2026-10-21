package com.exemplo.ordemmod;

import java.util.Map;

/**
 * Em qual ramo da árvore de poderes (aba Poderes da ficha) cada poder aparece. Os ramos são só uma organização visual
 * (como "Close Combat" e "Ranged Combat" nas árvores de habilidade): estar no mesmo ramo NÃO é pré-requisito.
 * Poder que não está na lista cai em {@link #GERAL}. Para mudar um poder de ramo, mexa só no mapa abaixo.
 */
public final class RamosDePoderes {
    public static final String CORPO_A_CORPO = "CORPO A CORPO";
    public static final String A_DISTANCIA = "À DISTÂNCIA";
    public static final String DEFESA = "DEFESA";
    public static final String TATICO = "TÁTICO";
    public static final String GERAL = "GERAL";

    /** Ordem em que os ramos aparecem na tela, da esquerda para a direita. */
    public static final String[] ORDEM = {CORPO_A_CORPO, A_DISTANCIA, DEFESA, TATICO, GERAL};

    private static final Map<String, String> RAMO_DO_PODER = Map.ofEntries(
            // Combatente
            Map.entry(CatalogoHabilidades.ID_ATAQUE_ESPECIAL, CORPO_A_CORPO),
            Map.entry("artista_marcial", CORPO_A_CORPO),
            Map.entry("ataque_de_oportunidade", CORPO_A_CORPO),
            Map.entry("combater_com_duas_armas", CORPO_A_CORPO),
            Map.entry("golpe_demolidor", CORPO_A_CORPO),
            Map.entry("golpe_pesado", CORPO_A_CORPO),
            Map.entry("armamento_pesado", CORPO_A_CORPO),
            Map.entry(CatalogoHabilidades.ID_TIRO_CERTEIRO, A_DISTANCIA),
            Map.entry("saque_rapido", A_DISTANCIA),
            Map.entry("segurar_o_gatilho", A_DISTANCIA),
            Map.entry("tiro_de_cobertura", A_DISTANCIA),
            Map.entry("combate_defensivo", DEFESA),
            Map.entry("protecao_pesada", DEFESA),
            Map.entry("tanque_de_guerra", DEFESA),
            Map.entry("reflexos_defensivos", DEFESA),
            Map.entry("sentido_tatico", TATICO),
            Map.entry(CatalogoHabilidades.ID_INCANSAVEL, TATICO),
            Map.entry(CatalogoHabilidades.ID_PRESTEZA_ATLETICA, TATICO),
            Map.entry(CatalogoHabilidades.ID_TREINAMENTO_PERICIA, TATICO),
            Map.entry("transcender", TATICO),
            // Especialista
            Map.entry("golpe_certeiro", CORPO_A_CORPO),
            Map.entry("golpe_mortal", CORPO_A_CORPO),
            Map.entry("olho_treinado", TATICO),
            // Ocultista
            Map.entry("escudo_mental", DEFESA),
            Map.entry("mente_blindada", DEFESA),
            Map.entry(CatalogoHabilidades.ID_ARMADURA_SANGUE, DEFESA),
            Map.entry("sentir_outro_lado", TATICO));

    private RamosDePoderes() {
    }

    /** O ramo do poder (pelo id da primeira versão). */
    public static String de(String idDoPoder) {
        return RAMO_DO_PODER.getOrDefault(idDoPoder, GERAL);
    }
}
