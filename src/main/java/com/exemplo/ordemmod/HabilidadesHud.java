package com.exemplo.ordemmod;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Os slots de habilidades ativas no canto inferior esquerdo da tela: PENTAGONOS (ponta para cima) empilhados em
 * diagonal, no estilo dos slots hexagonais de outro HUD, so que cada um mostra o simbolo da habilidade COBRINDO a forma
 * inteira (o icone e recortado pelo contorno do pentagono, nao fica uma caixinha quadrada dentro).
 *
 *  - 1o (grande): a habilidade ATUAL, a que a tecla USAR (R) dispara agora;
 *  - 2o (menor): a PROXIMA na fila, a que a tecla TROCAR (G) escolhe;
 *  - 3o (menor, mais apagado): a seguinte a essa.
 * A fila e circular (depois da ultima volta a primeira), igual a Personagem.proximaHabilidade().
 * Se o PE atual nao paga o custo, o icone escurece e a borda fica vermelha ("nao da para ativar agora").
 * O numero no canto de baixo e o custo em PE (como o "6" no slot do print de referencia).
 *
 * Como o pentagono e desenhado: o jogo so sabe pintar retangulos, entao a forma sai linha por linha (1 px de altura):
 * para cada linha calculo onde o contorno comeca e termina, pinto a borda e, por dentro, o icone recortado so nesse
 * trecho (enableScissor). Sao ~30 linhas por slot, barato para uma HUD.
 *
 * Todas as medidas estao nas constantes abaixo para voce ajustar (tamanho, margem, deslocamento diagonal).
 * Chamado de HudOrdem.desenhar; so existe no cliente.
 */
public final class HabilidadesHud {
    private HabilidadesHud() {
    }

    // ------------------------------------------------------------------ medidas (pixels da interface)
    /** Raio do pentagono grande (atual) e dos menores (fila). Largura = 1,90 * raio, altura = 1,81 * raio. */
    private static final float RAIO_ATUAL = 17F;
    private static final float RAIO_FILA = 12.5F;
    /** Espessura da borda (px). */
    private static final float BORDA = 1.6F;
    /** Distancia da borda esquerda e da borda de baixo da tela ate o bloco de slots. */
    private static final int MARGEM_ESQ = 8;
    private static final int MARGEM_BAIXO = 10;
    /** Vao vertical entre um pentagono e o seguinte. */
    private static final int VAO = 3;
    /** Quanto cada slot da fila anda para a direita em relacao ao anterior (a diagonal do print de referencia). */
    private static final int DESLOCA_X = 9;
    /** Quantas habilidades aparecem no maximo (atual + fila). */
    private static final int MAX_SLOTS = 3;

    private static final int COR_OSSO = 0xFFEDE8DC;
    private static final int COR_DOURADO = 0xFFC2B48C;
    private static final int COR_SEM_PE = 0xFFB3261E;
    private static final int COR_BORDA_FILA = 0xFF6A6A78;
    private static final int COR_FUNDO = 0xE0090910;
    private static final int COR_HALO = 0x99000000;
    private static final float ESCALA_CUSTO = 0.80F;

    // ------------------------------------------------------------------ desenho

    /** Desenha o bloco de pentagonos. {@code pe} e o PE atual, para saber quais a pessoa consegue pagar agora. */
    public static void desenhar(GuiGraphicsExtractor g, Minecraft mc, int altura, List<PacoteStatus.Slot> fila,
            int selecionada, int pe) {
        int n = fila.size();
        if (n == 0) {
            return; // sem habilidades ativas, nao aparece nada
        }
        int quantos = Math.min(n, MAX_SLOTS);
        int sel = Mth.clamp(selecionada, 0, n - 1);
        float pulso = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 400.0);

        // empilha de baixo para cima: o 1o slot (atual) e o de cima, entao calculo a altura total primeiro
        float[] raio = new float[quantos];
        float total = 0F;
        for (int i = 0; i < quantos; i++) {
            raio[i] = i == 0 ? RAIO_ATUAL : RAIO_FILA;
            total += alturaDe(raio[i]) + (i > 0 ? VAO : 0);
        }
        float y = altura - MARGEM_BAIXO - total;
        float larguraAtual = larguraDe(RAIO_ATUAL);

