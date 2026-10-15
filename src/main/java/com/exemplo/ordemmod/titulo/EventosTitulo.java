package com.exemplo.ordemmod.titulo;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

/**
 * Troca o menu principal do jogo pela {@link TelaInicial}.
 *
 * Fabric 26.2: no Forge o ScreenEvent.Opening trocava a tela antes de abrir. Aqui o ScreenEvents.AFTER_INIT avisa que
 * o TitleScreen acabou de iniciar e a troca e feita no proximo instante do cliente (nao se troca de tela dentro do
 * init da propria tela). A musica de menu do jogo e calada pelo proprio {@link TemaInicial} (nao ha PlaySoundEvent).
 */
public final class EventosTitulo {
    /** false devolve o menu principal original do Minecraft. */
    public static final boolean SUBSTITUIR_MENU = true;

    private EventosTitulo() {
    }

    /** Chamar do entrypoint do cliente. */
    public static void registrar() {
        ScreenEvents.AFTER_INIT.register((cliente, tela, largura, altura) -> {
            if (SUBSTITUIR_MENU && tela.getClass() == TitleScreen.class) {
                cliente.execute(() -> {
                    if (cliente.gui.screen() == tela) {
                        cliente.gui.setScreen(new TelaInicial());
                    }
                });
            }
        });
    }
}
