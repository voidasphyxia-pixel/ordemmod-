package com.exemplo.ordemmod.marcado;

import com.exemplo.ordemmod.OrdemMod;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Só cliente: toca as sequências de quadros PNG feitas no Blender (textures/gui/marcado/vfx/<nome>/fNNNN.png). */
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
    private final ResourceLocation[] texturas;

    VfxMarcado(String pasta, int quadros, int largura, int altura, boolean laco) {
        this.pasta = pasta;
        this.quadros = quadros;
        this.largura = largura;
        this.altura = altura;
        this.laco = laco;
        this.texturas = new ResourceLocation[quadros];
        for (int i = 0; i < quadros; i++) {
            texturas[i] = new ResourceLocation(OrdemMod.MOD_ID,
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
            for (ResourceLocation r : v.texturas) {
                tm.release(r);
            }
        }
    }

    public float duracao() {
        return quadros / FPS;
    }

    /** Desenha o quadro do instante {@code segundos}. Devolve false quando uma sequência sem laço já acabou. */
    public boolean desenhar(GuiGraphics g, float segundos, float alpha, int x, int y, int w, int h) {
        int q = (int) (segundos * FPS);
        if (laco) {
            q = Math.floorMod(q, quadros);
        } else if (q >= quadros) {
            return false;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, alpha);
        g.blit(texturas[Math.max(0, q)], x, y, w, h, 0f, 0f, largura, altura, largura, altura);
        g.setColor(1f, 1f, 1f, 1f);
        return true;
    }
}
