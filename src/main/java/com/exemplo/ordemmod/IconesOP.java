package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Os ícones em pixel art (16x16) dos poderes. Cada poder usa o arquivo
 * assets/ordemmod/textures/gui/poderes/&lt;id do poder&gt;.png; se o arquivo não existir, aparece o generico.png.
 * Os PNGs são gerados por tools/gerar_icones.py (dá para trocar por desenhos seus, é só manter o nome).
 * Só existe no cliente.
 */
@OnlyIn(Dist.CLIENT)
public final class IconesOP {
    private static final ResourceLocation GENERICO = caminho("generico");
    private static final Map<String, ResourceLocation> CACHE = new HashMap<>();

    private IconesOP() {
    }

    private static ResourceLocation caminho(String id) {
        return new ResourceLocation(OrdemMod.MOD_ID, "textures/gui/poderes/" + id + ".png");
    }

    /** A textura do poder (ou a genérica, se ele ainda não tem ícone). */
    public static ResourceLocation textura(String id) {
        ResourceLocation achada = CACHE.get(id);
        if (achada == null) {
            ResourceLocation candidata = caminho(id);
            boolean existe = Minecraft.getInstance().getResourceManager().getResource(candidata).isPresent();
            achada = existe ? candidata : GENERICO;
            CACHE.put(id, achada);
        }
        return achada;
    }

    /** Desenha o ícone com {@code tamanho} pixels de lado; {@code brilho} 1 = normal, menor = escurecido. */
    public static void desenhar(GuiGraphics g, String id, int x, int y, int tamanho, float brilho) {
        g.flush(); // termina o que já foi pedido antes, para a ordem de desenho ficar certa
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(brilho, brilho, brilho, 1F);
        g.blit(textura(id), x, y, tamanho, tamanho, 0F, 0F, 16, 16, 16, 16);
        g.setColor(1F, 1F, 1F, 1F);
    }
}
