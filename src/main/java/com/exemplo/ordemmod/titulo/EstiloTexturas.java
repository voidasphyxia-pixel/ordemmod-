package com.exemplo.ordemmod.titulo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Troca, em memoria, a arte dos botoes e sliders do Minecraft pelo estilo da {@link TelaInicial}: vidro escuro com
 * borda fina, e no hover borda de sangue com cantos de ritual. Nada em disco e alterado.
 * <ul>
 * <li>textures/gui/widgets.png: so as 3 faixas de botao (y 46..105: desativado, normal, hover). O resto da imagem
 * (hotbar, etc.) e copiado do original, entao a HUD nao muda.</li>
 * <li>textures/gui/slider.png (200x80): trilho normal/realcado e o cursor normal/realcado.</li>
 * <li>tela de criar mundo: tab_button.png, tab_header_background.png e checkbox.png sao recoloridas pelo brilho; os
 * separadores header_separator.png / footer_separator.png viram uma linha fina de sangue escuro.</li>
 * </ul>
 * Se o arquivo original nao tiver o tamanho esperado (resource pack de outra resolucao, por exemplo), aquela textura
 * fica como esta. Depois de F3+T o jogo recarrega os originais; na proxima tela que abrir o estilo volta sozinho.
 * Para desligar: EstiloMenu.ATIVO = false.
 */
@OnlyIn(Dist.CLIENT)
final class EstiloTexturas {
    private static final ResourceLocation WIDGETS = new ResourceLocation("textures/gui/widgets.png");
    private static final ResourceLocation SLIDER = new ResourceLocation("textures/gui/slider.png");

    // cores (ARGB), as da TelaInicial
    private static final int COR_BORDA = 0xFF34343F;
    private static final int COR_SANGUE = 0xFFB0172B;
    private static final int COR_SANGUE_CLARO = 0xFFE0263E;

    private static final int NORMAL_FUNDO = 0xC0141419;
    private static final int HOVER_FUNDO = 0xE63A0D15;
    private static final int OFF_FUNDO = 0x900E0E12;
    private static final int OFF_BORDA = 0xFF24242C;

    // tela de criar mundo (1.20.1): abas, caixa de marcar e cabecalho recebem uma "recoloracao" do desenho original
    // (o brilho de cada pixel vira a paleta do mod), entao nao importa como o Minecraft organizou os quadros do arquivo.
    private static final ResourceLocation[] COLORIDAS = {
            new ResourceLocation("textures/gui/tab_button.png"),
            new ResourceLocation("textures/gui/tab_header_background.png"),
            new ResourceLocation("textures/gui/checkbox.png"),
    };
    // linhas que separam o cabecalho/rodape do corpo (32x2 no jogo): viram uma linha fina de sangue escuro
    private static final ResourceLocation[] SEPARADORES = {
            new ResourceLocation("textures/gui/header_separator.png"),
            new ResourceLocation("textures/gui/footer_separator.png"),
    };
    private static final java.util.Map<ResourceLocation, DynamicTexture> feitas = new java.util.HashMap<>();
    private static final java.util.Set<ResourceLocation> ausentes = new java.util.HashSet<>();

    private static DynamicTexture widgets;
    private static DynamicTexture slider;

    private EstiloTexturas() {
    }

    static void garantir() {
        Minecraft mc = Minecraft.getInstance();
        TextureManager tm = mc.getTextureManager();
        try {
            AbstractTexture atual = tm.getTexture(WIDGETS, null);
            if (widgets == null || atual != widgets) {
                DynamicTexture novo = montarWidgets(mc);
                if (novo != null) {
                    widgets = novo;
                    tm.register(WIDGETS, widgets);
                }
            }
            atual = tm.getTexture(SLIDER, null);
            if (slider == null || atual != slider) {
                DynamicTexture novo = montarSlider(mc);
                if (novo != null) {
                    slider = novo;
                    tm.register(SLIDER, slider);
                }
            }
            for (ResourceLocation r : COLORIDAS) {
                garantirColorida(mc, tm, r, false);
            }
            for (ResourceLocation r : SEPARADORES) {
                garantirColorida(mc, tm, r, true);
            }
        } catch (RuntimeException | IOException ex) {
            System.out.println("Ordem Mod: nao deu para estilizar os botoes do menu (" + ex + ")");
        }
    }

    private static NativeImage ler(Minecraft mc, ResourceLocation local) throws IOException {
        Optional<Resource> r = mc.getResourceManager().getResource(local);
        if (r.isEmpty()) {
            return null;
        }
        try (InputStream in = r.get().open()) {
            return NativeImage.read(in);
        }
    }

    private static void garantirColorida(Minecraft mc, TextureManager tm, ResourceLocation r, boolean separador)
            throws IOException {
        if (ausentes.contains(r)) {
            return;
        }
        AbstractTexture atual = tm.getTexture(r, null);
        DynamicTexture feita = feitas.get(r);
        if (feita != null && atual == feita) {
            return;
        }
        NativeImage img = ler(mc, r);
        if (img == null) {
            ausentes.add(r); // esse arquivo nao existe nessa versao: nao procura de novo
            aviso(r.getPath() + " nao existe nessa versao (ignorado)");
            return;
        }
        aviso(r.getPath() + " ok: " + img.getWidth() + "x" + img.getHeight());
        if (separador) {
            separador(img);
        } else {
            recolorir(img);
        }
        feita = new DynamicTexture(img);
        feitas.put(r, feita);
        tm.register(r, feita);
    }

