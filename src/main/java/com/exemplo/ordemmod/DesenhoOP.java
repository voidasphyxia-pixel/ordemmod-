package com.exemplo.ordemmod;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

/**
 * Ferramentas de desenho das telas (cliente). No Forge as telas desenhavam formas livres com Tesselator/RenderSystem;
 * no 26.2 so existem fill/fillGradient (retangulos) e blit, entao:
 *  - {@link #poligono} e {@link #linha} desenham por varredura, uma fila de pixels por vez com fill (cada pixel e pintado
 *    uma vez so, entao a transparencia nao "dobra" nas emendas);
 *  - {@link #halo} usa mistura normal (antes era aditiva, entao o brilho ficou um pouco mais fraco);
 *  - {@link #entidade} e o unico lugar que chama a API de desenhar a skin do jogador no inventario.
 */
public final class DesenhoOP {
    private DesenhoOP() {
    }

    /** ARGB a partir de componentes de 0 a 1 (alfa minimo 1, porque alfa 0 some com o desenho no 26.x). */
    public static int argb(float a, float r, float g, float b) {
        return (Mth.clamp(Math.round(a * 255F), 1, 255) << 24) | (Mth.clamp(Math.round(r * 255F), 0, 255) << 16)
                | (Mth.clamp(Math.round(g * 255F), 0, 255) << 8) | Mth.clamp(Math.round(b * 255F), 0, 255);
    }

