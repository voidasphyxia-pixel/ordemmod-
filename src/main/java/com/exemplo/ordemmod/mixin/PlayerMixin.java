package com.exemplo.ordemmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exemplo.ordemmod.EventosDano;
import com.exemplo.ordemmod.EventosPericias;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Atualidades (etapa 4d): mais XP. No Forge era o PlayerXpEvent.XpChange; o Fabric API nao tem esse evento,
 * entao o gancho e o inicio de Player.giveExperiencePoints (orbes de XP e /xp add passam por ele).
 *
 * Dano ao jogador: Player sobrescreve actuallyHurt SEM chamar o da LivingEntity, entao o gancho do
 * LivingEntityMixin nunca via o dano que o jogador recebe (queda, golpes de mob). Este e o mesmo gancho,
 * agora no Player. O LivingEntityMixin ignora jogadores para o dano nao ser calculado duas vezes.
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int ordemmod$ajustarXp(int pontos) {
        return EventosPericias.ajustarXp((Player) (Object) this, pontos);
    }

    /** Mesmo gancho do LivingEntityMixin.ordemmod$ajustarDano, so que para o jogador como vitima. */
    @ModifyVariable(method = "actuallyHurt", at = @At("HEAD"), argsOnly = true)
    private float ordemmod$ajustarDanoDoJogador(float quantidade, ServerLevel nivel, DamageSource fonte) {
        return EventosDano.aoSofrerDano((LivingEntity) (Object) this, fonte, quantidade);
    }
}
