package com.exemplo.ordemmod.entidade;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Armadura de Sangue VESTIDA NO PERSONAGEM (Fabric 26.2 + GeckoLib 5).
 *
 * E a mesma armadura da versao de teste ({@link ArmaduraSangueEntity}), mas:
 *  - o modelo NAO tem o corpo de Steve (so placas, espinhos, filamentos e gotas): o corpo e o do proprio jogador;
 *  - ela pertence a um jogador (id guardado em dado sincronizado) e a cada tick copia posicao e giro dele,
 *    no servidor e no cliente, entao fica "colada" no personagem;
 *  - quem liga e desliga e o efeito da habilidade "Armadura de Sangue" (veja {@link #sincronizar}):
 *    efeito ativo -> a armadura existe e toca a animacao de formacao; efeito acabou -> ela e removida.
 *  - some sozinha se o dono morrer, sair, trocar de dimensao ou se o mundo for recarregado (nao e salva).
 *
 * Arquivos (gerados por tools/gerar_armadura_sangue.py): geckolib/models|animations/entity/armadura_sangue_jogador.*.json
 * e textures/entity/armadura_sangue_jogador.png.
 */
public class ArmaduraSangueJogadorEntity extends ArmaduraSangueEntity {

    private static final EntityDataAccessor<Integer> DONO =
            SynchedEntityData.defineId(ArmaduraSangueJogadorEntity.class, EntityDataSerializers.INT);

    /** So no cliente: jogador (id) -> sua armadura. Usado pela pose de formacao (braco levantado). */
    private static final java.util.Map<Integer, ArmaduraSangueJogadorEntity> NO_CLIENTE = new java.util.HashMap<>();

    /** Armadura do jogador de id donoId no cliente (null se nao houver / foi removida). */
    public static ArmaduraSangueJogadorEntity doDono(int donoId) {
        ArmaduraSangueJogadorEntity e = NO_CLIENTE.get(donoId);
        if (e != null && e.isRemoved()) {
            NO_CLIENTE.remove(donoId);
            return null;
        }
        return e;
    }

    public ArmaduraSangueJogadorEntity(EntityType<? extends ArmaduraSangueEntity> tipo, Level mundo) {
        super(tipo, mundo);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder construtor) {
        super.defineSynchedData(construtor);
        construtor.define(DONO, -1);
    }

    public int getDonoId() {
        return this.entityData.get(DONO);
    }

    /** O jogador dono (null se ainda nao carregou no cliente, ou se esta em outra dimensao). */
    public Entity getDono() {
        int id = getDonoId();
        return id < 0 ? null : this.level().getEntity(id);
    }

    /** Nao e alvo de mira nem de ataque: o jogador acerta o que esta atras dela. */
    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        Entity dono = getDono();
        if (dono == null || !dono.isAlive()) {
            // No servidor: dono sumiu/morreu/outra dimensao/mundo recarregado -> remove.
            // No cliente: o dono pode ainda nao ter carregado; espera o servidor remover.
            if (!this.level().isClientSide() && !this.isRemoved()) {
                this.discard();
            }
            return;
        }
        if (this.level().isClientSide()) {
            NO_CLIENTE.put(dono.getId(), this);
        }
        this.snapTo(dono.getX(), dono.getY(), dono.getZ(), dono.getYRot(), 0.0F);
        if (dono instanceof LivingEntity vivo) {
            this.yBodyRot = vivo.yBodyRot;
            this.yBodyRotO = vivo.yBodyRotO;
            this.yHeadRot = vivo.yBodyRot;
            this.yHeadRotO = vivo.yBodyRotO;
        }
    }

    // ---- ligar / desligar pelo efeito da habilidade -----------------------------------------
    private static ArmaduraSangueJogadorEntity procurar(ServerPlayer jogador) {
        for (ArmaduraSangueJogadorEntity e : jogador.level().getEntitiesOfClass(ArmaduraSangueJogadorEntity.class,
                jogador.getBoundingBox().inflate(8.0D))) {
            if (e.getDonoId() == jogador.getId()) {
                return e;
            }
        }
        return null;
    }

    /**
     * Chamado todo tick pelo EventosOrdem: garante que a armadura existe se (e so se) o efeito esta ativo.
     * So procura a entidade quando precisa (efeito ativo, ou 1 vez por segundo para limpar sobras).
     */
    public static void sincronizar(ServerPlayer jogador, boolean ativa) {
        if (!ativa && jogador.tickCount % 20 != 0) {
            return;
        }
        ArmaduraSangueJogadorEntity atual = procurar(jogador);
        if (ativa && jogador.isAlive()) {
            if (atual == null && jogador.level() instanceof ServerLevel nivel) {
                ArmaduraSangueJogadorEntity nova =
                        new ArmaduraSangueJogadorEntity(EntidadesOrdem.ARMADURA_SANGUE_JOGADOR, nivel);
                nova.entityData.set(DONO, jogador.getId());
                nova.snapTo(jogador.getX(), jogador.getY(), jogador.getZ(), jogador.getYRot(), 0.0F);
                nivel.addFreshEntity(nova);
            }
        } else if (atual != null) {
            atual.discard();
        }
    }
}