    /**
     * Poligono simples (convexo ou nao, sem se cruzar) preenchido de {@code argb}. Regra par-impar por fila de pixels:
     * em cada fila acha onde as arestas cruzam o centro da fila e pinta de dois em dois cruzamentos.
     */
    public static void poligono(GuiGraphicsExtractor g, float[] xs, float[] ys, int argb) {
        int n = xs.length;
        if (n < 3 || ((argb >>> 24) & 0xFF) == 0) {
            return;
        }
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            minY = Math.min(minY, ys[k]);
            maxY = Math.max(maxY, ys[k]);
        }
        float[] cruz = new float[n];
        for (int y = (int) Math.floor(minY); y < (int) Math.ceil(maxY); y++) {
            float yc = y + 0.5F;
            int c = 0;
            for (int k = 0; k < n; k++) {
                int k2 = (k + 1) % n;
                float y1 = ys[k];
                float y2 = ys[k2];
                if ((yc >= y1 && yc < y2) || (yc >= y2 && yc < y1)) {
                    cruz[c++] = xs[k] + (yc - y1) * (xs[k2] - xs[k]) / (y2 - y1);
                }
            }
            Arrays.sort(cruz, 0, c);
            for (int i = 0; i + 1 < c; i += 2) {
                int x0 = Math.round(cruz[i]);
                int x1 = Math.round(cruz[i + 1]);
                if (x1 > x0) {
                    g.fill(x0, y, x1, y + 1, argb);
                }
            }
        }
    }

    /** Linha com espessura (em pixels), como um retangulo fino girado, preenchido por varredura. */
    public static void linha(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, float espessura, int argb) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double comprimento = Math.sqrt(dx * dx + dy * dy);
        if (comprimento < 0.001) {
            return;
        }
        float nx = (float) (-dy / comprimento * Math.max(espessura, 1F) / 2);
        float ny = (float) (dx / comprimento * Math.max(espessura, 1F) / 2);
        float[] xs = {(float) x1 - nx, (float) x1 + nx, (float) x2 + nx, (float) x2 - nx};
        float[] ys = {(float) y1 - ny, (float) y1 + ny, (float) y2 + ny, (float) y2 - ny};
        poligonoFino(g, xs, ys, argb);
    }

    /**
     * Igual a {@link #poligono}, mas garante pelo menos 1 pixel de largura em cada fila: numa linha quase vertical a fila
     * cobre menos de um pixel e, sem isso, a linha sairia cheia de buracos.
     */
    private static void poligonoFino(GuiGraphicsExtractor g, float[] xs, float[] ys, int argb) {
        int n = xs.length;
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int k = 0; k < n; k++) {
            minY = Math.min(minY, ys[k]);
            maxY = Math.max(maxY, ys[k]);
        }
        for (int y = (int) Math.floor(minY); y < (int) Math.ceil(maxY); y++) {
            float yc = y + 0.5F;
            float esquerda = Float.MAX_VALUE;
            float direita = -Float.MAX_VALUE;
            for (int k = 0; k < n; k++) {
                int k2 = (k + 1) % n;
                float ya = ys[k];
                float yb = ys[k2];
                if ((yc >= ya && yc < yb) || (yc >= yb && yc < ya)) {
                    float x = xs[k] + (yc - ya) * (xs[k2] - xs[k]) / (yb - ya);
                    esquerda = Math.min(esquerda, x);
                    direita = Math.max(direita, x);
                }
            }
            if (esquerda <= direita) {
                int x0 = Math.round(esquerda);
                int x1 = Math.max(x0 + 1, Math.round(direita));
                g.fill(x0, y, x1, y + 1, argb);
            }
        }
    }

    /** Brilho suave (textura radial branca) tingido de (r, g, b) com transparencia {@code alfa}, por tras do resto. */
    public static void halo(GuiGraphicsExtractor g, Identifier textura, float cx, float cy, int raio, float alfa, float r,
            float gc, float b) {
        g.blit(RenderPipelines.GUI_TEXTURED, textura, Math.round(cx) - raio, Math.round(cy) - raio, 0F, 0F, raio * 2,
                raio * 2, 128, 128, 128, 128, argb(alfa, r, gc, b));
    }

    /** Como {@link #halo}, mas esticado numa elipse de raios {@code rx} x {@code ry} (o "feixe de luz" da cena do Marcado). */
    public static void haloEliptico(GuiGraphicsExtractor g, Identifier textura, int x, int y, int rx, int ry, float alfa,
            float r, float gc, float b) {
        if (alfa < 0.01F || rx <= 0 || ry <= 0) {
            return;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, textura, x - rx, y - ry, 0F, 0F, rx * 2, ry * 2, 128, 128, 128, 128,
                argb(alfa, r, gc, b));
    }

    /**
     * Silhueta humanoide chapada (cabeca, tronco, bracos e pernas, como retangulos), de pe em ({@code cx}, {@code pe}) e com
     * {@code altura} pixels. {@code angulo} (radianos) gira o corpo em torno do eixo vertical: o corpo todo afina e engorda
     * em x, o que da a ideia de ele virando. No Forge a cena desenhava o proprio jogador com uma textura branca; no 26.2 isso
     * exigiria trocar o RenderType da entidade (nao existe mais), por isso a silhueta e desenhada na mao.
     */
    public static void silhueta(GuiGraphicsExtractor g, float cx, float pe, float altura, float angulo, int argb) {
        float u = altura / 32F;
        float f = 0.35F + 0.65F * Math.abs((float) Math.cos(angulo));
        // {x0, y0, x1, y1} em unidades de 1/32 da altura: x centrado em 0, y de cima (0) para baixo (32)
        float[][] partes = {
                {-4F, 0F, 4F, 8F},      // cabeca
                {-4F, 8F, 4F, 20F},     // tronco
                {-8F, 8F, -4F, 20F},    // braco esquerdo
                {4F, 8F, 8F, 20F},      // braco direito
                {-4F, 20F, 0F, 32F},    // perna esquerda
                {0F, 20F, 4F, 32F}};    // perna direita
        for (float[] p : partes) {
            int x0 = Math.round(cx + p[0] * u * f);
            int x1 = Math.round(cx + p[2] * u * f);
            int y0 = Math.round(pe - altura + p[1] * u);
            int y1 = Math.round(pe - altura + p[3] * u);
            if (x1 > x0 && y1 > y0) {
                g.fill(x0, y0, x1, y1, argb);
            }
        }
    }

    /**
     * A skin do jogador (ou outra entidade viva) num quadro, olhando para o mouse. A caixa e (x0, y0)-(x1, y1);
     * {@code tamanho} e o numero de pixels por bloco. ATENCAO: o nome do metodo do jogo (extractEntityInInventoryFollowsMouse)
     * veio da convencao render* -> extract* do 26.x e NAO foi conferido; se o build reclamar, e so aqui.
     */
    public static void entidade(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int tamanho, float mouseX,
            float mouseY, LivingEntity entidade) {
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, tamanho, 0.0625F, mouseX, mouseY, entidade);
    }
}
