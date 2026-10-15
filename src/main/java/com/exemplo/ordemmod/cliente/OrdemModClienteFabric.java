package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.AvisoOrdem;
import com.exemplo.ordemmod.HudOrdem;
import com.exemplo.ordemmod.ReceptoresCliente;
import com.exemplo.ordemmod.RolagemHud;
import com.exemplo.ordemmod.TeclasOrdem;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/**
 * Entrypoint do CLIENTE (entrypoint "client" do fabric.mod.json). Etapa 6a: no lugar do provisorio da etapa 4
 * (que so escrevia a rede no log) agora liga o que e de verdade:
 *  - ReceptoresCliente: o que fazer quando chega cada pacote do servidor (HUD, avisos, painel da rolagem; as telas
 *    ainda sao provisorias ate a 6b);
 *  - TeclasOrdem: as teclas R, G e K (Controles > Ordem Mod);
 *  - HudOrdem.registrar(): as barras, a hotbar nova, o painel da rolagem e os avisos;
 *  - ClienteEntidades: o renderer do Zumbi de Sangue (etapa 5b).
 */
public class OrdemModClienteFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // etapa 5b: renderer do Zumbi de Sangue
        ClienteEntidades.registrar();

        // etapa 6a
        ReceptoresCliente.registrar();
        TeclasOrdem.registrar();
        HudOrdem.registrar();
        com.exemplo.ordemmod.titulo.EventosTitulo.registrar();

        // Ao sair do mundo (ou cair a conexao), a HUD esquece o que o servidor mandou e os paineis somem.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, cliente) -> {
            HudOrdem.limpar();
            AvisoOrdem.limpar();
            RolagemHud.limpar();
        });
    }
}
