package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.InventarioOrdem;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/**
 * Gancho de reserva para o fundo de vidro escuro (inventario, baus e bancada).
 *
 * O fundo normal vem dos mixins por classe (InventoryScreenMixin, ContainerScreenMixin, CraftingScreenMixin), que entram no
 * fim do extractBackground de cada tela. Este aqui so age se NENHUM deles desenhou o fundo neste quadro (por exemplo, se
 * alguma tela herdar o extractBackground em vez de declarar o proprio): entra em AbstractContainerScreen.extractRenderState
 * logo DEPOIS de super.extractRenderState (que desenha o fundo e os widgets) e antes das casas de item, e desenha o painel.
 *
 * CORRECAO (16/10): a versao anterior procurava uma chamada a extractBackground dentro do extractRenderState da
 * AbstractContainerScreen, mas essa chamada acontece dentro do Screen.extractRenderState (super), entao nunca achava nada.
 * Com require = 0, nome/descritor errado = sem efeito (so o visual do jogo), sem derrubar nada.
 *
 * Fica numa classe separada para que uma falha aqui nao derrube o gancho dos textos (AbstractContainerScreenMixin).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenFundoMixin {
    /** Comeco do quadro: zera a marca de "o fundo ja foi desenhado". */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V", at = @At("HEAD"), require = 0)
    private void ordemmod$novoQuadro(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        InventarioOrdem.novoQuadro((AbstractContainerScreen<?>) (Object) this);
    }

    /** Depois do super (fundo + widgets): se ninguem desenhou o painel do mod, desenha agora. */
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
                    shift = At.Shift.AFTER),
            require = 0)
    private void ordemmod$fundoSeFaltou(GuiGraphicsExtractor g, int mx, int my, float parcial, CallbackInfo ci) {
        InventarioOrdem.fundoSeFaltou((AbstractContainerScreen<?>) (Object) this, g, mx, my);
    }
}
