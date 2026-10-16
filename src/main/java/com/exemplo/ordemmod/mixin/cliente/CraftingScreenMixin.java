package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Bancada de trabalho: o painel do mod por cima da textura do jogo. O alvo e dado por NOME (targets) para o build nao
 * quebrar se a classe mudar de lugar; com require = 0, nome errado = bancada com o visual do jogo.
 */
@Mixin(targets = "net.minecraft.client.gui.screens.inventory.CraftingScreen")
public abstract class CraftingScreenMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
    private void ordemmod$fundo(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        InventarioOrdem.fundo((AbstractContainerScreen<?>) (Object) this, g, mx, my);
    }
}
