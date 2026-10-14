package com.exemplo.ordemmod;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * O que o CLIENTE faz quando chega cada pacote do servidor (antes era o metodo tratar() de cada Pacote*).
 * No Fabric o receptor do cliente ja roda na thread do jogo, entao nao existe enqueueWork nem DistExecutor.
 *
 * So pode ser carregada no cliente: registrar() e chamado de OrdemModClienteFabric (entrypoint "client").
 *
 * Etapa 6a: status (HUD), aviso e rolagem (painel da HUD) ja sao de verdade. As tres telas (criacao, ficha e dado)
 * ainda sao PROVISORIAS: ClienteCriacao, ClienteFicha e ClienteTeste so escrevem no log ate a etapa 6b portar as telas.
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
