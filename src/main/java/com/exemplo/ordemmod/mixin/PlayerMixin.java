package com.exemplo.ordemmod.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import com.exemplo.ordemmod.EventosPericias;

import net.minecraft.world.entity.player.Player;

/**
 * Atualidades (etapa 4d): mais XP. No Forge era o PlayerXpEvent.XpChange; o Fabric API nao tem esse evento,
 * entao o gancho e o inicio de Player.giveExperiencePoints (orbes de XP e /xp add passam por ele).
 */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int ordemmod$ajustarXp(int pontos) {
        return EventosPericias.ajustarXp((Player) (Object) this, pontos);
    }
}
