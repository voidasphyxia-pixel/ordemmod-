package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.ArmaduraSangueEntity;
import com.exemplo.ordemmod.entidade.EntidadesOrdem;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.phys.AABB;

/**
 * Renderer da Armadura de Sangue (teste). Mesmo padrao do ZumbiSangueRenderer: o modelo e criado sozinho a partir do id do
 * tipo da entidade (ordemmod:armadura_sangue).
 *
 * Arquivos usados (em src/main/resources/assets/ordemmod/):
 *   geckolib/models/entity/armadura_sangue.geo.json
 *   geckolib/animations/entity/armadura_sangue.animation.json
 *   textures/entity/armadura_sangue.png
 */
public class ArmaduraSangueRenderer extends GeoEntityRenderer<ArmaduraSangueEntity, LivingEntityRenderState> {

    public ArmaduraSangueRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, EntidadesOrdem.ARMADURA_SANGUE);
        this.shadowRadius = 0.4F;
    }

    /** Sem tombar o corpo ao morrer (/kill). */
    @Override
    protected float getDeathMaxRotation(GeoRenderState estadoDeRender) {
        return 0.0F;
    }

    /** Braco levantado, espinhos e gotas passam da hitbox: evita sumir da tela por culling. */
    @Override
    protected AABB getBoundingBoxForCulling(ArmaduraSangueEntity entidade) {
        return super.getBoundingBoxForCulling(entidade).inflate(2.0D);
    }
}
