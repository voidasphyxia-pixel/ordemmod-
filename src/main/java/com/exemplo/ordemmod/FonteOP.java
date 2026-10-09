package com.exemplo.ordemmod;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

/**
 * A fonte do mod (Tektur, em assets/ordemmod/font/tektur.ttf, descrita em font/hud.json).
 * A tela de criação e a HUD escrevem tudo por aqui, então trocar a fonte é mexer só em hud.json.
 * Só existe no cliente.
 */
public final class FonteOP {
    public static final ResourceLocation ID = new ResourceLocation(OrdemMod.MOD_ID, "hud");
    private static final Style ESTILO = Style.EMPTY.withFont(ID);

    private FonteOP() {
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

    public static void desenhar(GuiGraphics g, Font fonte, String texto, float x, float y, int cor, boolean sombra) {
        g.drawString(fonte, c(texto).getVisualOrderText(), x, y, cor, sombra);
    }

    /** Linhas já quebradas por {@code fonte.split(FonteOP.c(...), largura)}: a fonte vem dentro delas. */
    public static void desenhar(GuiGraphics g, Font fonte, FormattedCharSequence linha, float x, float y, int cor,
            boolean sombra) {
        g.drawString(fonte, linha, x, y, cor, sombra);
    }
}