    /** Linha fina de sangue escuro na metade de cima e transparente embaixo (no jogo: 2 px de altura). */
    private static void separador(NativeImage img) {
        int meio = Math.max(1, img.getHeight() / 2);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                img.setPixelRGBA(x, y, argbParaAbgr(y < meio ? 0xFF5A1620 : 0x00000000));
            }
        }
    }

    /**
     * Troca as cores pelo brilho: escuro = vidro do mod, meio = cinza da borda, claro = sangue. Mantem o alfa e a
     * estrutura do desenho (qual aba esta selecionada, onde esta o "check" etc. continuam distinguiveis).
     */
    private static void recolorir(NativeImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int abgr = img.getPixelRGBA(x, y);
                int a = (abgr >>> 24) & 255;
                if (a == 0) {
                    continue;
                }
                int b = (abgr >> 16) & 255;
                int g = (abgr >> 8) & 255;
                int r = abgr & 255;
                float lum = (0.30F * r + 0.59F * g + 0.11F * b) / 255F;
                int cor = lum < 0.5F ? misturar(0xFF0C0C10, COR_BORDA, lum / 0.5F)
                        : misturar(COR_BORDA, COR_SANGUE_CLARO, (lum - 0.5F) / 0.5F);
                img.setPixelRGBA(x, y, argbParaAbgr((a << 24) | (cor & 0xFFFFFF)));
            }
        }
    }

    private static int misturar(int c1, int c2, float k) {
        k = Math.max(0F, Math.min(1F, k));
        int r = Math.round(((c1 >> 16) & 255) * (1 - k) + ((c2 >> 16) & 255) * k);
        int g = Math.round(((c1 >> 8) & 255) * (1 - k) + ((c2 >> 8) & 255) * k);
        int b = Math.round((c1 & 255) * (1 - k) + (c2 & 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static DynamicTexture montarWidgets(Minecraft mc) throws IOException {
        NativeImage img = ler(mc, WIDGETS);
        if (img == null) {
            aviso("widgets.png nao encontrado");
            return null;
        }
        int w = img.getWidth();
        if (w != img.getHeight() || w % 256 != 0) { // 256x256 (ou 512, 1024... em pacotes HD)
            aviso("widgets.png com tamanho inesperado: " + w + "x" + img.getHeight());
            img.close();
            return null;
        }
        aviso("widgets.png ok: " + w + "x" + img.getHeight());
        int e = w / 256;
        botao(img, 0, 46 * e, e, OFF_FUNDO, OFF_BORDA, 0);                       // desativado
        botao(img, 0, 66 * e, e, NORMAL_FUNDO, COR_BORDA, 0);                     // normal
        botao(img, 0, 86 * e, e, HOVER_FUNDO, COR_SANGUE, COR_SANGUE_CLARO);      // hover / foco
        return new DynamicTexture(img);
    }

    private static DynamicTexture montarSlider(Minecraft mc) throws IOException {
        NativeImage img = ler(mc, SLIDER);
        if (img == null) {
            aviso("slider.png nao encontrado (os sliders ficam com o desenho original)");
            return null;
        }
        int w = img.getWidth();
        // o arquivo pode ser 200x80 ou 256x256 (os 4 quadros de 200x20 ficam no canto superior esquerdo); em pacote HD, 400/512...
        int base = w % 256 == 0 ? 256 : w % 200 == 0 ? 200 : 0;
        if (base == 0 || img.getHeight() < 80 * (w / base)) {
            aviso("slider.png com tamanho inesperado: " + w + "x" + img.getHeight() + " (esperado 200x80 ou 256x256)");
            img.close();
            return null;
        }
        aviso("slider.png ok: " + w + "x" + img.getHeight());
        int e = w / base;
        int h = 20 * e;
        botao(img, 0, 0, e, NORMAL_FUNDO, COR_BORDA, 0);                          // trilho
        botao(img, 0, h, e, HOVER_FUNDO, COR_SANGUE, COR_SANGUE_CLARO);           // trilho realcado
        botao(img, 0, 2 * h, e, 0xF0701020, COR_SANGUE, 0);                       // cursor (escuro: o texto passa por cima)
        botao(img, 0, 3 * h, e, COR_SANGUE, 0xFFFFD0D6, 0);                       // cursor realcado
        return new DynamicTexture(img);
    }

    private static final java.util.Set<String> avisados = new java.util.HashSet<>();

    /** Escreve no latest.log (uma vez por mensagem): ajuda a descobrir porque um desenho nao mudou. */
    private static void aviso(String msg) {
        if (avisados.add(msg)) {
            System.out.println("Ordem Mod (EstiloTexturas): " + msg);
        }
    }

    /**
     * Pinta uma faixa de 200x20 (vezes {@code e}): fundo de vidro, borda de {@code e} px e (se canto != 0) bracos de
     * 4*e px nos quatro cantos.
     */
    private static void botao(NativeImage img, int ox, int oy, int e, int fundo, int borda, int canto) {
        int largura = 200 * e;
        int altura = 20 * e;
        for (int y = 0; y < altura; y++) {
            for (int x = 0; x < largura; x++) {
                int c = fundo;
                boolean bordaX = x < e || x >= largura - e;
                boolean bordaY = y < e || y >= altura - e;
                if (bordaX || bordaY) {
                    c = borda;
                }
                if (canto != 0) {
                    boolean bracoH = (x < 4 * e || x >= largura - 4 * e) && bordaY;
                    boolean bracoV = (y < 4 * e || y >= altura - 4 * e) && bordaX;
                    if (bracoH || bracoV) {
                        c = canto;
                    }
                }
                img.setPixelRGBA(ox + x, oy + y, argbParaAbgr(c));
            }
        }
    }

    /** O NativeImage guarda o pixel como ABGR. */
    private static int argbParaAbgr(int argb) {
        return (argb & 0xFF00FF00) | ((argb >> 16) & 0xFF) | ((argb & 0xFF) << 16);
    }
}
