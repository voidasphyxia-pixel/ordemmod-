package com.exemplo.ordemmod;

import com.exemplo.ordemmod.entidade.EntidadesOrdem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(OrdemMod.MOD_ID)
public class OrdemMod {
    public static final String MOD_ID = "ordemmod";

    public OrdemMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::preparar);
        EntidadesOrdem.registrar(bus);
        SonsOrdem.registrar(bus);
        System.out.println("Ordem Mod carregado!");
    }

    /** Roda uma vez na partida do jogo: abre o canal de mensagens. */
    private void preparar(FMLCommonSetupEvent e) {
        e.enqueueWork(Rede::registrar);
    }
}
