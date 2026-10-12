package com.exemplo.ordemmod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * O que o CLIENTE faz quando chega cada pacote do servidor (antes era o metodo tratar() de cada Pacote*).
 * No Fabric o receptor do cliente ja roda na thread do jogo, entao nao existe enqueueWork nem DistExecutor.
 *
 * So pode ser carregada no cliente: registrar() deve ser chamado de um ClientModInitializer
 * (entrypoint "client" do fabric.mod.json, criado na etapa 6).
 * AINDA NAO esta na lista de compilacao: depende de HudOrdem, AvisoOrdem, RolagemHud, ClienteCriacao,
 * ClienteFicha e ClienteTeste, que sao portados na etapa 6.
 */
public final class ReceptoresCliente {
    private ReceptoresCliente() {
    }

    public static void registrar() {
        ClientPlayNetworking.registerGlobalReceiver(PacoteAbrirCriacao.TYPE,
                (m, contexto) -> ClienteCriacao.abrir(m.comMarcado()));
        ClientPlayNetworking.registerGlobalReceiver(PacoteStatus.TYPE,
                (m, contexto) -> HudOrdem.receber(m));
        ClientPlayNetworking.registerGlobalReceiver(PacoteAviso.TYPE,
                (m, contexto) -> AvisoOrdem.receber(m.texto(), m.cor()));
        ClientPlayNetworking.registerGlobalReceiver(PacoteFicha.TYPE,
                (m, contexto) -> ClienteFicha.abrir(m.dados()));
        ClientPlayNetworking.registerGlobalReceiver(PacoteTeste.TYPE,
                (m, contexto) -> ClienteTeste.abrir(m));
        ClientPlayNetworking.registerGlobalReceiver(PacoteRolagemHud.TYPE,
                (m, contexto) -> RolagemHud.receber(m));
    }

    /** Substitui Rede.CANAL.sendToServer(pacote). */
    public static void enviar(CustomPacketPayload pacote) {
        ClientPlayNetworking.send(pacote);
    }
}
