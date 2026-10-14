package com.exemplo.ordemmod.entidade;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.LoopType;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Zumbi de Sangue (Fabric 26.2 + GeckoLib 5).
 *
 * Ciclo: nasce DEITADO -> (jogador chega perto) LEVANTANDO -> NORMAL (anda/idle) <-> ATACANDO_1/2/3.
 *
 * O estado fica sincronizado servidor -> cliente (ESTADO) e o cliente apenas escolhe a animacao
 * correspondente. O DANO e decidido so no servidor, num tick calculado a partir do momento de
 * impacto da animacao (ver constantes de tempo abaixo), entao nunca vem antes do golpe.
 *
 * Mudancas em relacao ao Forge 1.20.1: hurt -> hurtServer(ServerLevel, ...), defineSynchedData com Builder,
 * NBT com ValueInput/ValueOutput, getDimensions -> getDefaultDimensions (o primeiro e final no LivingEntity),
 * GeckoLib 5 (pacotes com.geckolib.*, controladores sem o parametro da entidade,
 * velocidade da animacao definida dentro do predicado) e o culling da hitbox foi para o renderer.
 */
public class ZumbiSangueEntity extends Monster implements GeoEntity {

    // ---- estados -------------------------------------------------------------------------
    public static final int DEITADO = 0;
    public static final int LEVANTANDO = 1;
    public static final int NORMAL = 2;
    public static final int ATACANDO_1 = 3;
    public static final int ATACANDO_2 = 4;
    public static final int ATACANDO_3 = 5;

    // ---- tempos (em ticks; 20 ticks = 1 segundo) -------------------------------------------
    // O GeckoLib so comeca a contar a animacao DEPOIS da transicao entre animacoes, e o cliente
    // recebe o novo estado ~1 tick depois do servidor. Esse atraso e somado a todos os tempos.
    public static final int TRANSICAO_ANIM = 4;
    public static final int ATRASO_VISUAL = TRANSICAO_ANIM + 1;

    /** animation.zumbi.levantar tem 5.4s */
    public static final int LEVANTAR_FIM = 108 + ATRASO_VISUAL;
    /** a partir de 3.6s (tick 72) o corpo ja esta de pe: hitbox volta ao normal */
    public static final int LEVANTAR_EM_PE = 72 + ATRASO_VISUAL;

    /**
     * Quanto mais rapido o zumbi bate: 1.0 = velocidade original da animacao, 1.5 = 50% mais rapido.
     * Os tempos de impacto/fim abaixo ja sao divididos por esse valor (ticks "base" = animacao a 1.0x).
     * A transicao entre animacoes (ATRASO_VISUAL) continua contando inteira, entao o dano nunca vem antes do golpe.
     */
    public static final double VELOCIDADE_ATAQUE = 1.5D;

    private static int ataque(int ticksBase) {
        return ATRASO_VISUAL + Math.round((float) (ticksBase / VELOCIDADE_ATAQUE));
    }

    /** animation.zumbi.ataque_garra: 1.6s (32 ticks) a 1.0x, as garras acertam em ~0.45s (tick 9) */
    public static final int ATAQUE1_IMPACTO = ataque(9);
    public static final int ATAQUE1_FIM = ataque(32);
    /** animation.zumbi.ataque_arrasto: 1.7s (34 ticks) a 1.0x, o golpe acerta em ~0.6s (tick 12) */
    public static final int ATAQUE2_IMPACTO = ataque(12);
    public static final int ATAQUE2_FIM = ataque(34);
    /** animation.zumbi.ataque: 1.6s (32 ticks) a 1.0x, o golpe acerta em ~0.75s (tick 15) */
    public static final int ATAQUE3_IMPACTO = ataque(15);
    public static final int ATAQUE3_FIM = ataque(32);

    /** animation.zumbi.morte: 2.2s. O corpo some so depois da animacao terminar */
    public static final int MORTE_FIM = 44 + ATRASO_VISUAL;

    /** distancia (blocos) a partir da qual o jogador faz o zumbi se levantar. 0 = levanta logo ao nascer */
    public static final double RAIO_GATILHO = 8.0D;

