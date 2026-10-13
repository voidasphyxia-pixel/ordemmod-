package com.exemplo.ordemmod;

import com.exemplo.ordemmod.marcado.ReceptorMarcado;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Ponto de entrada do mod no Fabric (26.2). No Forge era o @Mod + event bus.
 *
 * Ja registra: o attachment do personagem, os tipos de pacote (rede), os eventos de ciclo de vida do
 * personagem e os comandos (EventosOrdem), dano/efeitos/sono/estado mental (EventosDano), os receptores
 * do servidor (ReceptoresServidor), as pericias (EventosPericias), as origens (EventosOrigens), o receptor
 * da cena do Marcado (ReceptorMarcado) e os sons (SonsOrdem).
 * Ainda NAO registra entidades.
 * Veja PORTE-FABRIC.md.
 */
public class OrdemMod implements ModInitializer {
    public static final String MOD_ID = "ordemmod";

    @Override
    public void onInitialize() {
        AnexosOrdem.registrar();
        Rede.registrarTipos();
        EventosOrdem.registrar();
        EventosDano.registrar();
        ReceptoresServidor.registrar();
        // etapa 4d
        EventosPericias.registrar();
        EventosOrigens.registrar();
        ReceptorMarcado.registrar();
        // etapa 5a
        SonsOrdem.registrar();

        // Teste de fumaca do nucleo (pode apagar depois): se aparecer no log, o Java puro carregou.
        Personagem teste = new Personagem(ClasseOP.COMBATENTE);
        System.out.println("Ordem Mod carregado no Fabric! Nucleo OK (Combatente NEX " + teste.nex.getNex()
                + "%, vida maxima " + teste.vidaMaxima() + ").");
        testarSalvamentoEPacote(teste);
        testarSons();
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

    /**
     * Teste de fumaca da etapa 5a (pode apagar depois): os sons entraram no registro do jogo com o id certo?
     * Mostra OK ou FALHOU no log.
     */
    private static void testarSons() {
        try {
            var chave = BuiltInRegistries.SOUND_EVENT.getKey(SonsOrdem.DADO_ROLANDO);
            boolean ok = chave != null && SonsOrdem.MARCADO.size() == SonsOrdem.NOMES_MARCADO.length;
            System.out.println("Ordem Mod etapa 5a: sons " + (ok ? "OK" : "FALHOU") + " (dado_rolando = " + chave
                    + ", " + SonsOrdem.MARCADO.size() + " sons do Marcado).");
        } catch (Throwable erro) {
            System.out.println("Ordem Mod etapa 5a: FALHOU com excecao (cole o erro abaixo no chat).");
            erro.printStackTrace();
        }
    }
}
