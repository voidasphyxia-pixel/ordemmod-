package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.EntidadesOrdem;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/**
 * Liga o renderer (GeckoLib) ao Zumbi de Sangue. So roda no cliente.
 * No Forge era um @EventBusSubscriber no EntityRenderersEvent.RegisterRenderers; no Fabric e uma chamada direta
 * a partir do entrypoint do cliente (OrdemModClienteFabric.onInitializeClient).
 */
public final class ClienteEntidades {
    private ClienteEntidades() {
    }

    public static void registrar() {
        EntityRendererRegistry.register(EntidadesOrdem.ZUMBI_SANGUE, ZumbiSangueRenderer::new);
    }
}
