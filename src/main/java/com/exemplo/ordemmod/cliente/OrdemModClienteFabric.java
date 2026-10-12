package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.PacoteAbrirCriacao;
import com.exemplo.ordemmod.PacoteAviso;
import com.exemplo.ordemmod.PacoteHabilidade;
import com.exemplo.ordemmod.PacoteStatus;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Entrypoint do CLIENTE, PROVISORIO (etapa 4). So serve para provar que a rede funciona nos dois sentidos:
 * escreve no log o que chega do servidor e, 5 segundos depois de entrar no mundo, manda uma tecla de teste.
 * Na etapa 6 isto e trocado por ReceptoresCliente.registrar() (HUD, telas, teclas de verdade).
 */
public class OrdemModClienteFabric implements ClientModInitializer {
    private static int ticksDesdeEntrada = -1;
    private static boolean statusVisto = false;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(PacoteAbrirCriacao.TYPE, (m, contexto) ->
                System.out.println("[Ordem cliente] servidor pediu a tela de criacao (com Marcado: "
                        + m.comMarcado() + ")"));
        ClientPlayNetworking.registerGlobalReceiver(PacoteStatus.TYPE, (m, contexto) -> {
            if (!statusVisto) { // so o primeiro, para nao encher o log
                statusVisto = true;
                System.out.println("[Ordem cliente] status recebido: " + m);
            }
        });
        ClientPlayNetworking.registerGlobalReceiver(PacoteAviso.TYPE, (m, contexto) ->
                System.out.println("[Ordem cliente] aviso recebido: " + m.texto()));

        ClientPlayConnectionEvents.JOIN.register((handler, enviador, cliente) -> {
            ticksDesdeEntrada = 0;
            statusVisto = false;
        });
        // Ao sair do mundo o contador para: sem isto, sair antes dos 5 s mandava o pacote ja desconectado (crash).
        ClientPlayConnectionEvents.DISCONNECT.register((handler, cliente) -> ticksDesdeEntrada = -1);
        ClientTickEvents.END_CLIENT_TICK.register(cliente -> {
            if (ticksDesdeEntrada >= 0 && ++ticksDesdeEntrada == 100) {
                System.out.println("[Ordem cliente] enviando tecla de teste (USAR) ao servidor");
                ClientPlayNetworking.send(new PacoteHabilidade(PacoteHabilidade.USAR));
            }
        });
    }
}
