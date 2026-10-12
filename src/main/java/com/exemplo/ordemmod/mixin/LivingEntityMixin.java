package com.exemplo.ordemmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.exemplo.ordemmod.EventosDano;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * Os dois ganchos que o Fabric API nao tem (no Forge eram LivingHurtEvent e MobEffectEvent.Applicable).
 * Toda a logica fica em EventosDano; aqui so se liga o jogo a ela.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    /**
     * Altera o dano logo no inicio do actuallyHurt. ATENCAO: aqui o dano ainda NAO passou pela armadura
     * (no Forge o LivingHurtEvent vinha depois da armadura). O dano de quem ataca e o de quem apanha
     * passam por aqui; veja EventosDano.aoSofrerDano.
     */
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float ordemmod$ajustarDano(float quantidade, ServerLevel nivel, DamageSource fonte) {
        return EventosDano.aoSofrerDano((LivingEntity) (Object) this, fonte, quantidade);
    }

    /** Se o jogador resistir a um efeito ruim, o efeito nem e aplicado. */
    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void ordemmod$resistirAoEfeito(MobEffectInstance efeito, CallbackInfoReturnable<Boolean> retorno) {
        if (EventosDano.resisteAoEfeito((LivingEntity) (Object) this, efeito)) {
            retorno.setReturnValue(false);
        }
    }
}
