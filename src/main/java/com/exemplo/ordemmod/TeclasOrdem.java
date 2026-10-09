package com.exemplo.ordemmod;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** As teclas do mod. Só existe no cliente (no computador do jogador). */
public final class TeclasOrdem {
    public static final KeyMapping USAR = new KeyMapping(
            "key.ordemmod.usar_habilidade", InputConstants.KEY_R, "key.categories.ordemmod");
    public static final KeyMapping TROCAR = new KeyMapping(
            "key.ordemmod.trocar_habilidade", InputConstants.KEY_G, "key.categories.ordemmod");

    public static final KeyMapping MENU = new KeyMapping(
            "key.ordemmod.menu", InputConstants.KEY_K, "key.categories.ordemmod");

    private TeclasOrdem() {
    }

    /** Avisa o Minecraft que essas teclas existem (elas aparecem em Controles). */
    @Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static class Registro {
        @SubscribeEvent
        public static void registrar(RegisterKeyMappingsEvent e) {
            e.register(USAR);
            e.register(TROCAR);
            e.register(MENU);
        }
    }

    /** A cada tick do cliente, vê se a tecla foi apertada e manda a mensagem ao servidor. */
    @Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
    public static class Uso {
        @SubscribeEvent
        public static void aoTick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END || Minecraft.getInstance().player == null) {
                return;
            }
            while (USAR.consumeClick()) {
                Rede.CANAL.sendToServer(new PacoteHabilidade(PacoteHabilidade.USAR));
            }
            while (TROCAR.consumeClick()) {
                Rede.CANAL.sendToServer(new PacoteHabilidade(PacoteHabilidade.TROCAR));
            }
            while (MENU.consumeClick()) {
                if (Minecraft.getInstance().screen == null) {
                    Rede.CANAL.sendToServer(new PacoteHabilidade(PacoteHabilidade.ABRIR_FICHA));
                }
            }
        }
    }
}
