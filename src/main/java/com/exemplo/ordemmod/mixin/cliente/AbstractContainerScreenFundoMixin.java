package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Gancho generico para o fundo de vidro escuro (inventario, baus e bancada). Em vez de depender do nome/descritor
 * exato do metodo que desenha a textura do jogo (que mudou no 26.x e os mixins por classe nao estavam pegando),
 * entra em AbstractContainerScreen.extractRenderState logo DEPOIS da chamada que desenha o fundo, antes das casas
 * de item. Os dois nomes possiveis (extractBackground e extractBg) estao listados; o que nao existir e ignorado
 * (require = 0). Se o jogo desenhar duas vezes, o resultado e o mesmo.
 *
 * Fica numa classe separada para que uma falha aqui nao derrube o gancho dos textos (AbstractContainerScreenMixin).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenFundoMixin {
    @Inject(method = "extractRenderState", at = {
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBackground", shift = At.Shift.AFTER),
            @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;extractBg", shift = At.Shift.AFTER)
    }, require = 0)
    private void ordemmod$fundoGenerico(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        InventarioOrdem.fundo((AbstractContainerScreen<?>) (Object) this, g, mx, my);
    }
}
