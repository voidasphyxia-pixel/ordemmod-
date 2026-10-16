package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

/**
 * Baus e barris: o painel do mod por cima da textura do jogo. Os textos vem do AbstractContainerScreenMixin.
 *
 * CORRECAO (16/10): o descritor estava com a ordem dos parametros errada (g, float, int, int, a do antigo renderBg).
 * No 26.x o metodo e extractBackground(GuiGraphicsExtractor, int mouseX, int mouseY, float delta), como mostra a doc do
 * Fabric (Container Menus 26.2). Com o descritor errado o mixin nao achava o metodo e o bau ficava com o visual do jogo.
 */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
    private void ordemmod$fundo(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        InventarioOrdem.fundo((AbstractContainerScreen<?>) (Object) this, g, mx, my);
    }
}
