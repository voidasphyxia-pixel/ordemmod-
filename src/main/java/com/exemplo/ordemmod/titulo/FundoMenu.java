package com.exemplo.ordemmod.titulo;

import com.exemplo.ordemmod.marcado.VfxMarcado;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.util.Util;

/**
 * Leva o visual da {@link TelaInicial} para as telas do proprio jogo que partem dela (Um jogador, Multijogador, Opcoes e
 * todos os submenus): fundo preto + NEVOA + POEIRA + vinheta no lugar do fundo do jogo, e a borda do mod nos campos de
 * texto. Substitui o antigo EstiloMenu (eventos do Forge). Os botoes de vidro sao sprites estaticos
 * (assets/minecraft/textures/gui/sprites/widget). Quem chama e o mixin {@code ScreenMixin}.
 * So vale fora do mundo (menus); as telas do proprio mod ja tem o visual delas.
 */
public final class FundoMenu {
    /** false devolve o fundo original do Minecraft nessas telas. */
    public static final boolean ATIVO = true;

    private static final int COR_FUNDO = 0xFF050507;
    private static final int COR_BORDA = 0xFF34343F;
    private static final int COR_SANGUE = 0xFFB0172B;
    private static final long INICIO = Util.getMillis();

    private FundoMenu() {
    }

    /** True se essa tela deve receber o fundo do mod (menus do jogo, nunca dentro de um mundo nem as telas do mod). */
    public static boolean estilizar(Screen s) {
        if (!ATIVO || s == null || s instanceof TitleScreen) {
            return false;
        }
        if (Minecraft.getInstance().level != null) {
            return false;
        }
        return !s.getClass().getName().startsWith("com.exemplo.ordemmod");
    }

    /** Limpa a tela com o fundo do mod (no lugar do fundo do jogo). */
    public static void fundo(Screen s, GuiGraphicsExtractor g) {
        int w = s.width;
        int h = s.height;
        float t = (Util.getMillis() - INICIO) / 1000F;
        float entrada = Math.min(1F, t / 0.6F); // so para a primeira tela depois de abrir o jogo

        g.fill(0, 0, w, h, COR_FUNDO);
        VfxMarcado.NEVOA.desenhar(g, t, 0.30F * entrada, 0, 0, w, h);
        VfxMarcado.POEIRA.desenhar(g, t, 0.45F * entrada, 0, 0, w, h);
        g.fillGradient(0, 0, w, h / 3, 0xDD000000, 0x00000000);
        g.fillGradient(0, h * 2 / 3, w, h, 0x00000000, 0xDD000000);
    }

    /** Depois dos widgets: campos de texto (busca de mundos, nome do mundo...) ganham a borda do mod no lugar da branca. */
    public static void bordas(Screen s, GuiGraphicsExtractor g) {
        for (GuiEventListener filho : s.children()) {
            if (filho instanceof EditBox caixa && caixa.visible) {
                int x = caixa.getX() - 1;
                int y = caixa.getY() - 1;
                int w = caixa.getWidth() + 2;
                int h = caixa.getHeight() + 2;
                int cor = caixa.isFocused() ? COR_SANGUE : COR_BORDA;
                g.fill(x, y, x + w, y + 1, cor);
                g.fill(x, y + h - 1, x + w, y + h, cor);
                g.fill(x, y + 1, x + 1, y + h - 1, cor);
                g.fill(x + w - 1, y + 1, x + w, y + h - 1, cor);
            }
        }
    }
}
