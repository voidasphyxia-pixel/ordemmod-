package com.exemplo.ordemmod;

/**
 * Os cinco atributos de Ordem Paranormal.
 */
public enum Atributo {
    AGILIDADE("AGI"),
    FORCA("FOR"),
    INTELECTO("INT"),
    PRESENCA("PRE"),
    VIGOR("VIG");

    private final String sigla;

    Atributo(String sigla) {
        this.sigla = sigla;
    }

    public String getSigla() {
        return sigla;
    }
}
