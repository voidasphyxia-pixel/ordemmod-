package com.exemplo.ordemmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.exemplo.ordemmod.EventosPericias;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Furtividade e Iniciativa (etapa 4d). No Forge era o LivingChangeTargetEvent; o Fabric API nao tem evento de
 * mudanca de alvo, entao o gancho e o inicio de Mob.setTarget. Cancelar aqui = o monstro continua com o alvo antigo.
 */
@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void ordemmod$aoMirar(LivingEntity alvo, CallbackInfo retorno) {
        if (EventosPericias.aoMirar((Mob) (Object) this, alvo)) {
            retorno.cancel();
        }
    }
}
