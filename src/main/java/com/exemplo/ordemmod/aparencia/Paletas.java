package com.exemplo.ordemmod.aparencia;

/** As cores (RGB) que o jogador pode escolher na criação do personagem. */
public final class Paletas {
    /** Tons de pele, do mais claro ao mais escuro. O personagem guarda só o índice. */
    public static final int[] PELE = {
            0xF8DDC8, 0xEDC4A0, 0xE0AC7C, 0xC98E62, 0xB07548,
            0x8F5A38, 0x6E4128, 0x4F2E1C, 0x3A2114, 0x2A170F
    };

    /** Cores de cabelo (e de barba). */
    public static final int[] CABELO = {
            0x0B0B0B, 0x2B1B10, 0x4A2F1B, 0x6B4423, 0x8C5A2B, 0xA0693D,
            0xA23B1E, 0xB8913B, 0xDDB96A, 0xE8E1C8, 0x8A8A8A, 0xF2F2F2
    };

    private Paletas() {
    }
}
