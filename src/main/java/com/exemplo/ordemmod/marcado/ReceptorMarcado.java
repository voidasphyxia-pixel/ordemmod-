package com.exemplo.ordemmod.marcado;

import java.util.Map;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Receptor (servidor) do PacoteMarcado. Fica neste pacote porque PerfilMarcado/RoteiroMarcado
 * tem campos de acesso de pacote. AINDA NAO esta na lista de compilacao: depende de PerfilMarcado,
 * que usa getPersistentData() e precisa migrar para attachment (etapa 4).
 */
public final class ReceptorMarcado {
    private ReceptorMarcado() {
    }

    public static void registrar() {
        // roda na thread do servidor: nao precisa de enqueueWork
        ServerPlayNetworking.registerGlobalReceiver(PacoteMarcado.TYPE, (m, contexto) -> {
            ServerPlayer j = contexto.player();
            PerfilMarcado perfil = PerfilMarcado.carregar(j);
            if (perfil.concluido) {
                return; // a cena so vale uma vez (use /ordem resetar para repetir)
            }
            Map<String, Integer> tags = RoteiroMarcado.carregar().somarTags(m.escolhas());
            if (tags == null) {
                return; // escolha invalida: a cena abre de novo no proximo login
            }
            perfil.respostas = m.escolhas();
            perfil.tags.clear();
            perfil.tags.putAll(tags);
            perfil.concluido = true;
            perfil.salvar(j);
        });
    }
}
