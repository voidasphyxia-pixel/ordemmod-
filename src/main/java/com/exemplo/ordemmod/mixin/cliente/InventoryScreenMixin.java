package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;

/**
 * Inventario do jogador: o painel do mod por cima da textura do jogo (extractBackground) e os textos (extractLabels).
 *
 * CORRECAO (16/10): o descritor do extractBackground estava com a ordem errada (g, float, int, int). O certo no 26.x e
 * (GuiGraphicsExtractor, int, int, float), igual ao Screen.extractBackground que o ScreenMixin ja usa.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"), require = 0)
    private void ordemmod$fundo(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        InventarioOrdem.fundo((AbstractContainerScreen<?>) (Object) this, g, mx, my);
    }

    @Inject(method = "extractLabels", at = @At("TAIL"), require = 0)
    private void ordemmod$textos(GuiGraphicsExtractor g, int mx, int my, CallbackInfo ci) {
        InventarioOrdem.frente((AbstractContainerScreen<?>) (Object) this, g);
    }
}
