package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

/**
 * Avisos curtos embaixo da mira, sutis: entram com fade, ficam um instante e saem com fade, na fonte do mod.
 * Até {@link #MAXIMO} linhas ao mesmo tempo (a mais antiga some quando chega uma nova além disso). Só existe no cliente.
 *
 * Fabric 26.2: desenhar() é um HudElement (registrado em HudOrdem.registrar, depois da mira); o evento de
 * "saiu do mundo" virou limpar(), chamado pelo entrypoint do cliente.
 */
public final class AvisoOrdem {
    private AvisoOrdem() {
    }

    private static final int MAXIMO = 3;
    private static final float ESCALA = 0.8F;
    private static final int DESCE_DA_MIRA = 16;     // distância entre o centro da tela e a primeira linha
    private static final int PASSO_LINHA = 10;
    private static final float FADE_ENTRADA = 250F;  // ms
    private static final float FADE_SAIDA = 700F;    // ms
    private static final float PERMANENCIA_BASE = 1400F;
    private static final float PERMANENCIA_POR_LETRA = 35F;
    private static final float CENTRO_GLIFO = -0.6F; // mesma ideia da HUD (veja HudOrdem)
    private static final int COR_PADRAO = 0xE8E2D4;
    private static final float TRANSPARENCIA_GERAL = 0.85F; // nunca totalmente opaco: é um aviso discreto

    private static final float[][] HALO = {
            {-0.9F, 0F}, {0.9F, 0F}, {0F, -0.9F}, {0F, 0.9F}};

    private record Aviso(String texto, int rgb, long inicio, float duracao) {
    }

    private static final List<Aviso> AVISOS = new ArrayList<>();

    /** Chamado quando o servidor manda um aviso (já na thread do cliente). */
    public static void receber(String texto, int cor) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        float duracao = FADE_ENTRADA + PERMANENCIA_BASE + PERMANENCIA_POR_LETRA * texto.length() + FADE_SAIDA;
        AVISOS.add(new Aviso(texto, cor < 0 ? COR_PADRAO : cor & 0xFFFFFF, Util.getMillis(), duracao));
        while (AVISOS.size() > MAXIMO) {
            AVISOS.remove(0);
        }
    }

    /** Ao sair do mundo, os avisos pendentes somem. */
    public static void limpar() {
        AVISOS.clear();
    }

    private static float opacidade(Aviso a, long agora) {
        float t = agora - a.inicio();
        if (t < FADE_ENTRADA) {
            return t / FADE_ENTRADA;
        }
        float restante = a.duracao() - t;
        if (restante < FADE_SAIDA) {
            return Math.max(0F, restante / FADE_SAIDA);
        }
        return 1F;
    }

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (AVISOS.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int largura = mc.getWindow().getGuiScaledWidth();
        int altura = mc.getWindow().getGuiScaledHeight();
        long agora = Util.getMillis();
        AVISOS.removeIf(a -> agora - a.inicio() >= a.duracao());
        Font fonte = mc.font;
        float larguraMaxima = largura * 0.6F;

        int linha = 0;
        for (Aviso a : AVISOS) {
            float o = Mth.clamp(opacidade(a, agora), 0F, 1F) * TRANSPARENCIA_GERAL;
            int alfa = Math.round(255F * o);
            if (alfa < 8) { // alfa muito baixo vira "opaco" ou some, então nem desenha
                linha++;
                continue;
            }
            String texto = a.texto();
            if (FonteOP.largura(fonte, texto) * ESCALA > larguraMaxima) {
                texto = FonteOP.cortar(fonte, texto, Math.round(larguraMaxima / ESCALA)) + "…";
            }
            float w = FonteOP.largura(fonte, texto) * ESCALA;
            float x = largura / 2F - w / 2F;
            float centroY = altura / 2F + DESCE_DA_MIRA + linha * PASSO_LINHA + fonte.lineHeight * ESCALA / 2F;
            float y = centroY - ESCALA * CENTRO_GLIFO;

            g.pose().pushMatrix();
            g.pose().translate(x, y);
            g.pose().scale(ESCALA, ESCALA);
            int halo = (Math.max(1, Math.round(alfa * 0.6F)) << 24);
            for (float[] d : HALO) {
                FonteOP.desenhar(g, fonte, texto, d[0], d[1], halo, false);
            }
            FonteOP.desenhar(g, fonte, texto, 0F, 0F, (alfa << 24) | a.rgb(), false);
            g.pose().popMatrix();
            linha++;
        }
    }
}
