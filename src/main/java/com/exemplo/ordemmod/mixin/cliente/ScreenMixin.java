package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.titulo.FundoMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/**
 * Fundo do mod nos menus do jogo (Um jogador, Multijogador, Opcoes...). Substitui o ScreenEvent.Render do Forge:
 * o fundo do jogo (Screen.extractBackground) e cancelado e trocado pelo {@link FundoMenu}, e as bordas dos campos de
 * texto sao desenhadas no fim de Screen.extractRenderState. Os nomes e descritores sao os do 26.2 (extract*), mas
 * nao foram conferidos no jogo; com require = 0, nome errado = menu com o visual original.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Inject(method = "extractBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"),
            cancellable = true, require = 0)
    private void ordemmod$fundo(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        Screen tela = (Screen) (Object) this;
        if (FundoMenu.estilizar(tela)) {
            FundoMenu.fundo(tela, g);
            ci.cancel();
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("TAIL"),
            require = 0)
    private void ordemmod$bordas(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        Screen tela = (Screen) (Object) this;
        if (FundoMenu.estilizar(tela)) {
            FundoMenu.bordas(tela, g);
        }
    }
}
