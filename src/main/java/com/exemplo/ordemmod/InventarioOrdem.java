package com.exemplo.ordemmod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ContainerScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * O inventário no mesmo visual da HUD: painel de vidro escuro com borda fina, casas escuras e textos na fonte do mod.
 * Vale para o inventário do jogador e para baús/barris (ContainerScreen); as outras telas (fornalha, bancada,
 * criativo...) continuam do jogo, porque têm setas e abas desenhadas na textura delas.
 *
 * Como funciona: depois que o jogo desenha a textura dele, cobrimos tudo com o nosso painel e redesenhamos as casas
 * nas posições reais dos slots. Só existe no cliente.
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
public final class InventarioOrdem {
    private InventarioOrdem() {
    }

    private static final int FUNDO_TOPO = 0xFF1B1B23;
    private static final int FUNDO_BASE = 0xFF0B0B10;
    private static final int BORDA_TOPO = 0xFF42424E;
    private static final int BORDA_LADO = 0xFF2A2A33;
    private static final int BORDA_BASE = 0xFF17171D;
    private static final int CASA_TOPO = 0xFF111118;
    private static final int CASA_BASE = 0xFF08080B;
    private static final int BORDA_RESULTADO = 0xFFB8991F;
    private static final int COR_OSSO = 0xFFE8E2D4;
    private static final int COR_SETA = 0xFF6A6A78;

    private static final float ESCALA_TEXTO = 0.8F;
    /** Mesma ideia da HUD: o centro das letras fica um pouco acima do topo da linha de texto. */
    private static final float CENTRO_GLIFO = -0.6F;

    private static boolean aplica(AbstractContainerScreen<?> tela) {
        return tela instanceof InventoryScreen || tela instanceof ContainerScreen;
    }

    // ------------------------------------------------------------------ fundo (depois da textura do jogo)

    @SubscribeEvent
    public static void fundo(ContainerScreenEvent.Render.Background e) {
        AbstractContainerScreen<?> tela = e.getContainerScreen();
        if (!aplica(tela)) {
            return;
        }
        GuiGraphics g = e.getGuiGraphics();
        int x = tela.getGuiLeft();
        int y = tela.getGuiTop();
        int w = tela.getXSize();
        int h = tela.getYSize();

        painel(g, x, y, w, h);

        if (tela instanceof InventoryScreen) {
            // caixa do boneco + o boneco de novo (a textura do jogo, com ele dentro, acabou de ser coberta)
            caixa(g, x + 26, y + 8, 49, 70);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, x + 51, y + 75, 30,
                    (float) (x + 51) - e.getMouseX(), (float) (y + 75 - 50) - e.getMouseY(),
                    Minecraft.getInstance().player);
            seta(g, x + 134, y + 36);
        }

        for (Slot slot : tela.getMenu().slots) {
            casa(g, x + slot.x - 1, y + slot.y - 1, slot instanceof ResultSlot);
        }
    }

    /** Painel de vidro escuro: halo fraco por fora, borda fina (clara em cima, escura embaixo) e reflexo no topo. */
    private static void painel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 2, y - 2, x + w + 2, y + h + 2, 0x20E8E2D4);
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDA_LADO);
        g.fill(x - 1, y - 1, x + w + 1, y, BORDA_TOPO);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, BORDA_BASE);
        g.fillGradient(x, y, x + w, y + h, FUNDO_TOPO, FUNDO_BASE);
        g.fillGradient(x, y, x + w, y + h / 3, 0x18FFFFFF, 0x00FFFFFF);
    }

    /** Caixa funda (onde fica o boneco). */
    private static void caixa(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, BORDA_LADO);
        g.fill(x - 1, y - 1, x + w + 1, y, BORDA_BASE);
        g.fill(x - 1, y + h, x + w + 1, y + h + 1, BORDA_TOPO);
        g.fillGradient(x, y, x + w, y + h, 0xFF0D0D13, 0xFF050508);
    }

    /** Uma casa de item (18x18, o item tem 16): vidro bem escuro com borda fina. */
    private static void casa(GuiGraphics g, int x, int y, boolean resultado) {
        g.fill(x, y, x + 18, y + 18, resultado ? BORDA_RESULTADO : BORDA_LADO);
        if (!resultado) {
            g.fill(x, y, x + 18, y + 1, BORDA_TOPO);
            g.fill(x, y + 17, x + 18, y + 18, BORDA_BASE);
        }
        g.fillGradient(x + 1, y + 1, x + 17, y + 17, CASA_TOPO, CASA_BASE);
    }

    /** A seta da grade de criação até o resultado (a do jogo estava na textura que foi coberta). */
    private static void seta(GuiGraphics g, int x, int y) {
        g.fill(x, y - 1, x + 10, y + 1, COR_SETA);
        g.fill(x + 10, y - 3, x + 11, y + 3, COR_SETA);
        g.fill(x + 11, y - 2, x + 12, y + 2, COR_SETA);
        g.fill(x + 12, y - 1, x + 13, y + 1, COR_SETA);
    }

    // ------------------------------------------------------------------ textos (depois dos textos do jogo)

    @SubscribeEvent
    public static void frente(ContainerScreenEvent.Render.Foreground e) {
        AbstractContainerScreen<?> tela = e.getContainerScreen();
        if (!aplica(tela)) {
            return;
        }
        GuiGraphics g = e.getGuiGraphics();
        Font fonte = Minecraft.getInstance().font;
        int h = tela.getYSize();
        String titulo = tela.getTitle().getString();

        // aqui o desenho já está deslocado para o canto do painel; os textos do jogo (cinza escuro, ilegíveis no
        // fundo escuro) são cobertos com a mesma cor do painel e reescritos na fonte do mod
        if (tela instanceof InventoryScreen) {
            rotulo(g, fonte, 97, 8, titulo, h);
        } else {
            rotulo(g, fonte, 8, 6, titulo, h);
            rotulo(g, fonte, 8, h - 94, Component.translatable("container.inventory").getString(), h);
        }
    }

    private static void rotulo(GuiGraphics g, Font fonte, int x, int y, String texto, int alturaPainel) {
        int largura = fonte.width(texto);
        int topo = y - 1;
        int base = y + fonte.lineHeight;
        g.fillGradient(x - 1, topo, x + largura + 2, base, corFundo(topo, alturaPainel), corFundo(base, alturaPainel));

        float centroY = y + fonte.lineHeight / 2F;
        g.pose().pushPose();
        g.pose().translate(x, centroY - ESCALA_TEXTO * CENTRO_GLIFO, 0F);
        g.pose().scale(ESCALA_TEXTO, ESCALA_TEXTO, 1F);
        FonteOP.desenhar(g, fonte, texto, 0F, 0F, COR_OSSO, false);
        g.pose().popPose();
    }

    /** A cor do degradê do painel na altura {@code y} (relativa ao topo do painel). */
    private static int corFundo(int y, int alturaPainel) {
        float t = Math.max(0F, Math.min(1F, y / (float) alturaPainel));
        int r = Math.round(((FUNDO_TOPO >> 16) & 255) * (1 - t) + ((FUNDO_BASE >> 16) & 255) * t);
        int gr = Math.round(((FUNDO_TOPO >> 8) & 255) * (1 - t) + ((FUNDO_BASE >> 8) & 255) * t);
        int b = Math.round((FUNDO_TOPO & 255) * (1 - t) + (FUNDO_BASE & 255) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }
}
