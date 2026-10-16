package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

/** Baus e barris: o painel do mod por cima da textura do jogo. Os textos vem do AbstractContainerScreenMixin. */
@Mixin(ContainerScreen.class)
public abstract class ContainerScreenMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;FII)V", at = @At("TAIL"), require = 0)
    private void ordemmod$fundo(GuiGraphicsExtractor g, float parcial, int mx, int my, CallbackInfo ci) {
        InventarioOrdem.fundo((AbstractContainerScreen<?>) (Object) this, g, mx, my);
    }
}
