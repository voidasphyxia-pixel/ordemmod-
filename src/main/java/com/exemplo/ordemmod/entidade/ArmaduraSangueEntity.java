package com.exemplo.ordemmod.entidade;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.animation.object.PlayState;
import com.geckolib.util.GeckoLibUtil;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Armadura de Sangue - VERSAO DE TESTE (Fabric 26.2 + GeckoLib 5).
 *
 * E um "Steve" de cubos que nasce na sua frente, toca a animacao de formacao da armadura (animation.armadura_sangue.formar,
 * 3,7 s: inclina o tronco, levanta o braco, filamentos de sangue, placas, espinhos, gotas) e FICA parado na pose final
 * (thenPlayAndHold) com a armadura de sangue no corpo. Sem IA, sem dano, sem despawn.
 *
 * Comando: /ordem armadura (toca de novo, no lugar do anterior)  |  /ordem armadura limpar
 * Tambem da: /summon ordemmod:armadura_sangue
 *
 * Arquivos (gerados por tools/gerar_armadura_sangue.py a partir da animacao do Blender):
 *   assets/ordemmod/geckolib/models/entity/armadura_sangue.geo.json
 *   assets/ordemmod/geckolib/animations/entity/armadura_sangue.animation.json
 *   assets/ordemmod/textures/entity/armadura_sangue.png
 *
 * PROXIMO PASSO (fora deste teste): em vez de uma entidade separada, desenhar o mesmo modelo como camada do JOGADOR,
 * ligada ao estado "armadura ativa" salvo no personagem. Veja ARMADURA-DE-SANGUE.md.
 */
public class ArmaduraSangueEntity extends PathfinderMob implements GeoEntity {

    /** distancia (blocos) a frente do jogador onde ela aparece */
    public static final double DISTANCIA = 3.0D;

    private static final RawAnimation ANIM_FORMAR =
            RawAnimation.begin().thenPlayAndHold("animation.armadura_sangue.formar");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ArmaduraSangueEntity(EntityType<? extends ArmaduraSangueEntity> tipo, Level mundo) {
        super(tipo, mundo);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder criarAtributos() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0D);
    }

    /** Intocavel (so /kill e o void passam). */
    @Override
    public boolean hurtServer(ServerLevel nivel, DamageSource fonte, float quantidade) {
        if (!fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        return super.hurtServer(nivel, fonte, quantidade);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---- comando ---------------------------------------------------------------------------
    /** Remove as armaduras de teste perto do jogador. Devolve quantas removeu. */
    public static int limparPerto(ServerPlayer jogador) {
        int n = 0;
        for (ArmaduraSangueEntity e : jogador.level().getEntitiesOfClass(ArmaduraSangueEntity.class,
                jogador.getBoundingBox().inflate(64.0D))) {
            e.discard();
            n++;
        }
        return n;
    }

    /** Remove a(s) anterior(es) e cria uma nova na frente do jogador, virada para ele. Devolve 1 se criou. */
    public static int invocarPara(ServerPlayer jogador) {
        if (!(jogador.level() instanceof ServerLevel nivel)) {
            return 0;
        }
        limparPerto(jogador);
        float giro = jogador.getYRot();
        double rad = Math.toRadians(giro);
        ArmaduraSangueEntity e = new ArmaduraSangueEntity(EntidadesOrdem.ARMADURA_SANGUE, nivel);
        e.setPos(jogador.getX() - Math.sin(rad) * DISTANCIA, jogador.getY(), jogador.getZ() + Math.cos(rad) * DISTANCIA);
        float olhar = giro + 180.0F; // de frente para o jogador
        e.setYRot(olhar);
        e.setYBodyRot(olhar);
        e.setYHeadRot(olhar);
        nivel.addFreshEntity(e);
        return 1;
    }

    // ---- GeckoLib ---------------------------------------------------------------------------
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controladores) {
        controladores.add(new AnimationController<ArmaduraSangueEntity>("principal", 0, this::animar));
    }

    private PlayState animar(AnimationTest<ArmaduraSangueEntity> teste) {
        return teste.setAndContinue(ANIM_FORMAR);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
