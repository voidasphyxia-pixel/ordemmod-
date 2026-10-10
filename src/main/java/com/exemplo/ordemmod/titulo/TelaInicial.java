package com.exemplo.ordemmod.titulo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.exemplo.ordemmod.FonteOP;
import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.marcado.AudioMarcado;
import com.exemplo.ordemmod.marcado.VfxMarcado;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ModListScreen;
import net.minecraftforge.fml.ModList;

/**
 * A tela inicial do mod, no visual da cena do Marcado e das telas de criacao/ficha: preto com nevoa e poeira, o simbolo
 * maior (circulo ritual) respirando atras do titulo, titulo em Cinzel Black, botoes de vidro escuro com cantos de
 * ritual, glitch raro no titulo. O tema (TemaInicial) toca em laco. Substitui o TitleScreen via EventosTitulo.
 * Nao existe Minecraft Realms aqui (de proposito).
 */
@OnlyIn(Dist.CLIENT)
public class TelaInicial extends Screen {
    // cores (ARGB), as mesmas da TelaCriacao
    private static final int COR_FUNDO = 0xFF050507;
    private static final int COR_BORDA = 0xFF34343F;
    private static final int COR_ZONA = 0xA0141419;
    private static final int COR_OSSO = 0xFFEDE8DC;
    private static final int COR_APAGADO = 0xFF7C7C86;
    private static final int COR_DOURADO = 0xFFC2B48C;
    private static final int COR_SANGUE = 0xFFB0172B;
    private static final int COR_SANGUE_CLARO = 0xFFE0263E;

    private static final ResourceLocation SIMBOLO = new ResourceLocation(OrdemMod.MOD_ID,
            "textures/gui/titulo/simbolo_maior.png");
    private static final ResourceLocation HALO = new ResourceLocation(OrdemMod.MOD_ID, "textures/gui/marcado/halo.png");
    private static final Style ESTILO_LOGO = Style.EMPTY.withFont(new ResourceLocation(OrdemMod.MOD_ID, "titulo_logo"));
    private static final Style ESTILO_TITULO = Style.EMPTY.withFont(new ResourceLocation(OrdemMod.MOD_ID, "titulo_hud"));

    // ajustes finos (so da para acertar vendo no jogo)
    private static final float TRACK_LOGO = 6F;       // espaco extra entre as letras de ORDEM (px a escala 1)
    private static final float CAP_LOGO = 28F;        // altura das maiusculas da fonte do logo (size 40 * ~0,70)
    private static final float CAP_SUB = 8.75F;       // idem para a Cinzel normal (size 12,5)
    private static final float BASE_LOGO = 6.6F;      // da linha de texto ate a linha-base, fonte do logo
    private static final float BASE_SUB = 4.9F;       // idem, Cinzel normal
    /** Onde fica o centro do circulo interno dentro da textura do simbolo (fracao da altura). */
    private static final float CENTRO_SIMBOLO = 0.539F;
    private static final float ALFA_SIMBOLO = 0.40F;

    private static final class Item {
        final String chave;
        final String rotulo;
        final boolean principal;
        final Runnable acao;
        boolean ativo = true;
        int x, y, w, h;
        float anim; // 0..1 (hover)

        Item(String chave, String fallback, boolean principal, Runnable acao) {
            this.chave = chave;
            String t = I18n.exists(chave) ? Component.translatable(chave).getString() : fallback;
            this.rotulo = t.replace("...", "").replace("\u2026", "").trim().toUpperCase(Locale.ROOT);
            this.principal = principal;
            this.acao = acao;
        }

