package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.EntidadesOrdem;
import com.exemplo.ordemmod.entidade.ZumbiSangueEntity;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * Renderer do Zumbi de Sangue (GeckoLib 5). Faz o trabalho do antigo ZumbiSangueModel + ZumbiSangueRenderer:
 * o modelo e criado sozinho a partir do id do tipo da entidade (ordemmod:zumbi_sangue), e a cabeca acompanha o olhar.
 *
 * Arquivos usados (em src/main/resources/assets/ordemmod/):
 *   geckolib/models/entity/zumbi_sangue.geo.json
 *   geckolib/animations/entity/zumbi_sangue.animation.json
 *   textures/entity/zumbi_sangue.png
 *
 * No GeckoLib 5 o renderer nao enxerga a entidade na hora de desenhar: tudo que ele precisa (aqui, "pode agir")
 * e copiado para o RenderState em addRenderData.
 */
public class ZumbiSangueRenderer extends GeoEntityRenderer<ZumbiSangueEntity, LivingEntityRenderState> {

    private static final DataTicket<Boolean> PODE_AGIR =
            DataTicket.create("ordemmod:zumbi_pode_agir", Boolean.class);

    public ZumbiSangueRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, EntidadesOrdem.ZUMBI_SANGUE);
        this.shadowRadius = 0.5F;
    }

    /** O jogo tomba o corpo 90 graus ao morrer; a animacao "morte" ja faz isso, entao desligamos. */
    @Override
    protected float getDeathMaxRotation(GeoRenderState estadoDeRender) {
        return 0.0F;
    }

    /** O modelo deitado se estende ~2 blocos alem da hitbox: evita sumir da tela por culling. */
    @Override
    protected AABB getBoundingBoxForCulling(ZumbiSangueEntity zumbi) {
        return super.getBoundingBoxForCulling(zumbi).inflate(2.5D);
    }

    @Override
    public void addRenderData(ZumbiSangueEntity zumbi, Void relacionado, LivingEntityRenderState estado, float parcial) {
        super.addRenderData(zumbi, relacionado, estado, parcial);
        estado.addGeckolibData(PODE_AGIR, zumbi.podeAgir());
    }

    /** A cabeca acompanha o olhar, mas so em pe (deitado/levantando a animacao manda). */
    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> passada, BoneSnapshots ossos) {
        super.adjustModelBonesForRender(passada, ossos);

        if (!passada.getOrDefaultGeckolibData(PODE_AGIR, false)
                || passada.getOrDefaultGeckolibData(DataTickets.IS_DEAD_OR_DYING, false)) {
            return;
        }
        float inclinacao = passada.getOrDefaultGeckolibData(DataTickets.ENTITY_PITCH, 0.0F) * Mth.DEG_TO_RAD;
        float giro = passada.getOrDefaultGeckolibData(DataTickets.ENTITY_YAW, 0.0F) * Mth.DEG_TO_RAD;
        // Sinal: igual ao de outros mods 26.x com GeckoLib 5 (subtrai). Se a cabeca olhar ao contrario, troque para soma.
        ossos.ifPresent("head", cabeca -> {
            cabeca.setRotX(cabeca.getRotX() - inclinacao);
            cabeca.setRotY(cabeca.getRotY() - giro);
        });
    }
}
