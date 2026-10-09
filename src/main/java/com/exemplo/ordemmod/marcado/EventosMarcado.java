package com.exemplo.ordemmod.marcado;

import com.exemplo.ordemmod.OrdemMod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Garante que o perfil do Marcado sobrevive à morte (o Forge cria um jogador novo no respawn). */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID)
public final class EventosMarcado {
    private EventosMarcado() {
    }

    @SubscribeEvent
    public static void aoClonar(PlayerEvent.Clone e) {
        if (e.getOriginal() instanceof ServerPlayer antigo && e.getEntity() instanceof ServerPlayer novo) {
            PerfilMarcado.copiar(antigo, novo);
        }
    }
}
