package com.exemplo.ordemmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/** Toca no jogador o passo de esquiva do Epic Fight, na direção que o afasta de quem atacou. Só no servidor. */
public final class EsquivaEpicFight {
    private EsquivaEpicFight() {
    }

    public static void esquivar(ServerPlayer jogador, DamageSource fonte) {
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(jogador, ServerPlayerPatch.class);
            if (patch == null) {
                return;
            }
            switch (passoContra(jogador, fonte)) {
                case TRAS -> patch.playAnimationSynchronized(Animations.BIPED_STEP_BACKWARD, 0.0F);
                case FRENTE -> patch.playAnimationSynchronized(Animations.BIPED_STEP_FORWARD, 0.0F);
                case ESQUERDA -> patch.playAnimationSynchronized(Animations.BIPED_STEP_LEFT, 0.0F);
                case DIREITA -> patch.playAnimationSynchronized(Animations.BIPED_STEP_RIGHT, 0.0F);
            }
        } catch (RuntimeException erro) { // a esquiva visual é estética: se falhar, o jogo segue
            System.err.println("[OrdemMod] não consegui tocar a esquiva do Epic Fight: " + erro);
        }
    }

    /** De onde veio o golpe (a flecha, se for flecha; senão quem bateu) em relação a pra onde o jogador olha. */
    private static EsquivaReflexos.Passo passoContra(ServerPlayer jogador, DamageSource fonte) {
        Vec3 origem = fonte.getSourcePosition();
        if (origem == null && fonte.getEntity() != null) {
            origem = fonte.getEntity().position();
        }
        if (origem == null) {
            return EsquivaReflexos.Passo.TRAS;
        }
        double dx = origem.x - jogador.getX();
        double dz = origem.z - jogador.getZ();
        double tamanho = Math.sqrt(dx * dx + dz * dz);
        if (tamanho < 1.0E-4) {
            return EsquivaReflexos.Passo.TRAS;
        }
        double giro = Math.toRadians(jogador.getYRot());
        // olhar = (-sen, cos); direita = (-cos, -sen) no plano horizontal
        double frente = (dx * -Math.sin(giro) + dz * Math.cos(giro)) / tamanho;
        double lado = (dx * -Math.cos(giro) + dz * -Math.sin(giro)) / tamanho;
        return EsquivaReflexos.escolherPasso(frente, lado);
    }
}
