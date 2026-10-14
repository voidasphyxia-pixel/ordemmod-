package com.exemplo.ordemmod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * A fonte do mod (Tektur, em assets/ordemmod/font/tektur.ttf, descrita em font/hud.json).
 * A tela de criação e a HUD escrevem tudo por aqui, então trocar a fonte é mexer só em hud.json.
 * Só existe no cliente.
 *
 * Fabric 26.2: GuiGraphics virou GuiGraphicsExtractor (drawString virou text, só aceita x/y inteiros),
 * ResourceLocation virou Identifier e Style.withFont recebe um FontDescription. As posições fracionárias
 * que o código antigo usava (halo de 0,9 px, textos escalados) agora passam por uma translação na pilha de matrizes.
 */
public final class FonteOP {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "hud");
    private static final Style ESTILO = estilo("hud");

    // estilo da cena do Marcado: Cinzel (títulos) e Cormorant Garamond (a "voz" da entidade, textos longos)
    private static final Style ESTILO_TITULO = estilo("titulo_hud");
    private static final Style ESTILO_ENTIDADE = estilo("entidade_hud");
    // Cinzel Black: rótulos pequenos da HUD (a Regular é fina demais em barras de 7 px)
    private static final Style ESTILO_ROTULO = estilo("titulo_hud_black");

    private FonteOP() {
    }

    private static Style estilo(String fonte) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, fonte)));
    }

    /** Texto em Cinzel Black (rótulos pequenos da HUD). */
    public static MutableComponent rotulo(String texto) {
        return Component.literal(texto).withStyle(ESTILO_ROTULO);
    }

    public static int larguraRotulo(Font fonte, String texto) {
        return fonte.width(rotulo(texto));
    }

    public static void desenharRotulo(GuiGraphicsExtractor g, Font fonte, String texto, float x, float y, int cor,
            boolean sombra) {
        escrever(g, fonte, rotulo(texto).getVisualOrderText(), x, y, cor, sombra);
    }

    /** Texto em Cinzel (títulos, rótulos). */
    public static MutableComponent titulo(String texto) {
        return Component.literal(texto).withStyle(ESTILO_TITULO);
    }

    /** Texto em Cormorant Garamond (descrições, dicas). */
    public static MutableComponent entidade(String texto) {
        return Component.literal(texto).withStyle(ESTILO_ENTIDADE);
    }

    public static int larguraTitulo(Font fonte, String texto) {
        return fonte.width(titulo(texto));
    }

    public static void desenharTitulo(GuiGraphicsExtractor g, Font fonte, String texto, float x, float y, int cor,
            boolean sombra) {
        escrever(g, fonte, titulo(texto).getVisualOrderText(), x, y, cor, sombra);
    }

    public static void desenharEntidade(GuiGraphicsExtractor g, Font fonte, String texto, float x, float y, int cor,
            boolean sombra) {
        escrever(g, fonte, entidade(texto).getVisualOrderText(), x, y, cor, sombra);
    }

    /** O texto já com a fonte do mod aplicada. */
    public static MutableComponent c(String texto) {
        return Component.literal(texto).withStyle(ESTILO);
    }

    /** Largura do texto nessa fonte (a fonte comum do jogo mediria errado). */
    public static int largura(Font fonte, String texto) {
        return fonte.width(c(texto));
    }

    /** Corta o texto para caber na largura, medindo com a fonte do mod. */
    public static String cortar(Font fonte, String texto, int largura) {
        return fonte.substrByWidth(c(texto), largura).getString();
    }

    public static void desenhar(GuiGraphicsExtractor g, Font fonte, String texto, float x, float y, int cor,
            boolean sombra) {
        escrever(g, fonte, c(texto).getVisualOrderText(), x, y, cor, sombra);
    }

    /** Linhas já quebradas por {@code fonte.split(FonteOP.c(...), largura)}: a fonte vem dentro delas. */
    public static void desenhar(GuiGraphicsExtractor g, Font fonte, FormattedCharSequence linha, float x, float y,
            int cor, boolean sombra) {
        escrever(g, fonte, linha, x, y, cor, sombra);
    }

    /**
     * Escreve nas coordenadas (x, y), que podem ser fracionárias: a parte inteira vai para g.text e o resto
     * vira uma translação. {@code cor} é ARGB e o alfa NÃO pode ser 0 (no 26.x alfa 0 some com o texto).
     */
    private static void escrever(GuiGraphicsExtractor g, Font fonte, FormattedCharSequence linha, float x, float y,
            int cor, boolean sombra) {
        int xi = Math.round(x);
        int yi = Math.round(y);
        float fx = x - xi;
        float fy = y - yi;
        if (Math.abs(fx) < 0.001F && Math.abs(fy) < 0.001F) {
            g.text(fonte, linha, xi, yi, cor, sombra);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(fx, fy);
        g.text(fonte, linha, xi, yi, cor, sombra);
        g.pose().popMatrix();
    }
}
