package com.exemplo.ordemmod.titulo;

import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.marcado.VfxMarcado;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Leva o visual da {@link TelaInicial} para as telas que partem dela (Um jogador, Multijogador, Mods, Opcoes e todos os
 * submenus, que sao telas do proprio Minecraft/Forge):
 * <ul>
 * <li>fundo: preto + NEVOA + POEIRA + vinheta, desenhado antes da tela (ScreenEvent.Render.Pre);</li>
 * <li>campos de texto: borda do mod (cinza, sangue quando em foco) no lugar da borda branca;</li>
 * <li>botoes e sliders: vidro escuro com borda e cantos de sangue (ver {@link EstiloTexturas});</li>
 * <li>o fundo de terra das listas e dos cabecalhos/rodapes some: a textura assets/minecraft/textures/gui/options_background.png
 * do mod e totalmente transparente (o shader do jogo descarta esses pixels), entao a nevoa aparece por tras de tudo.</li>
 * </ul>
 * So vale fora do mundo (menus); as telas do proprio mod (com.exemplo.ordemmod.*) ja tem o visual delas.
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
public final class EstiloMenu {
    /** false devolve o visual original do Minecraft nessas telas (o fundo de terra e os botoes cinza). */
    public static final boolean ATIVO = true;

    private static final int COR_FUNDO = 0xFF050507;
    private static final int COR_BORDA = 0xFF34343F;
    private static final int COR_SANGUE = 0xFFB0172B;
    private static final long INICIO = Util.getMillis();

    private EstiloMenu() {
    }

    /** True se essa tela deve receber o fundo do mod (menus do jogo, nunca dentro de um mundo nem as telas do mod). */
    static boolean estilizar(Screen s) {
        if (!ATIVO || s == null || s instanceof TitleScreen) {
            return false;
        }
        if (Minecraft.getInstance().level != null) {
            return false;
        }
        return !s.getClass().getName().startsWith("com.exemplo.ordemmod");
    }

    /** Antes de qualquer tela abrir, garante os botoes de vidro (barato: so faz algo na primeira vez ou apos F3+T). */
    @SubscribeEvent
    public static void aoAbrirTela(ScreenEvent.Opening e) {
        if (ATIVO && e.getNewScreen() != null) {
            EstiloTexturas.garantir();
        }
    }

    /**
     * ANTES da tela desenhar: limpa tudo com o fundo do mod. Tem que ser aqui (e nao em BackgroundRendered) porque
     * algumas telas (a de selecionar mundo, por exemplo) nem chamam renderBackground, e o fundo de terra do jogo e
     * transparente agora: sem isso sobravam restos do quadro anterior (a tela inicial, textos fantasmas).
     */
    @SubscribeEvent
    public static void aoComecarTela(ScreenEvent.Render.Pre e) {
        Screen s = e.getScreen();
        if (!estilizar(s)) {
            return;
        }
        GuiGraphics g = e.getGuiGraphics();
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

    /** Depois da tela: campos de texto (busca de mundos, nome do mundo...) ganham a borda do mod no lugar da branca. */
    @SubscribeEvent
    public static void aoTerminarTela(ScreenEvent.Render.Post e) {
        Screen s = e.getScreen();
        if (!estilizar(s)) {
            return;
        }
        GuiGraphics g = e.getGuiGraphics();
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
