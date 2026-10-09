package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.entidade.EntidadesOrdem;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Liga o renderer (GeckoLib) ao Zumbi de Sangue. So roda no cliente. */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClienteEntidades {

    private ClienteEntidades() {
    }

    @SubscribeEvent
    public static void registrarRenderers(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(EntidadesOrdem.ZUMBI_SANGUE.get(), ZumbiSangueRenderer::new);
    }
}
