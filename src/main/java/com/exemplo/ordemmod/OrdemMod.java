package com.exemplo.ordemmod;

import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Ponto de entrada do mod no Fabric (26.2). No Forge era o @Mod + event bus.
 *
 * Ja registra: o attachment do personagem e os tipos de pacote (rede).
 * Ainda NAO registra entidades, sons nem eventos, e os receptores dos pacotes
 * (ReceptoresServidor / ReceptoresCliente / ReceptorMarcado) entram nas etapas 4 e 6.
 * Veja PORTE-FABRIC.md.
 */
public class OrdemMod implements ModInitializer {
    public static final String MOD_ID = "ordemmod";

    @Override
    public void onInitialize() {
        AnexosOrdem.registrar();
        Rede.registrarTipos();

        // Teste de fumaca do nucleo (pode apagar depois): se aparecer no log, o Java puro carregou.
        Personagem teste = new Personagem(ClasseOP.COMBATENTE);
        System.out.println("Ordem Mod carregado no Fabric! Nucleo OK (Combatente NEX " + teste.nex.getNex()
                + "%, vida maxima " + teste.vidaMaxima() + ").");
        testarSalvamentoEPacote(teste);
    }

    /**
     * Teste de fumaca da etapa 3 (pode apagar depois): o personagem vira dados, vira JSON e volta igual,
     * e um pacote escrito num buffer volta igual. Nao precisa de mundo. Mostra OK ou FALHOU no log.
     */
    private static void testarSalvamentoEPacote(Personagem p) {
        try {
            SalvamentoOrdem.DadosSalvos antes = SalvamentoOrdem.paraDados(p);
            var json = SalvamentoOrdem.DadosSalvos.CODEC.encodeStart(JsonOps.INSTANCE, antes).getOrThrow();
            SalvamentoOrdem.DadosSalvos depois = SalvamentoOrdem.DadosSalvos.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow();
            boolean salvamentoOk = antes.equals(depois) && SalvamentoOrdem.deDados(depois) != null;

            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            PacoteStatus enviado = PacoteStatus.de(p);
            PacoteStatus.escrever(enviado, buffer);
            boolean pacoteOk = enviado.equals(PacoteStatus.ler(buffer));

            System.out.println("Ordem Mod etapa 3: salvamento " + (salvamentoOk ? "OK" : "FALHOU")
                    + ", pacote " + (pacoteOk ? "OK" : "FALHOU") + ".");
        } catch (Throwable erro) {
            System.out.println("Ordem Mod etapa 3: FALHOU com excecao (cole o erro abaixo no chat).");
            erro.printStackTrace();
        }
    }
}
