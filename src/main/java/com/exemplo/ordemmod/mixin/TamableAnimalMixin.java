package com.exemplo.ordemmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.EventosPericias;

import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;

/**
 * Adestramento (etapa 4d): vida extra para bichos domados. No Forge era o AnimalTameEvent; o Fabric API nao tem
 * evento de domar, entao o gancho e o fim de TamableAnimal.tame. So cobre lobo, gato e papagaio (cavalos nao).
 */
@Mixin(TamableAnimal.class)
public abstract class TamableAnimalMixin {
    @Inject(method = "tame", at = @At("TAIL"))
    private void ordemmod$aoDomar(Player jogador, CallbackInfo retorno) {
        EventosPericias.aoDomar((TamableAnimal) (Object) this, jogador);
    }
}
