package com.exemplo.ordemmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.Vec3;

/**
 * Esquiva e rolada SEM Epic Fight (etapa 7). No Forge o passo de esquiva e a rolada tocavam animacoes do Epic Fight
 * (EsquivaEpicFight e RolamentoEpicFight). Aqui nao ha animacao de corpo: o servidor da um impulso de verdade no
 * jogador (o passo para longe de quem atacou, ou a rolada para a frente), com fumaca e um som de "vento". A decisao
 * (esquivou ou nao, reduziu a queda ou nao) ja foi tomada pelo servidor e aparece no painel da rolagem.
 * Todos os numeros estao nas constantes abaixo, para ajustar vendo no jogo.
 */
public final class EsquivaVisual {
    /** Forca do passo lateral/para tras (blocos por tick; ~0,5 leva o jogador uns 2-3 blocos no chao). */
    public static final double FORCA_PASSO = 0.55;
    /** Forca da rolada para a frente (menor: ele ja esta caindo e o chao freia). */
    public static final double FORCA_ROLADA = 0.40;
    /** Um pulinho junto do passo, so quando esta no chao (tira o atrito do chao e fica mais fluido). */
    private static final double SALTINHO = 0.10;

    private EsquivaVisual() {
    }

    /** O jogador esquivou de um ataque: da um passo para longe de quem atacou (ou da flecha). */
    public static void esquivar(ServerPlayer jogador, DamageSource fonte) {
        EsquivaReflexos.Passo passo = passoContra(jogador, fonte);
        double giro = Math.toRadians(jogador.getYRot());
        // olhar = (-sen, cos); direita = (-cos, -sen) no plano horizontal
        double fx = -Math.sin(giro);
        double fz = Math.cos(giro);
        double rx = -Math.cos(giro);
        double rz = -Math.sin(giro);
        double dx;
        double dz;
        switch (passo) {
            case FRENTE -> { dx = fx; dz = fz; }
            case ESQUERDA -> { dx = -rx; dz = -rz; }
            case DIREITA -> { dx = rx; dz = rz; }
            default -> { dx = -fx; dz = -fz; } // TRAS
        }
        impulso(jogador, dx * FORCA_PASSO, jogador.onGround() ? SALTINHO : 0.0, dz * FORCA_PASSO);
        efeitos(jogador, 8, 1.4F);
    }

    /** O jogador reduziu o dano da queda e rola no chao, para a frente. */
    public static void rolar(ServerPlayer jogador) {
        double giro = Math.toRadians(jogador.getYRot());
        impulso(jogador, -Math.sin(giro) * FORCA_ROLADA, 0.0, Math.cos(giro) * FORCA_ROLADA);
        efeitos(jogador, 4, 1.0F);
    }

    /** Soma o impulso ao movimento atual e avisa o cliente (sem isso o jogador so se mexe no servidor e volta para tras). */
    private static void impulso(ServerPlayer jogador, double x, double y, double z) {
        Vec3 atual = jogador.getDeltaMovement();
        jogador.setDeltaMovement(atual.x + x, Math.max(atual.y, y), atual.z + z);
        jogador.hurtMarked = true; // manda o movimento ao cliente do proprio jogador
    }

    private static void efeitos(ServerPlayer jogador, int fumaca, float tom) {
        if (jogador.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.POOF, jogador.getX(), jogador.getY(0.2), jogador.getZ(),
                    fumaca, 0.3, 0.1, 0.3, 0.02);
            nivel.playSound(null, jogador.getX(), jogador.getY(), jogador.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP,
                    SoundSource.PLAYERS, 0.4F, tom);
        }
    }

    /** De onde veio o golpe (a flecha, se for flecha; senao quem bateu) em relacao a pra onde o jogador olha. */
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
        double frente = (dx * -Math.sin(giro) + dz * Math.cos(giro)) / tamanho;
        double lado = (dx * -Math.cos(giro) + dz * -Math.sin(giro)) / tamanho;
        return EsquivaReflexos.escolherPasso(frente, lado);
    }
}
