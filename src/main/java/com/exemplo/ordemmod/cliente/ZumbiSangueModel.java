package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.entidade.ZumbiSangueEntity;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * Arquivos usados (em src/main/resources/assets/ordemmod/):
 *   geo/entity/zumbi_sangue.geo.json
 *   animations/entity/zumbi_sangue.animation.json
 *   textures/entity/zumbi_sangue.png
 */
public class ZumbiSangueModel extends DefaultedEntityGeoModel<ZumbiSangueEntity> {

    private static final float GRAUS_PARA_RAD = (float) (Math.PI / 180.0D);

    public ZumbiSangueModel() {
        super(new ResourceLocation(OrdemMod.MOD_ID, "zumbi_sangue"));
    }

    /** A cabeca acompanha o olhar, mas so em pe (deitado/levantando a animacao manda). */
    @Override
    public void setCustomAnimations(ZumbiSangueEntity zumbi, long instanceId,
                                    AnimationState<ZumbiSangueEntity> estado) {
        if (!zumbi.podeAgir() || zumbi.isDeadOrDying()) {
            return;
        }
        CoreGeoBone cabeca = this.getAnimationProcessor().getBone("head");
        if (cabeca == null) {
            return;
        }
        EntityModelData dados = estado.getData(DataTickets.ENTITY_MODEL_DATA);
        cabeca.setRotX(cabeca.getRotX() + dados.headPitch() * GRAUS_PARA_RAD);
        cabeca.setRotY(cabeca.getRotY() + dados.netHeadYaw() * GRAUS_PARA_RAD);
    }
}
