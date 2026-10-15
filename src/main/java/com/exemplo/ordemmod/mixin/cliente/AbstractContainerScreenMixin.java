package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;

/** Textos dos baus (o InventoryScreen tem o proprio gancho; aqui so vale ContainerScreen, para nao desenhar duas vezes). */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @Inject(method = "extractLabels", at = @At("TAIL"), require = 0)
    private void ordemmod$textos(GuiGraphicsExtractor g, int mx, int my, CallbackInfo ci) {
        AbstractContainerScreen<?> tela = (AbstractContainerScreen<?>) (Object) this;
        if (tela instanceof ContainerScreen) {
            InventarioOrdem.frente(tela, g);
        }
    }
}
