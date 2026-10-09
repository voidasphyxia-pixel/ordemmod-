package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.ZumbiSangueEntity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ZumbiSangueRenderer extends GeoEntityRenderer<ZumbiSangueEntity> {

    public ZumbiSangueRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, new ZumbiSangueModel());
        this.shadowRadius = 0.5F;
    }

    /** O jogo tomba o corpo 90 graus ao morrer; a animacao "morte" ja faz isso, entao desligamos. */
    @Override
    protected float getDeathMaxRotation(ZumbiSangueEntity zumbi) {
        return 0.0F;
    }
}