        boolean contem(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    private final List<Item> itens = new ArrayList<>();
    private int sel = -1;
    private int ultMx = -1, ultMy = -1;
    private long inicio;
    private long ultimoQuadro;
    private boolean tumTocou;
    private int cicloGlitchSom = -1;
    private float parX, parY;

    // layout
    private int entH, entW;
    private float sLogo, sSub, capOrd, capSub, gapA, gapB, blocoAlt, larguraOrdem, trackSub, cyTitulo, tamSimbolo;

    public TelaInicial() {
        super(Component.literal("Ordem Paranormal"));
    }

    @Override
    protected void init() {
        layout();
        TemaInicial.garantir();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== layout

    private void montarItens() {
        itens.clear();
        itens.add(new Item("menu.singleplayer", "Um jogador", true,
                () -> minecraft.setScreen(new SelectWorldScreen(this))));
        Item multi = new Item("menu.multiplayer", "Multijogador", false, () -> {
            if (minecraft.options.skipMultiplayerWarning) {
                minecraft.setScreen(new JoinMultiplayerScreen(this));
            } else {
                minecraft.setScreen(new SafetyScreen(this));
            }
        });
        multi.ativo = minecraft.allowsMultiplayer();
        itens.add(multi);
        itens.add(new Item("fml.menu.mods", "Mods", false, () -> minecraft.setScreen(new ModListScreen(this))));
        itens.add(new Item("menu.options", "Opcoes", false,
                () -> minecraft.setScreen(new OptionsScreen(this, minecraft.options))));
        itens.add(new Item("menu.quit", "Sair", false, () -> minecraft.stop()));
    }

    private void layout() {
        montarItens();
        int n = itens.size();
        entH = Mth.clamp(height / 14, 16, 24);
        int gap = Math.max(4, entH / 4);
        entW = Mth.clamp(width / 3, 150, 230);
        int menuH = n * entH + (n - 1) * gap;
        int menuTop = height - 28 - menuH;
        int x = (width - entW) / 2;
        for (int i = 0; i < n; i++) {
            Item it = itens.get(i);
            it.x = x;
            it.y = menuTop + i * (entH + gap);
            it.w = entW;
            it.h = entH;
        }

        float areaTop = 12F;
        float areaBot = menuTop - 12F;
        float areaH = Math.max(40F, areaBot - areaTop);
        float w1 = larguraEspacada("ORDEM", ESTILO_LOGO, TRACK_LOGO);
        sLogo = Mth.clamp(Math.min(Math.min(areaH / 150F, width * 0.42F / Math.max(1F, w1)), 2.4F), 0.6F, 2.4F);
        sSub = sLogo * 0.95F;
        capOrd = CAP_LOGO * sLogo;
        capSub = CAP_SUB * sSub;
        gapA = 0.32F * capOrd;
        gapB = 8F + 4F * sLogo;
        blocoAlt = capOrd + gapA + capSub + gapB + 9F;
        larguraOrdem = w1 * sLogo;
        float natSub = larguraEspacada("PARANORMAL", ESTILO_TITULO, 0F) * sSub;
        trackSub = Math.max(0F, (larguraOrdem * 0.94F - natSub) / 9F / sSub);
        cyTitulo = Mth.clamp(height * 0.455F, areaTop + blocoAlt / 2F, areaBot - blocoAlt / 2F);
        tamSimbolo = Math.min(Math.min(0.84F * height, 0.70F * width), (cyTitulo + 0.04F * height) / CENTRO_SIMBOLO);
    }

    // ================================================================== desenho

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        long agora = Util.getMillis();
        if (inicio == 0L) {
            inicio = agora;
        }
        float t = (agora - inicio) / 1000F;
        float dt = ultimoQuadro == 0L ? 0F : Math.min(0.1F, (agora - ultimoQuadro) / 1000F);
        ultimoQuadro = agora;
        float bg = suave(t / 2.2F);
        float ui = suave((t - 1.0F) / 1.6F);

        if (!tumTocou && t >= 1.0F) {
            tumTocou = true;
            AudioMarcado.tocar("tum", 0.5F, 1F);
        }
        if (mx != ultMx || my != ultMy) {
            ultMx = mx;
            ultMy = my;
            for (int i = 0; i < itens.size(); i++) {
                Item it = itens.get(i);
                if (it.ativo && it.contem(mx, my)) {
                    if (sel != i) {
                        sel = i;
                        AudioMarcado.tocar("ui_hover", 0.5F, 1F);
                    }
                    break;
                }
            }
        }
        parX += ((mx - width / 2F) / width * -14F - parX) * Math.min(1F, dt * 5F);
        parY += ((my - height / 2F) / height * -8F - parY) * Math.min(1F, dt * 5F);

        desenharFundo(g, t, bg);
        desenharLogo(g, t, ui);
        desenharMenu(g, dt, ui);
        desenharRodape(g, ui);
    }

    private void desenharFundo(GuiGraphics g, float t, float bg) {
        g.fill(0, 0, width, height, COR_FUNDO);
        VfxMarcado.NEVOA.desenhar(g, t, 0.34F * bg, 0, 0, width, height);

        float respira = 0.8F + 0.2F * (float) Math.sin(t * 1.2F);
        float cx = width / 2F + parX;
        float cy = cyTitulo + parY;
        int raio = (int) (tamSimbolo * 0.62F);
        halo(g, cx, cy, raio, raio, 0.20F * respira * bg, 0.70F, 0.80F, 1F);
        if (bg > 0.01F) {
            g.flush();
            minecraft.getTextureManager().getTexture(SIMBOLO).setFilter(true, false);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            g.setColor(0.85F, 0.90F, 1F, ALFA_SIMBOLO * respira * bg);
            g.pose().pushPose();
            g.pose().translate(cx - tamSimbolo / 2F, cy - CENTRO_SIMBOLO * tamSimbolo, 0F);
            int tam = Math.round(tamSimbolo);
            g.blit(SIMBOLO, 0, 0, tam, tam, 0F, 0F, 1024, 1024, 1024, 1024);
            g.pose().popPose();
            g.setColor(1F, 1F, 1F, 1F);
        }

        VfxMarcado.POEIRA.desenhar(g, t, 0.55F * bg, 0, 0, width, height);
        g.fillGradient(0, 0, width, height / 3, 0xDD000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xDD000000);
    }

    private void desenharLogo(GuiGraphics g, float t, float ui) {
        if (ui < 0.02F) {
            return;
        }
        int cx = width / 2;
        float respira = 0.85F + 0.15F * (float) Math.sin(t * 1.2F);
        float topo = cyTitulo - blocoAlt / 2F;
        float baseOrdem = topo + capOrd;
        float baseSub = baseOrdem + gapA + capSub;
        float yDiv = baseSub + gapB;
        float yOrdem = baseOrdem - BASE_LOGO * sLogo;
        float ySub = baseSub - BASE_SUB * sSub;

        halo(g, cx, cyTitulo, (int) (larguraOrdem * 0.75F), (int) (blocoAlt * 0.9F), 0.22F * ui * respira, 0.75F, 0.85F, 1F);

        float gl = glitch(t);
        float dx = 0F;
        if (gl > 0F) {
            dx = (float) Math.sin(t * 190F) * 2.5F * gl;
            float off = 3F * gl * sLogo / 2F;
            desenharEspacado(g, "ORDEM", ESTILO_LOGO, cx - off + dx, yOrdem, sLogo, TRACK_LOGO, alfa(0xFF30E0FF, ui * 0.65F));
            desenharEspacado(g, "ORDEM", ESTILO_LOGO, cx + off + dx, yOrdem, sLogo, TRACK_LOGO, alfa(0xFFFF2040, ui * 0.65F));
            Random r = new Random((long) (t * 60F));
            for (int i = 0; i < 4; i++) {
                int yy = Math.round(topo + r.nextInt(Math.max(1, (int) capOrd)));
                int xx = Math.round(cx - larguraOrdem / 2F) + r.nextInt(Math.max(1, (int) (larguraOrdem / 2F)));
                g.fill(xx, yy, xx + (int) (larguraOrdem / 3F) + r.nextInt(Math.max(1, (int) (larguraOrdem / 3F))),
                        yy + 1 + r.nextInt(3), alfa(0x50AAFFFF, ui));
            }
            int ciclo = (int) Math.floor((t + 27F) / 29F);
            if (ciclo != cicloGlitchSom) {
                cicloGlitchSom = ciclo;
                AudioMarcado.tocar("glitch", 0.25F, 1F);
            }
        }
        desenharEspacado(g, "ORDEM", ESTILO_LOGO, cx + dx, yOrdem, sLogo, TRACK_LOGO, alfa(COR_OSSO, ui));
        desenharEspacado(g, "PARANORMAL", ESTILO_TITULO, cx, ySub, sSub, trackSub, alfa(COR_DOURADO, ui));

        // divisoria com losango, igual a das telas de criacao
        int meio = cx;
        int y = Math.round(yDiv);
        int meioLado = Math.round(larguraOrdem * 0.30F);
        int linha = alfa(0xFF5A1620, ui);
        g.fill(meio - meioLado, y, meio - 9, y + 1, linha);
        g.fill(meio + 9, y, meio + meioLado, y + 1, linha);
        losango(g, meio, y, 4, alfa(COR_SANGUE_CLARO, ui));
    }

    private void desenharMenu(GuiGraphics g, float dt, float ui) {
        float sTxt = Mth.clamp(entH / 17F, 1F, 1.4F);
        for (int i = 0; i < itens.size(); i++) {
            Item it = itens.get(i);
            float ki = Mth.clamp(ui * 1.6F - i * 0.15F, 0F, 1F);
            float alvo = (i == sel && it.ativo) ? 1F : 0F;
            it.anim += (alvo - it.anim) * Math.min(1F, dt * 14F);
            if (ki < 0.02F) {
                continue;
            }
            int grow = Math.round(5F * it.anim);
            int x = it.x - grow;
            int w = it.w + grow * 2;
            int fundo;
            int borda;
            if (!it.ativo) {
                fundo = 0x900E0E12;
                borda = 0xFF24242C;
            } else if (it.principal) {
                fundo = misturar(0xE08E1424, 0xF0B01A30, it.anim);
                borda = misturar(COR_SANGUE, COR_SANGUE_CLARO, it.anim);
            } else {
                fundo = misturar(COR_ZONA, 0xE63A0D15, it.anim);
                borda = misturar(COR_BORDA, COR_SANGUE, it.anim);
            }
            caixa(g, x, it.y, w, it.h, alfa(fundo, ki), alfa(borda, ki));
            if (it.principal && it.ativo) {
                cantos(g, x, it.y, w, it.h, alfa(0xFFFFD0D6, ki));
            } else if (it.anim > 0.02F) {
                cantos(g, x, it.y, w, it.h, alfa(COR_SANGUE_CLARO, ki * it.anim));
            }
            int cor = !it.ativo ? COR_APAGADO : it.principal ? 0xFFFFFFFF : misturar(COR_DOURADO, COR_OSSO, it.anim);
            float cxi = it.x + it.w / 2F;
            float larg = larguraEspacada(it.rotulo, ESTILO_TITULO, 1.2F) * sTxt;
            desenharEspacado(g, it.rotulo, ESTILO_TITULO, cxi, it.y + it.h / 2F - 0.5F * sTxt, sTxt, 1.2F, alfa(cor, ki));
            if (it.anim > 0.02F && it.ativo) {
                float d = larg / 2F + 11F - (1F - it.anim) * 4F;
                int cm = alfa(COR_SANGUE_CLARO, ki * it.anim);
                losango(g, cxi - d, it.y + it.h / 2F, 2, cm);
                losango(g, cxi + d, it.y + it.h / 2F, 2, cm);
            }
        }
    }

    private void desenharRodape(GuiGraphics g, float ui) {
        if (ui < 0.05F) {
            return;
        }
        int cor = alfa(COR_APAGADO, ui);
        String esq = "Minecraft " + SharedConstants.getCurrentVersion().getName() + "  \u00b7  "
                + Component.translatable("fml.menu.loadingmods", ModList.get().size()).getString();
        FonteOP.desenhar(g, font, esq, 8, height - 11 + 0.5F, cor, false);
        String dir = "Copyright Mojang AB. Do not distribute!";
        FonteOP.desenhar(g, font, dir, width - 8 - FonteOP.largura(font, dir), height - 11 + 0.5F, cor, false);
    }

    // ================================================================== entrada

    @Override
    public boolean keyPressed(int tecla, int scan, int mods) {
        if (tecla == GLFW.GLFW_KEY_DOWN || tecla == GLFW.GLFW_KEY_TAB) {
            mover(1);
            return true;
        }
        if (tecla == GLFW.GLFW_KEY_UP) {
            mover(-1);
            return true;
        }
        if ((tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_KP_ENTER || tecla == GLFW.GLFW_KEY_SPACE)
                && sel >= 0 && sel < itens.size()) {
            ativar(itens.get(sel));
            return true;
        }
        return super.keyPressed(tecla, scan, mods);
    }

    private void mover(int passo) {
        int n = itens.size();
        int i = sel;
        for (int k = 0; k < n; k++) {
            i = Math.floorMod(i + passo, n);
            if (itens.get(i).ativo) {
                sel = i;
                AudioMarcado.tocar("ui_hover", 0.5F, 1F);
                return;
            }
        }
    }

    private void ativar(Item it) {
        if (!it.ativo) {
            return;
        }
        AudioMarcado.tocar("ui_select", 0.6F, 1F);
        it.acao.run();
    }

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao == 0) {
            for (Item it : itens) {
                if (it.ativo && it.contem(mx, my)) {
                    ativar(it);
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, botao);
    }

    // ================================================================== utilitarios de desenho

    private static float suave(float x) {
        float k = Mth.clamp(x, 0F, 1F);
        return k * k * (3F - 2F * k);
    }

    /** Glitch raro do titulo: primeiro em t = 2 s, depois a cada 29 s. Devolve 0 fora do glitch, ~0,45..1 durante. */
    private static float glitch(float t) {
        float c = (t + 27F) % 29F;
        if (c > 0.24F) {
            return 0F;
        }
        return 0.45F + 0.55F * (float) Math.abs(Math.sin(c * 60F));
    }

    private static int alfa(int argb, float k) {
        int a = Math.round(((argb >>> 24) & 255) * Mth.clamp(k, 0F, 1F));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    private static int misturar(int a, int b, float k) {
        k = Mth.clamp(k, 0F, 1F);
        int aa = Math.round(((a >>> 24) & 255) * (1 - k) + ((b >>> 24) & 255) * k);
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int gg = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bb = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (aa << 24) | (r << 16) | (gg << 8) | bb;
    }

    private float larguraEspacada(String txt, Style estilo, float track) {
        float w = 0F;
        for (int i = 0; i < txt.length(); i++) {
            w += font.width(Component.literal(txt.substring(i, i + 1)).withStyle(estilo));
            if (i < txt.length() - 1) {
                w += track;
            }
        }
        return w;
    }

    /** Texto letra a letra com espacamento, centrado em cx. y = topo da linha de texto (antes da escala). */
    private void desenharEspacado(GuiGraphics g, String txt, Style estilo, float cx, float y, float escala, float track,
            int cor) {
        if (((cor >>> 24) & 255) < 8) { // alfa muito baixo vira opaco no texto do Minecraft
            return;
        }
        float x = cx - larguraEspacada(txt, estilo, track) * escala / 2F;
        for (int i = 0; i < txt.length(); i++) {
            Component c = Component.literal(txt.substring(i, i + 1)).withStyle(estilo);
            g.pose().pushPose();
            g.pose().translate(x, y, 0F);
            g.pose().scale(escala, escala, 1F);
            g.drawString(font, c.getVisualOrderText(), 0F, 0F, cor, false);
            g.pose().popPose();
            x += (font.width(c) + track) * escala;
        }
    }

    /** Brilho suave (textura radial branca) somado ao fundo. */
    private void halo(GuiGraphics g, float cx, float cy, int rx, int ry, float alfa, float r, float gc, float b) {
        if (alfa <= 0.003F) {
            return;
        }
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.setColor(r, gc, b, alfa);
        g.blit(HALO, Math.round(cx) - rx, Math.round(cy) - ry, rx * 2, ry * 2, 0F, 0F, 128, 128, 128, 128);
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
    }

    private static void losango(GuiGraphics g, float cx, float cy, int r, int cor) {
        int x = Math.round(cx);
        int y = Math.round(cy);
        for (int dy = -r; dy <= r; dy++) {
            int hw = r - Math.abs(dy);
            g.fill(x - hw, y + dy, x + hw + 1, y + dy + 1, cor);
        }
    }

    private static void caixa(GuiGraphics g, int x, int y, int w, int h, int fundo, int borda) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fundo);
        g.fill(x, y, x + w, y + 1, borda);
        g.fill(x, y + h - 1, x + w, y + h, borda);
        g.fill(x, y + 1, x + 1, y + h - 1, borda);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, borda);
    }

    /** Pequenos colchetes nos quatro cantos (os "cantos de ritual"). */
    private static void cantos(GuiGraphics g, int x, int y, int w, int h, int cor) {
        int t = 4;
        g.fill(x - 1, y - 1, x + t, y, cor);
        g.fill(x - 1, y - 1, x, y + t, cor);
        g.fill(x + w - t, y - 1, x + w + 1, y, cor);
        g.fill(x + w, y - 1, x + w + 1, y + t, cor);
        g.fill(x - 1, y + h, x + t, y + h + 1, cor);
        g.fill(x - 1, y + h - t, x, y + h + 1, cor);
        g.fill(x + w - t, y + h, x + w + 1, y + h + 1, cor);
        g.fill(x + w, y + h - t, x + w + 1, y + h + 1, cor);
    }
}
