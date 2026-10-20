package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.ArmaduraSangueJogadorEntity;
import com.exemplo.ordemmod.entidade.EntidadesOrdem;
import com.geckolib.constant.DataTickets;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.GeoRenderState;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer da Armadura de Sangue vestida no personagem.
 *  - posicao: usa a posicao SUAVIZADA do dono (mesma que o jogo usa para desenhar o jogador), sem atraso;
 *  - em primeira pessoa nao desenha (placas e espinhos ficariam voando na frente da camera);
 *  - escala 0,9375 como o modelo do jogador.
 * Arquivos: veja {@link ArmaduraSangueJogadorEntity}.
 */
public class ArmaduraSangueJogadorRenderer
        extends GeoEntityRenderer<ArmaduraSangueJogadorEntity, LivingEntityRenderState> {

    public ArmaduraSangueJogadorRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, EntidadesOrdem.ARMADURA_SANGUE_JOGADOR);
        this.shadowRadius = 0.0F;
        this.scaleWidth = 0.9375F;
        this.scaleHeight = 0.9375F;
    }

    @Override
    public void extractRenderState(ArmaduraSangueJogadorEntity entidade, LivingEntityRenderState estado,
                                   float parcial) {
        super.extractRenderState(entidade, estado, parcial);
        Entity dono = entidade.getDono();
        if (dono == null) {
            return;
        }
        Vec3 p = dono.getPosition(parcial);
        estado.x = p.x;
        estado.y = p.y;
        estado.z = p.z;
        Minecraft mc = Minecraft.getInstance();
        if (dono == mc.player && mc.options.getCameraType().isFirstPerson()) {
            estado.isInvisible = true;
            estado.addGeckolibData(DataTickets.INVISIBLE_TO_PLAYER, true);
        }
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState estadoDeRender) {
        return 0.0F;
    }

    @Override
    protected AABB getBoundingBoxForCulling(ArmaduraSangueJogadorEntity entidade) {
        return super.getBoundingBoxForCulling(entidade).inflate(2.0D);
    }
}
