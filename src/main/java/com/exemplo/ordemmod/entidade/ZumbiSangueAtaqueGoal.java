package com.exemplo.ordemmod.entidade;

import java.util.EnumSet;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Perseguir + atacar do Zumbi de Sangue.
 *
 * Diferente do MeleeAttackGoal do jogo (que bate NA HORA), aqui o ataque tem tres fases:
 *   1) o zumbi chega perto e inicia o ataque (estado ATACANDO_x => cliente toca a animacao)
 *   2) durante a animacao ele fica parado olhando pro alvo (nenhum dano ainda)
 *   3) no tick de IMPACTO da animacao o dano e aplicado, mas SO se o alvo ainda estiver
 *      ao alcance; se o jogador se afastou, o golpe pega no ar.
 */
public class ZumbiSangueAtaqueGoal extends Goal {

    /** distancia (blocos) para comecar o ataque */
    private static final double ALCANCE_INICIO = 2.4D;
    /** distancia (blocos) maxima para o golpe realmente acertar no momento do impacto */
    private static final double ALCANCE_ACERTO = 3.0D;
    /** velocidade ao perseguir (multiplica o atributo MOVEMENT_SPEED); combina com a animacao de corrida */
    private static final double VELOCIDADE_CORRIDA = 1.35D;
    /** pausa entre um ataque e outro (ticks) */
    private static final int RECARGA = 5;

    private final ZumbiSangueEntity zumbi;
    private int ticksAtaque;
    private int impacto;
    private int fim;
    private boolean jaBateu;
    private int recarga;
    private int refazerCaminho;

    public ZumbiSangueAtaqueGoal(ZumbiSangueEntity zumbi) {
        this.zumbi = zumbi;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        LivingEntity alvo = this.zumbi.getTarget();
        return this.zumbi.podeAgir() && alvo != null && alvo.isAlive();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        this.refazerCaminho = 0;
        this.zumbi.setAggressive(true);   // o cliente usa isso para tocar a animacao de correr
    }

    @Override
    public void stop() {
        this.zumbi.setAggressive(false);
        this.zumbi.getNavigation().stop();
        if (this.zumbi.estaAtacando()) {
            this.zumbi.setEstado(ZumbiSangueEntity.NORMAL);
        }
        this.ticksAtaque = 0;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity alvo = this.zumbi.getTarget();
        if (alvo == null) {
            return;
        }
        if (this.recarga > 0) {
            this.recarga--;
        }

        if (this.zumbi.estaAtacando()) {
            tickAtaque(alvo);
            return;
        }

        // ---- perseguindo ----
        this.zumbi.getLookControl().setLookAt(alvo, 30.0F, 30.0F);
        if (--this.refazerCaminho <= 0) {
            this.refazerCaminho = 4 + this.zumbi.getRandom().nextInt(7);
            this.zumbi.getNavigation().moveTo(alvo, VELOCIDADE_CORRIDA);
        }
        double dist2 = this.zumbi.distanceToSqr(alvo);
        if (this.recarga <= 0 && dist2 <= ALCANCE_INICIO * ALCANCE_INICIO
                && this.zumbi.hasLineOfSight(alvo)) {
            iniciarAtaque(alvo);
        }
    }

    private void iniciarAtaque(LivingEntity alvo) {
        int golpe = this.zumbi.getRandom().nextInt(3);   // 0 = garra, 1 = arrasto, 2 = ataque
        int estado;
        if (golpe == 0) {
            estado = ZumbiSangueEntity.ATACANDO_1;
            this.impacto = ZumbiSangueEntity.ATAQUE1_IMPACTO;
            this.fim = ZumbiSangueEntity.ATAQUE1_FIM;
        } else if (golpe == 1) {
            estado = ZumbiSangueEntity.ATACANDO_2;
            this.impacto = ZumbiSangueEntity.ATAQUE2_IMPACTO;
            this.fim = ZumbiSangueEntity.ATAQUE2_FIM;
        } else {
            estado = ZumbiSangueEntity.ATACANDO_3;
            this.impacto = ZumbiSangueEntity.ATAQUE3_IMPACTO;
            this.fim = ZumbiSangueEntity.ATAQUE3_FIM;
        }
        this.ticksAtaque = 0;
        this.jaBateu = false;
        this.zumbi.getNavigation().stop();
        virarPara(alvo);
        this.zumbi.setEstado(estado);
    }

    private void tickAtaque(LivingEntity alvo) {
        this.ticksAtaque++;
        this.zumbi.getNavigation().stop();
        // acompanha o alvo ate o momento do golpe, depois trava a mira
        if (this.ticksAtaque < this.impacto) {
            virarPara(alvo);
        }

        if (!this.jaBateu && this.ticksAtaque >= this.impacto) {
            this.jaBateu = true;
            boolean alcanca = alvo.isAlive()
                    && this.zumbi.distanceToSqr(alvo) <= ALCANCE_ACERTO * ALCANCE_ACERTO
                    && this.zumbi.hasLineOfSight(alvo);
            if (alcanca) {
                this.zumbi.doHurtTarget(alvo);
            }
        }

        if (this.ticksAtaque >= this.fim) {
            this.zumbi.setEstado(ZumbiSangueEntity.NORMAL);
            this.recarga = RECARGA;
            this.ticksAtaque = 0;
        }
    }

    private void virarPara(LivingEntity alvo) {
        double dx = alvo.getX() - this.zumbi.getX();
        double dz = alvo.getZ() - this.zumbi.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
        this.zumbi.setYRot(yaw);
        this.zumbi.setYBodyRot(yaw);
        this.zumbi.setYHeadRot(yaw);
    }
}
