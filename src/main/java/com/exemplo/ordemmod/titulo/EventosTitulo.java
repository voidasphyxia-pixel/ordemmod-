package com.exemplo.ordemmod.titulo;

import com.exemplo.ordemmod.OrdemMod;

import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Troca o menu principal do jogo pela {@link TelaInicial} e cala a musica de menu do jogo enquanto o tema toca. */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
public final class EventosTitulo {
    /** false devolve o menu principal original do Minecraft. */
    public static final boolean SUBSTITUIR_MENU = true;

    private EventosTitulo() {
    }

    @SubscribeEvent
    public static void aoAbrirTela(ScreenEvent.Opening e) {
        if (SUBSTITUIR_MENU && e.getNewScreen() != null && e.getNewScreen().getClass() == TitleScreen.class) {
            e.setNewScreen(new TelaInicial());
        }
    }

    @SubscribeEvent
    public static void aoTocarSom(PlaySoundEvent e) {
        SoundInstance som = e.getSound();
        if (som == null || !TemaInicial.tocando()) {
            return;
        }
        if (som.getSource() == SoundSource.MUSIC && !OrdemMod.MOD_ID.equals(som.getLocation().getNamespace())) {
            e.setSound(null); // musica do menu do Minecraft (ou de outro mod): nao toca por cima do tema
        }
    }
}
