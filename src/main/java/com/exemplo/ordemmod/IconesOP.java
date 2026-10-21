package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Os ícones dos poderes (pixel art 16x16 ou artes maiores, como as 128x128 das trilhas de combatente). Cada poder usa o arquivo
 * assets/ordemmod/textures/gui/poderes/&lt;id do poder&gt;.png; se o arquivo não existir, aparece o generico.png.
 * Os PNGs são gerados por tools/gerar_icones.py (dá para trocar por desenhos seus, é só manter o nome).
 * Só existe no cliente.
 *
 * Fabric 26.2: sem RenderSystem/setColor; o escurecimento (brilho &lt; 1) vai como tinta ARGB no blit.
 */
public final class IconesOP {
    private static final Identifier GENERICO = caminho("generico");
    private static final Map<String, Identifier> CACHE = new HashMap<>();
    /** Lado (em pixels) de cada textura; os ícones podem ser 16x16 (pixel art) ou maiores (ex.: 128x128 com borda). */
    private static final Map<Identifier, Integer> LADOS = new HashMap<>();

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

    /** O poder tem ícone próprio (e não o genérico)? */
    public static boolean temIcone(String id) {
        return textura(id) != GENERICO;
    }

    /** Desenha o ícone do poder; se ele não tiver um próprio, usa o do poder {@code reserva} (ex.: a primeira versão). */
    public static void desenharOuReserva(GuiGraphicsExtractor g, String id, String reserva, int x, int y, int tamanho,
            float brilho) {
        desenhar(g, temIcone(id) ? id : reserva, x, y, tamanho, brilho);
    }

    /** Lê o lado do PNG (quadrado) direto do cabeçalho; 16 se não der para ler. */
    private static int lado(Identifier tex) {
        Integer guardado = LADOS.get(tex);
        if (guardado != null) {
            return guardado;
        }
        int lado = 16;
        try (java.io.InputStream in = Minecraft.getInstance().getResourceManager().getResource(tex).orElseThrow().open()) {
            byte[] cab = in.readNBytes(24); // 8 da assinatura + 8 do chunk IHDR + largura(4) + altura(4)
            if (cab.length == 24) {
                int w = ((cab[16] & 0xFF) << 24) | ((cab[17] & 0xFF) << 16) | ((cab[18] & 0xFF) << 8) | (cab[19] & 0xFF);
                if (w > 0 && w <= 4096) {
                    lado = w;
                }
            }
        } catch (Exception ignorada) {
            // fica com 16
        }
        LADOS.put(tex, lado);
        return lado;
    }

    /** Desenha o ícone com {@code tamanho} pixels de lado; {@code brilho} 1 = normal, menor = escurecido. */
    public static void desenhar(GuiGraphicsExtractor g, String id, int x, int y, int tamanho, float brilho) {
        int v = Math.max(0, Math.min(255, Math.round(brilho * 255F)));
        int tinta = 0xFF000000 | (v << 16) | (v << 8) | v;
        Identifier tex = textura(id);
        int l = lado(tex);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0F, 0F, tamanho, tamanho, l, l, l, l, tinta);
    }
}
