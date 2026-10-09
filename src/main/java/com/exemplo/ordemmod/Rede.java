package com.exemplo.ordemmod;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** O "canal" por onde o cliente e o servidor do mod conversam. */
public final class Rede {
    private static final String VERSAO = "7";

    public static final SimpleChannel CANAL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(OrdemMod.MOD_ID, "principal"),
            () -> VERSAO, VERSAO::equals, VERSAO::equals);

    private Rede() {
    }

    public static void registrar() {
        CANAL.registerMessage(0, PacoteHabilidade.class,
                PacoteHabilidade::escrever, PacoteHabilidade::ler, PacoteHabilidade::tratar);
        // servidor -> cliente: "abra a tela de criação de personagem"
        CANAL.registerMessage(1, PacoteAbrirCriacao.class,
                PacoteAbrirCriacao::escrever, PacoteAbrirCriacao::ler, PacoteAbrirCriacao::tratar,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // cliente -> servidor: "terminei a criação, aqui estão minhas escolhas"
        CANAL.registerMessage(2, PacoteCriarPersonagem.class,
                PacoteCriarPersonagem::escrever, PacoteCriarPersonagem::ler, PacoteCriarPersonagem::tratar,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // servidor -> cliente: sanidade, PE e NEX para a HUD desenhar as barras
        CANAL.registerMessage(3, PacoteStatus.class,
                PacoteStatus::escrever, PacoteStatus::ler, PacoteStatus::tratar,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // servidor -> cliente: um aviso curto para aparecer embaixo da mira (no lugar do chat)
        CANAL.registerMessage(4, PacoteAviso.class,
                PacoteAviso::escrever, PacoteAviso::ler, PacoteAviso::tratar,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // servidor -> cliente: a ficha do personagem, para o menu da tecla K
        CANAL.registerMessage(5, PacoteFicha.class,
                PacoteFicha::escrever, PacoteFicha::ler, PacoteFicha::tratar,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // cliente -> servidor: "quero aprender esta habilidade" / "quero esta trilha" (aba Árvore da ficha)
        CANAL.registerMessage(6, PacoteArvore.class,
                PacoteArvore::escrever, PacoteArvore::ler, PacoteArvore::tratar,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // cliente -> servidor: "cliquei nesta perícia na ficha, role um teste"
        CANAL.registerMessage(7, PacoteRolarPericia.class,
                PacoteRolarPericia::escrever, PacoteRolarPericia::ler, PacoteRolarPericia::tratar,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
        // servidor -> cliente: o resultado de um teste, para a tela do dado rolar na tela
        CANAL.registerMessage(8, PacoteTeste.class,
                PacoteTeste::escrever, PacoteTeste::ler, PacoteTeste::tratar,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // servidor -> cliente: uma rolagem já feita, para o painel acima da barra de sanidade (só estética)
        CANAL.registerMessage(9, PacoteRolagemHud.class,
                PacoteRolagemHud::escrever, PacoteRolagemHud::ler, PacoteRolagemHud::tratar,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        // cliente -> servidor: "terminei a cena do Marcado, estas foram as minhas escolhas"
        CANAL.registerMessage(10, com.exemplo.ordemmod.marcado.PacoteMarcado.class,
                com.exemplo.ordemmod.marcado.PacoteMarcado::escrever,
                com.exemplo.ordemmod.marcado.PacoteMarcado::ler,
                com.exemplo.ordemmod.marcado.PacoteMarcado::tratar,
                Optional.of(NetworkDirection.PLAY_TO_SERVER));
    }
}
