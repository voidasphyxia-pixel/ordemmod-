package com.exemplo.ordemmod.marcado;

import com.exemplo.ordemmod.DesenhoOP;
import com.exemplo.ordemmod.OrdemMod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * Só cliente: toca as sequências de quadros PNG feitas no Blender (textures/gui/marcado/vfx/<nome>/fNNNN.png).
 * Fabric 26.2: GuiGraphics -> GuiGraphicsExtractor, Identifier -> Identifier e a transparência vai como tinta no blit.
 */
public enum VfxMarcado {
    NEVOA("nevoa", 48, 512, 288, true),
    TINTA("tinta_sangue", 30, 512, 512, false),
    CORRENTES("correntes", 60, 720, 405, false),
    TITULO("titulo", 60, 720, 405, false),
    POEIRA("poeira", 72, 512, 288, true),
    SOMBRA("sombra", 24, 512, 512, false);

    public static final float FPS = 24f;

    public final String pasta;
    public final int quadros;
    public final int largura;
    public final int altura;
    public final boolean laco;
    private final Identifier[] texturas;

    VfxMarcado(String pasta, int quadros, int largura, int altura, boolean laco) {
        this.pasta = pasta;
        this.quadros = quadros;
        this.largura = largura;
        this.altura = altura;
        this.laco = laco;
        this.texturas = new Identifier[quadros];
        for (int i = 0; i < quadros; i++) {
            texturas[i] = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID,
                    String.format("textures/gui/marcado/vfx/%s/f%04d.png", pasta, i));
        }
    }

    /** Solta da memória (VRAM) os quadros já carregados (menos os das sequências em {@code manter}); recarregam sozinhos. */
    public static void liberarTodos(VfxMarcado... manter) {
        var tm = net.minecraft.client.Minecraft.getInstance().getTextureManager();
        java.util.List<VfxMarcado> ficam = java.util.Arrays.asList(manter);
        for (VfxMarcado v : values()) {
            if (ficam.contains(v)) {
                continue;
            }
            for (Identifier r : v.texturas) {
                tm.release(r);
            }
        }
    }

    public float duracao() {
        return quadros / FPS;
    }

    /** Desenha o quadro do instante {@code segundos}. Devolve false quando uma sequência sem laço já acabou. */
    public boolean desenhar(GuiGraphicsExtractor g, float segundos, float alpha, int x, int y, int w, int h) {
        int q = (int) (segundos * FPS);
        if (laco) {
            q = Math.floorMod(q, quadros);
        } else if (q >= quadros) {
            return false;
        }
        g.blit(RenderPipelines.GUI_TEXTURED, texturas[Math.max(0, q)], x, y, 0f, 0f, w, h, largura, altura, largura,
                altura, DesenhoOP.argb(alpha, 1f, 1f, 1f));
        return true;
    }
}
