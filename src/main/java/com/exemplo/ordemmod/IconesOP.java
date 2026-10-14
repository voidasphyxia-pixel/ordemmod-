package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Os ícones em pixel art (16x16) dos poderes. Cada poder usa o arquivo
 * assets/ordemmod/textures/gui/poderes/&lt;id do poder&gt;.png; se o arquivo não existir, aparece o generico.png.
 * Os PNGs são gerados por tools/gerar_icones.py (dá para trocar por desenhos seus, é só manter o nome).
 * Só existe no cliente.
 *
 * Fabric 26.2: sem RenderSystem/setColor; o escurecimento (brilho &lt; 1) vai como tinta ARGB no blit.
 */
public final class IconesOP {
    private static final Identifier GENERICO = caminho("generico");
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    private IconesOP() {
    }

    private static Identifier caminho(String id) {
        return Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "textures/gui/poderes/" + id + ".png");
    }

    /** A textura do poder (ou a genérica, se ele ainda não tem ícone). */
    public static Identifier textura(String id) {
        Identifier achada = CACHE.get(id);
        if (achada == null) {
            Identifier candidata = caminho(id);
            boolean existe = Minecraft.getInstance().getResourceManager().getResource(candidata).isPresent();
            achada = existe ? candidata : GENERICO;
            CACHE.put(id, achada);
        }
        return achada;
    }

    /** Desenha o ícone com {@code tamanho} pixels de lado; {@code brilho} 1 = normal, menor = escurecido. */
    public static void desenhar(GuiGraphicsExtractor g, String id, int x, int y, int tamanho, float brilho) {
        int v = Math.max(0, Math.min(255, Math.round(brilho * 255F)));
        int tinta = 0xFF000000 | (v << 16) | (v << 8) | v;
        g.blit(RenderPipelines.GUI_TEXTURED, textura(id), x, y, 0F, 0F, tamanho, tamanho, 16, 16, 16, 16, tinta);
    }
}
