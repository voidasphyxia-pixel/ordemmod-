package com.exemplo.ordemmod.aparencia;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import com.exemplo.ordemmod.OrdemMod;
import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/**
 * Monta a skin 64x64 do jogador empilhando camadas: corpo (tingido com o tom de pele), sapato,
 * calça, camiseta, casaco, barba e cabelo. Cada camada já vem no formato de skin do Minecraft.
 *
 * A conta de tingir cor (matiz/saturação/brilho) é baseada na classe PersonaColors do mod
 * Bedrock Skins, de BrandonItaly, licença MIT (ver licencas/BedrockSkins-MIT.txt).
 */
final class ComposicaoSkin {
    /** O cinza do corpo base (assets/ordemmod/appearance/base_body.png). */
    private static final int CINZA_BASE = 224;
    private static final Map<String, NativeImage> CAMADAS = new HashMap<>();

    private ComposicaoSkin() {
    }

    static NativeImage compor(AparenciaData dados) throws IOException {
        NativeImage saida = new NativeImage(64, 64, true);

        // 1) corpo com o tom de pele
        int pele = Paletas.PELE[dados.getPele()];
        int pr = pele >> 16 & 255, pg = pele >> 8 & 255, pb = pele & 255;
        NativeImage corpo = camada("base_body.png");
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                int pixel = corpo.getPixelRGBA(x, y);
                int alfa = pixel >>> 24;
                if (alfa == 0) {
                    continue;
                }
                int r = Math.min(255, (pixel & 255) * pr / CINZA_BASE);
                int g = Math.min(255, (pixel >> 8 & 255) * pg / CINZA_BASE);
                int b = Math.min(255, (pixel >> 16 & 255) * pb / CINZA_BASE);
                saida.setPixelRGBA(x, y, alfa << 24 | b << 16 | g << 8 | r);
            }
        }

        // 2) as peças, de baixo para cima
        for (String slot : CatalogoPecas.ORDEM_DESENHO) {
            String id = dados.getPeca(slot);
            CatalogoPecas.Peca peca = id == null ? null : CatalogoPecas.buscar(slot, id);
            if (peca == null) {
                continue;
            }
            NativeImage imagem = camada(slot + "/" + id + ".png");
            NativeImage mascara = peca.tingivel() ? camada(slot + "/" + id + "_tint.png") : null;
            float[] base = hsv(peca.corBase());
            float[] escolhida = hsv(dados.getCor(slot));
            for (int y = 0; y < 64; y++) {
                for (int x = 0; x < 64; x++) {
                    int pixel = imagem.getPixelRGBA(x, y);
                    int alfa = pixel >>> 24;
                    if (alfa == 0) {
                        continue;
                    }
                    int r = pixel & 255, g = pixel >> 8 & 255, b = pixel >> 16 & 255;
                    if (mascara != null) {
                        int quantia = mascara.getPixelRGBA(x, y) & 255; // o canal vermelho da máscara
                        if (quantia > 0) {
                            int tingido = tingir(r, g, b, quantia, base, escolhida);
                            r = tingido >> 16 & 255;
                            g = tingido >> 8 & 255;
                            b = tingido & 255;
                        }
                    }
                    sobrepor(saida, x, y, r, g, b, alfa);
                }
            }
        }
        return saida;
    }

    /** Desenha um pixel por cima do que já existe (mistura de transparência normal). */
    private static void sobrepor(NativeImage imagem, int x, int y, int r, int g, int b, int alfa) {
        int destino = imagem.getPixelRGBA(x, y);
        int alfaDestino = destino >>> 24;
        if (alfa == 255 || alfaDestino == 0) {
            imagem.setPixelRGBA(x, y, alfa << 24 | b << 16 | g << 8 | r);
            return;
        }
        int alfaFinal = alfa + alfaDestino * (255 - alfa) / 255;
        int dr = destino & 255, dg = destino >> 8 & 255, db = destino >> 16 & 255;
        int nr = (r * alfa + dr * alfaDestino * (255 - alfa) / 255) / alfaFinal;
        int ng = (g * alfa + dg * alfaDestino * (255 - alfa) / 255) / alfaFinal;
        int nb = (b * alfa + db * alfaDestino * (255 - alfa) / 255) / alfaFinal;
        imagem.setPixelRGBA(x, y, alfaFinal << 24 | nb << 16 | ng << 8 | nr);
    }

    // ---- tingir (baseado no PersonaColors do Bedrock Skins) ----

    private static float[] hsv(int rgb) {
        return Color.RGBtoHSB(rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255, null);
    }

    private static int tingir(int r, int g, int b, int quantia, float[] base, float[] escolhida) {
        float[] hsv = Color.RGBtoHSB(r, g, b, null);
        float matiz = (hsv[0] + escolhida[0] - base[0]) % 1f;
        if (matiz < 0) {
            matiz += 1f;
        }
        int novo = Color.HSBtoRGB(matiz, transferir(hsv[1], base[1], escolhida[1]),
                transferir(hsv[2], base[2], escolhida[2]));
        int nr = misturar(r, novo >>> 16 & 255, quantia);
        int ng = misturar(g, novo >>> 8 & 255, quantia);
        int nb = misturar(b, novo & 255, quantia);
        return nr << 16 | ng << 8 | nb;
    }

    private static float transferir(float valor, float base, float escolhida) {
        return base <= 1e-6f ? escolhida : Math.min(1f, valor * escolhida / base);
    }

    private static int misturar(int de, int para, int quantia) {
        return (de * (255 - quantia) + para * quantia + 127) / 255;
    }

    // ---- leitura das camadas (guardadas em cache) ----

    private static NativeImage camada(String caminho) throws IOException {
        NativeImage guardada = CAMADAS.get(caminho);
        if (guardada != null) {
            return guardada;
        }
        ResourceLocation local = new ResourceLocation(OrdemMod.MOD_ID, "appearance/" + caminho);
        try (InputStream entrada = Minecraft.getInstance().getResourceManager().getResourceOrThrow(local).open()) {
            NativeImage imagem = NativeImage.read(entrada);
            CAMADAS.put(caminho, imagem);
            return imagem;
        }
    }
}