        for (int i = 0; i < quantos; i++) {
            PacoteStatus.Slot slot = fila.get((sel + i) % n);
            float r = raio[i];
            float cx = MARGEM_ESQ + larguraAtual / 2F + DESLOCA_X * i;
            float cy = y + r; // a ponta fica em y e o centro (circuncentro) r abaixo dela
            boolean paga = pe >= slot.custo();
            boolean atual = i == 0;
            // cada passo da fila fica um pouco mais apagado
            float brilho = (paga ? 1F : 0.38F) * (atual ? 1F : (i == 1 ? 0.85F : 0.65F));
            int corBorda = !paga ? COR_SEM_PE : atual ? COR_OSSO : COR_BORDA_FILA;
            pentagono(g, slot.id(), cx, cy, r, corBorda, brilho, atual, pulso, atual && paga);
            // o custo fica no canto de baixo-direita (perto do vertice inferior direito, em cx + 0,59 r)
            custo(g, mc.font, slot.custo(), cx + 0.59F * r + 4F, y + alturaDe(r) + 1F, paga);
            y += alturaDe(r) + VAO;
        }
    }

    // ------------------------------------------------------------------ geometria do pentagono

    // Pentagono regular com a ponta para cima: vertices em -90, -18, 54, 126, 198 graus, centro no circuncentro.
    private static final float[] ANG = {-90F, -18F, 54F, 126F, 198F};
    private static final float COS18 = (float) Math.cos(Math.toRadians(18));
    private static final float COS36 = (float) Math.cos(Math.toRadians(36));

    private static float larguraDe(float r) {
        return 2F * r * COS18;
    }

    private static float alturaDe(float r) {
        return r * (1F + COS36);
    }

    /** Onde a linha horizontal y cruza o pentagono de centro (cx, cy) e raio r: {xEsq, xDir}, ou null se nao cruza. */
    private static float[] extensao(float cx, float cy, float r, float y) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (int k = 0; k < 5; k++) {
            double a0 = Math.toRadians(ANG[k]);
            double a1 = Math.toRadians(ANG[(k + 1) % 5]);
            float x0 = cx + r * (float) Math.cos(a0);
            float y0 = cy + r * (float) Math.sin(a0);
            float x1 = cx + r * (float) Math.cos(a1);
            float y1 = cy + r * (float) Math.sin(a1);
            if ((y0 <= y && y < y1) || (y1 <= y && y < y0)) {
                float x = x0 + (y - y0) / (y1 - y0) * (x1 - x0);
                min = Math.min(min, x);
                max = Math.max(max, x);
            }
        }
        return min <= max ? new float[] { min, max } : null;
    }

    /**
     * Um pentagono completo: brilho por tras (so o atual), borda, fundo escuro e o icone recortado.
     * {@code r} e o raio externo; o icone ocupa o pentagono de dentro (r - BORDA) por inteiro.
     */
    private static void pentagono(GuiGraphicsExtractor g, String idIcone, float cx, float cy, float r, int corBorda,
            float brilho, boolean atual, float pulso, boolean brilha) {
        // 1) brilho suave em volta do atual que da para usar: pentagono um pouco maior, bem transparente
        if (brilha) {
            int alfa = 0x26 + Math.round(0x22 * pulso);
            preencher(g, cx, cy, r + 2.6F, (alfa << 24) | (COR_DOURADO & 0xFFFFFF));
        }
        // 2) borda = pentagono externo cheio; depois o interno por cima (sobra so o contorno)
        preencher(g, cx, cy, r, corBorda);
        float ri = r - BORDA;
        preencher(g, cx, cy, ri, COR_FUNDO);
        // 3) o icone, recortado linha por linha pelo pentagono interno. O quadrado do icone cobre o pentagono inteiro:
        //    lado = largura do pentagono interno, centrado no CENTRO DA CAIXA (nao no circuncentro).
        float lado = larguraDe(ri);
        float caixaCy = cy - r + alturaDe(ri) / 2F + BORDA; // meio da caixa do pentagono interno
        int ix = Math.round(cx - lado / 2F);
        int iy = Math.round(caixaCy - lado / 2F);
        int tam = Math.round(lado);
        int topo = (int) Math.floor(cy - ri);
        int base = (int) Math.ceil(cy - ri + alturaDe(ri));
        for (int py = topo; py < base; py++) {
            float[] e = extensao(cx, cy, ri, py + 0.5F);
            if (e == null) {
                continue;
            }
            int xa = Math.round(e[0]);
            int xb = Math.round(e[1]);
            if (xb <= xa) {
                continue;
            }
            g.enableScissor(xa, py, xb, py + 1);
            IconesOP.desenhar(g, idIcone, ix, iy, tam, brilho);
            g.disableScissor();
        }
        // 4) a selecionada ganha um fio claro por cima da borda (a ponta do pentagono marca "e esta")
        if (atual) {
            int topoBorda = 0x30FFFFFF;
            contorno(g, cx, cy, r, topoBorda);
        }
    }

    /** Pinta o pentagono inteiro (de centro cx,cy e raio r) com uma cor, uma linha de 1 px por vez. */
    private static void preencher(GuiGraphicsExtractor g, float cx, float cy, float r, int argb) {
        int topo = (int) Math.floor(cy - r);
        int base = (int) Math.ceil(cy - r + alturaDe(r));
        for (int py = topo; py < base; py++) {
            float[] e = extensao(cx, cy, r, py + 0.5F);
            if (e == null) {
                continue;
            }
            int xa = Math.round(e[0]);
            int xb = Math.round(e[1]);
            if (xb > xa) {
                g.fill(xa, py, xb, py + 1, argb);
            }
        }
    }

    /** Um fio de 1 px so no contorno (a diferenca entre dois pentagonos), usado como reflexo da borda do atual. */
    private static void contorno(GuiGraphicsExtractor g, float cx, float cy, float r, int argb) {
        float ri = r - 1F;
        int topo = (int) Math.floor(cy - r);
        int base = (int) Math.ceil(cy - r + alturaDe(r));
        for (int py = topo; py < base; py++) {
            float[] e = extensao(cx, cy, r, py + 0.5F);
            if (e == null) {
                continue;
            }
            float[] d = extensao(cx, cy, ri, py + 0.5F);
            int xa = Math.round(e[0]);
            int xb = Math.round(e[1]);
            if (d == null) {
                g.fill(xa, py, xb, py + 1, argb);
            } else {
                int da = Math.round(d[0]);
                int db = Math.round(d[1]);
                g.fill(xa, py, Math.max(xa, da), py + 1, argb);
                g.fill(Math.min(xb, db), py, xb, py + 1, argb);
            }
        }
    }

    /** O custo em PE no canto de baixo-direita do slot, com halo escuro para ler sobre qualquer icone. */
    private static void custo(GuiGraphicsExtractor g, Font fonte, int custo, float xDir, float yBase, boolean paga) {
        String texto = String.valueOf(custo);
        float w = FonteOP.largura(fonte, texto) * ESCALA_CUSTO;
        float x = xDir - w;
        float y = yBase - 8F * ESCALA_CUSTO;
        int cor = paga ? 0xFFFFE066 : 0xFFFF7A70;
        float[][] halo = { { -0.9F, 0F }, { 0.9F, 0F }, { 0F, -0.9F }, { 0F, 0.9F } };
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(ESCALA_CUSTO, ESCALA_CUSTO);
        for (float[] o : halo) {
            FonteOP.desenhar(g, fonte, texto, o[0], o[1], COR_HALO, false);
        }
        FonteOP.desenhar(g, fonte, texto, 0F, 0F, cor, false);
        g.pose().popMatrix();
    }
}
