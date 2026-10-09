package com.exemplo.ordemmod;

import java.text.Normalizer;
import java.util.Locale;

/**
 * As 28 perícias de Ordem Paranormal, cada uma ligada a um atributo.
 */
public enum Pericia {
    ACROBACIA("Acrobacia", Atributo.AGILIDADE),
    ADESTRAMENTO("Adestramento", Atributo.PRESENCA),
    ARTES("Artes", Atributo.PRESENCA),
    ATLETISMO("Atletismo", Atributo.FORCA),
    ATUALIDADES("Atualidades", Atributo.INTELECTO),
    CIENCIAS("Ciências", Atributo.INTELECTO),
    CRIME("Crime", Atributo.AGILIDADE),
    DIPLOMACIA("Diplomacia", Atributo.PRESENCA),
    ENGANACAO("Enganação", Atributo.PRESENCA),
    FORTITUDE("Fortitude", Atributo.VIGOR),
    FURTIVIDADE("Furtividade", Atributo.AGILIDADE),
    INICIATIVA("Iniciativa", Atributo.AGILIDADE),
    INTIMIDACAO("Intimidação", Atributo.PRESENCA),
    INTUICAO("Intuição", Atributo.PRESENCA),
    INVESTIGACAO("Investigação", Atributo.INTELECTO),
    LUTA("Luta", Atributo.FORCA),
    MEDICINA("Medicina", Atributo.INTELECTO),
    OCULTISMO("Ocultismo", Atributo.INTELECTO),
    PERCEPCAO("Percepção", Atributo.PRESENCA),
    PILOTAGEM("Pilotagem", Atributo.AGILIDADE),
    PONTARIA("Pontaria", Atributo.AGILIDADE),
    PROFISSAO("Profissão", Atributo.INTELECTO),
    REFLEXOS("Reflexos", Atributo.AGILIDADE),
    RELIGIAO("Religião", Atributo.PRESENCA),
    SOBREVIVENCIA("Sobrevivência", Atributo.INTELECTO),
    TATICA("Tática", Atributo.INTELECTO),
    TECNOLOGIA("Tecnologia", Atributo.INTELECTO),
    VONTADE("Vontade", Atributo.PRESENCA);

    private final String nome;
    private final Atributo atributo;

    Pericia(String nome, Atributo atributo) {
        this.nome = nome;
        this.atributo = atributo;
    }

    public String getNome() {
        return nome;
    }

    public Atributo getAtributo() {
        return atributo;
    }

    /** Id para comandos: minúsculo e sem acento (ex.: "percepcao"). */
    public String getId() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Aceita "Percepção", "percepcao", "PERCEPCAO"... Retorna null se não existe. */
    public static Pericia porTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');
        try {
            return valueOf(limpo);
        } catch (IllegalArgumentException erro) {
            return null;
        }
    }
}
