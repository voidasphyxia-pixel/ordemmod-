package com.exemplo.ordemmod.aparencia;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.Rede;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Lado do cliente: lembra a aparência de cada jogador que ele conhece e entrega a textura montada
 * para o mixin que troca a skin. A textura é montada só na primeira vez que é pedida (e de novo se
 * a aparência mudar).
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
public final class AparenciaCliente {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Map<UUID, AparenciaData> DADOS = new ConcurrentHashMap<>();
    private static final Map<UUID, Textura> TEXTURAS = new HashMap<>();

    /** {@code local} fica nulo se a montagem falhou (aí o jogo usa a skin normal e não tenta de novo). */
    private record Textura(AparenciaData origem, ResourceLocation local) {
    }

    private AparenciaCliente() {
    }

    /** Chamado quando o servidor avisa a aparência de um jogador (nulo = sem aparência). */
    public static void receber(UUID jogador, AparenciaData dados) {
        if (dados == null) {
            DADOS.remove(jogador);
            liberar(jogador);
        } else {
            DADOS.put(jogador, dados);
        }
    }

    public static AparenciaData de(UUID jogador) {
        return DADOS.get(jogador);
    }

    /**
     * Para a tela de aparência: mostra já no boneco e manda ao servidor para salvar.
     * Para só pré-visualizar, use {@link #receber}.
     */
    public static void enviarAoServidor(AparenciaData dados) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        receber(mc.player.getUUID(), dados);
        Rede.CANAL.sendToServer(new PacoteAparencia(mc.player.getUUID(), dados));
    }

    /** A skin montada do jogador, ou null para usar a skin normal. Só roda na thread de desenho. */
    public static ResourceLocation texturaDe(UUID jogador) {
        AparenciaData dados = DADOS.get(jogador);
        if (dados == null || !RenderSystem.isOnRenderThread()) {
            return null;
        }
        Textura atual = TEXTURAS.get(jogador);
        if (atual != null && atual.origem().equals(dados)) {
            return atual.local();
        }
        ResourceLocation local = null;
        try {
            NativeImage imagem = ComposicaoSkin.compor(dados);
            local = new ResourceLocation(OrdemMod.MOD_ID, "aparencia/" + jogador);
            Minecraft.getInstance().getTextureManager().register(local, new DynamicTexture(imagem));
        } catch (Exception erro) {
            LOG.warn("Não consegui montar a skin de aparência de {}", jogador, erro);
            local = null;
        }
        TEXTURAS.put(jogador, new Textura(dados, local));
        return local;
    }

    private static void liberar(UUID jogador) {
        Textura antiga = TEXTURAS.remove(jogador);
        if (antiga != null && antiga.local() != null) {
            Minecraft.getInstance().getTextureManager().release(antiga.local());
        }
    }

    /** Ao sair do mundo/servidor, esquece tudo. */
    @SubscribeEvent
    public static void aoSair(ClientPlayerNetworkEvent.LoggingOut evento) {
        for (UUID id : List.copyOf(TEXTURAS.keySet())) {
            liberar(id);
        }
        DADOS.clear();
    }
}