    // ---- animacoes (nomes iguais aos do zumbi_sangue.animation.json) --------------------------
    private static final RawAnimation ANIM_DEITADO = RawAnimation.begin().thenLoop("animation.zumbi.deitado");
    private static final RawAnimation ANIM_LEVANTAR = RawAnimation.begin().thenPlayAndHold("animation.zumbi.levantar");
    private static final RawAnimation ANIM_IDLE = RawAnimation.begin().thenLoop("animation.zumbi.idle");
    private static final RawAnimation ANIM_ANDAR = RawAnimation.begin().thenLoop("animation.zumbi.andar");
    private static final RawAnimation ANIM_CORRER = RawAnimation.begin().thenLoop("animation.zumbi.correr");
    private static final RawAnimation ANIM_ATAQUE_1 = RawAnimation.begin().thenPlayAndHold("animation.zumbi.ataque_garra");
    private static final RawAnimation ANIM_ATAQUE_2 = RawAnimation.begin().thenPlayAndHold("animation.zumbi.ataque_arrasto");
    private static final RawAnimation ANIM_ATAQUE_3 = RawAnimation.begin().thenPlayAndHold("animation.zumbi.ataque");
    private static final RawAnimation ANIM_MORTE = RawAnimation.begin().thenPlayAndHold("animation.zumbi.morte");
    private static final RawAnimation ANIM_DANO = RawAnimation.begin().then("animation.zumbi.dano", LoopType.PLAY_ONCE);

    private static final EntityDimensions DIMENSOES_DEITADO = EntityDimensions.fixed(1.0F, 0.5F);

