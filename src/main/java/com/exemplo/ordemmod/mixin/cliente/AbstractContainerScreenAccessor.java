package com.exemplo.ordemmod.mixin.cliente;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

/** Le a posicao e o tamanho do painel (campos protegidos do jogo) para o {@link com.exemplo.ordemmod.InventarioOrdem}. */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int ordemmod$esquerda();

    @Accessor("topPos")
    int ordemmod$topo();

    @Accessor("imageWidth")
    int ordemmod$largura();

    @Accessor("imageHeight")
    int ordemmod$altura();
}
