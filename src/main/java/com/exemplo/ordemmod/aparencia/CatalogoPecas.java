package com.exemplo.ordemmod.aparencia;

import java.util.List;

/**
 * Lista das peças de aparência (cabelo, roupas, sapatos). Servidor e cliente usam a mesma lista,
 * então o servidor consegue recusar uma peça que não existe. As texturas ficam em
 * assets/ordemmod/appearance/&lt;slot&gt;/&lt;id&gt;.png (e &lt;id&gt;_tint.png nas peças que aceitam cor).
 *
 * As peças vêm do mod Bedrock Skins (BrandonItaly, licença MIT; ver licencas/BedrockSkins-MIT.txt).
 */
public final class CatalogoPecas {
    public record Peca(String slot, String id, boolean tingivel, int corBase, int corPadrao) {
    }

    /** Os slots, na ordem em que a tela de aparência vai mostrar. */
    public static final List<String> SLOTS = List.of("hair", "facial_hair", "top", "outerwear", "bottom", "feet");

    /** Ordem de desenho na skin, de baixo para cima. */
    public static final List<String> ORDEM_DESENHO = List.of("feet", "bottom", "top", "outerwear", "facial_hair", "hair");

    public static final List<Peca> TODAS = List.of(
        new Peca("hair", "alex_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "ari_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "efe_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "kai_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "makena_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "noor_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "steve_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "sunny_hair", true, 0x862F0B, 0x862F0B),
        new Peca("hair", "zuri_hair", true, 0x862F0B, 0x862F0B),
        new Peca("facial_hair", "steve_beard", true, 0x862F0B, 0x862F0B),
        new Peca("top", "alex_shirt", false, 0, 0),
        new Peca("top", "ari_shirt", false, 0, 0),
        new Peca("top", "efe_shirt", false, 0, 0),
        new Peca("top", "kai_dress", false, 0, 0),
        new Peca("top", "makena_shirt", false, 0, 0),
        new Peca("top", "noor_shirt", false, 0, 0),
        new Peca("top", "steve_shirt", false, 0, 0),
        new Peca("top", "sunny_shirt", false, 0, 0),
        new Peca("top", "zuri_shirt", false, 0, 0),
        new Peca("outerwear", "efe_cardigan", false, 0, 0),
        new Peca("outerwear", "kai_jacket", false, 0, 0),
        new Peca("outerwear", "noor_jacket", false, 0, 0),
        new Peca("bottom", "alex_pants", false, 0, 0),
        new Peca("bottom", "ari_pants", false, 0, 0),
        new Peca("bottom", "efe_pants", false, 0, 0),
        new Peca("bottom", "makena_pants", false, 0, 0),
        new Peca("bottom", "noor_pants", false, 0, 0),
        new Peca("bottom", "steve_pants", false, 0, 0),
        new Peca("bottom", "sunny_pants", false, 0, 0),
        new Peca("bottom", "zuri_pants", false, 0, 0),
        new Peca("feet", "alex_shoes", false, 0, 0),
        new Peca("feet", "ari_shoes", false, 0, 0),
        new Peca("feet", "efe_shoes", false, 0, 0),
        new Peca("feet", "kai_shoes", false, 0, 0),
        new Peca("feet", "makena_shoes", false, 0, 0),
        new Peca("feet", "noor_shoes", false, 0, 0),
        new Peca("feet", "steve_shoes", false, 0, 0),
        new Peca("feet", "sunny_shoes", false, 0, 0),
        new Peca("feet", "zuri_shoes", false, 0, 0)
    );

    private CatalogoPecas() {
    }

    public static List<Peca> doSlot(String slot) {
        return TODAS.stream().filter(p -> p.slot().equals(slot)).toList();
    }

    /** Retorna null se a peça não existe. */
    public static Peca buscar(String slot, String id) {
        for (Peca peca : TODAS) {
            if (peca.slot().equals(slot) && peca.id().equals(id)) {
                return peca;
            }
        }
        return null;
    }
}