    private static final EntityDataAccessor<Integer> ESTADO =
            SynchedEntityData.defineId(ZumbiSangueEntity.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** copia local do estado (evita ler o entityData dentro de getDefaultDimensions durante a construcao) */
    private int estadoLocal = DEITADO;
    /** ticks desde que o estado atual comecou (contado no cliente e no servidor) */
    private int tempoEstado = 0;

    public ZumbiSangueEntity(EntityType<? extends ZumbiSangueEntity> tipo, Level mundo) {
        super(tipo, mundo);
        this.xpReward = 10;
    }

    public static AttributeSupplier.Builder criarAtributos() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 60.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.ATTACK_DAMAGE, 6.0D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.3D);
    }

    // ---- IA ------------------------------------------------------------------------------
    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new ZumbiSangueAtaqueGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        // mustSee = false: ele caca o jogador mesmo sem linha de visao (dentro do FOLLOW_RANGE)
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    // ---- estado --------------------------------------------------------------------------
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        super.defineSynchedData(construtor);
        construtor.define(ESTADO, DEITADO);
    }

    public int getEstado() {
        return this.estadoLocal;
    }

    /** Troca de estado (so no servidor). Reinicia o contador de tempo do estado. */
    public void setEstado(int novo) {
        if (this.estadoLocal == novo) {
            return;
        }
        this.estadoLocal = novo;
        this.tempoEstado = 0;
        this.entityData.set(ESTADO, novo);
        this.refreshDimensions();
        if (novo == LEVANTANDO) {
            this.playSound(SoundEvents.ZOMBIE_AMBIENT, 1.0F, 0.6F);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> chave) {
        super.onSyncedDataUpdated(chave);
        if (ESTADO.equals(chave)) {
            int novo = this.entityData.get(ESTADO);
            if (novo != this.estadoLocal) {
                this.estadoLocal = novo;
                this.tempoEstado = 0;
            }
            this.refreshDimensions();
        }
    }

    public int getTempoEstado() {
        return this.tempoEstado;
    }

    /** true quando o zumbi esta no chao / se levantando (sem IA, intocavel) */
    public boolean estaEmergindo() {
        return this.estadoLocal == DEITADO || this.estadoLocal == LEVANTANDO;
    }

    /** true quando pode andar e atacar */
    public boolean podeAgir() {
        return !estaEmergindo();
    }

    public boolean estaAtacando() {
        return this.estadoLocal == ATACANDO_1 || this.estadoLocal == ATACANDO_2 || this.estadoLocal == ATACANDO_3;
    }

    @Override
    public void tick() {
        super.tick();
        this.tempoEstado++;

        if (this.estadoLocal == LEVANTANDO && this.tempoEstado == LEVANTAR_EM_PE) {
            this.refreshDimensions(); // corpo ja esta de pe: hitbox normal
        }
        if (this.level().isClientSide()) {
            return;
        }
        if (this.estadoLocal == DEITADO) {
            if (jogadorPerto()) {
                setEstado(LEVANTANDO);
            }
        } else if (this.estadoLocal == LEVANTANDO) {
            if (this.tempoEstado >= LEVANTAR_FIM) {
                setEstado(NORMAL);
            }
        }
    }

    private boolean jogadorPerto() {
        if (RAIO_GATILHO <= 0.0D) {
            return true;
        }
        Player p = this.level().getNearestPlayer(this, RAIO_GATILHO);
        return p != null && !p.isSpectator() && !p.isCreative();
    }

    // ---- fisica / dano enquanto no chao ---------------------------------------------------
    /** Sem IA e sem andar enquanto deitado ou levantando (a gravidade continua valendo). */
    @Override
    protected boolean isImmobile() {
        return super.isImmobile() || estaEmergindo();
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && !estaEmergindo();
    }

    /** No 26.2 o dano so chega aqui no servidor (hurtServer), entao nao ha mais teste de lado do cliente. */
    @Override
    public boolean hurtServer(ServerLevel nivel, DamageSource fonte, float quantidade) {
        // o corpo deitado nao bate com a hitbox, entao fica intocavel ate levantar
        // (mas /kill e o void continuam funcionando)
        if (estaEmergindo() && !fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        boolean levou = super.hurtServer(nivel, fonte, quantidade);
        // toca a animacao de dano (so em pe e sem atacar, para nao cortar o golpe no meio)
        if (levou && this.isAlive() && this.estadoLocal == NORMAL) {
            this.triggerAnim("dano", "dano");
        }
        return levou;
    }

    /**
     * Morte: o jogo normalmente remove o corpo 20 ticks depois de morrer. Como a animacao
     * de morte tem 2.2s, seguramos o corpo ate ela terminar.
     */
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (this.deathTime >= MORTE_FIM && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    /**
     * No 26.2 o LivingEntity.getDimensions(Pose) e final: ele chama este metodo e depois aplica a escala.
     * Deitado (ou ainda levantando, antes do corpo ficar de pe) usa a hitbox baixa.
     */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        boolean deitado = this.estadoLocal == DEITADO
                || (this.estadoLocal == LEVANTANDO && this.tempoEstado < LEVANTAR_EM_PE);
        return deitado ? DIMENSOES_DEITADO : super.getDefaultDimensions(pose);
    }

    // O culling (modelo deitado se estende ~2 blocos alem da hitbox) agora e feito no ZumbiSangueRenderer.

    // ---- save / load ------------------------------------------------------------------------
    @Override
    public void addAdditionalSaveData(ValueOutput saida) {
        super.addAdditionalSaveData(saida);
        saida.putInt("EstadoZumbi", this.estadoLocal);
    }

    @Override
    public void readAdditionalSaveData(ValueInput entrada) {
        super.readAdditionalSaveData(entrada);
        int e = entrada.getIntOr("EstadoZumbi", -1);
        if (e >= 0) {
            if (e == LEVANTANDO) {
                e = DEITADO;   // recomeca a levantar
            } else if (e == ATACANDO_1 || e == ATACANDO_2 || e == ATACANDO_3) {
                e = NORMAL;
            }
            setEstado(e);
        }
    }

    // ---- sons ---------------------------------------------------------------------------------
    @Override
    protected SoundEvent getAmbientSound() {
        return estaEmergindo() ? null : SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource fonte) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState estado) {
        this.playSound(SoundEvents.ZOMBIE_STEP, 0.15F, 1.0F);
    }

    // ---- GeckoLib -----------------------------------------------------------------------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
        // A velocidade dos golpes (VELOCIDADE_ATAQUE) e definida dentro de animar(); andar/correr/levantar/morte
        // seguem na velocidade normal.
        controladores.add(new AnimationController<ZumbiSangueEntity>("principal", TRANSICAO_ANIM, this::animar));
        // controlador separado, so para o "tranco" ao levar dano (disparado por triggerAnim em hurtServer())
        controladores.add(new AnimationController<ZumbiSangueEntity>("dano", 0, teste -> PlayState.STOP)
                .triggerableAnim("dano", ANIM_DANO));
    }

    private PlayState animar(AnimationTest<ZumbiSangueEntity> teste) {
        teste.setControllerSpeed(this.estaAtacando() ? (float) VELOCIDADE_ATAQUE : 1.0F);
        if (this.isDeadOrDying()) {
            return teste.setAndContinue(ANIM_MORTE);
        }
        switch (this.estadoLocal) {
            case DEITADO:
                return teste.setAndContinue(ANIM_DEITADO);
            case LEVANTANDO:
                return teste.setAndContinue(ANIM_LEVANTAR);
            case ATACANDO_1:
                return teste.setAndContinue(ANIM_ATAQUE_1);
            case ATACANDO_2:
                return teste.setAndContinue(ANIM_ATAQUE_2);
            case ATACANDO_3:
                return teste.setAndContinue(ANIM_ATAQUE_3);
            default:
                if (teste.isMoving()) {
                    // isAggressive() e sincronizado servidor -> cliente (getTarget() nao e)
                    return teste.setAndContinue(this.isAggressive() ? ANIM_CORRER : ANIM_ANDAR);
                }
                return teste.setAndContinue(ANIM_IDLE);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
