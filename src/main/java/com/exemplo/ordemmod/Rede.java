package com.exemplo.ordemmod;

import com.exemplo.ordemmod.marcado.PacoteMarcado;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * Registro dos pacotes do mod. No Forge era um SimpleChannel com ids numericos; no Fabric cada pacote
 * e um payload com Identifier proprio, registrado aqui (nos dois lados) e tratado em ReceptoresServidor
 * (servidor) e ReceptoresCliente (cliente).
 */
public final class Rede {
    private Rede() {
    }

    /** Cria o tipo (identificador) de um payload: ordemmod:<nome>. */
    public static <T extends CustomPacketPayload> CustomPacketPayload.Type<T> tipo(String nome) {
        return new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, nome));
    }

    /** Chamar no inicializador comum, nos dois lados, ANTES de registrar qualquer receptor. */
    public static void registrarTipos() {
        // cliente -> servidor
        PayloadTypeRegistry.serverboundPlay().register(PacoteHabilidade.TYPE, PacoteHabilidade.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PacoteCriarPersonagem.TYPE, PacoteCriarPersonagem.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PacoteArvore.TYPE, PacoteArvore.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PacoteRolarPericia.TYPE, PacoteRolarPericia.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(PacoteMarcado.TYPE, PacoteMarcado.CODEC);

        // servidor -> cliente
        PayloadTypeRegistry.clientboundPlay().register(PacoteAbrirCriacao.TYPE, PacoteAbrirCriacao.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PacoteStatus.TYPE, PacoteStatus.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PacoteAviso.TYPE, PacoteAviso.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PacoteFicha.TYPE, PacoteFicha.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PacoteTeste.TYPE, PacoteTeste.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(PacoteRolagemHud.TYPE, PacoteRolagemHud.CODEC);
    }

    /** Substitui Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador), pacote). */
    public static void paraJogador(ServerPlayer jogador, CustomPacketPayload pacote) {
        ServerPlayNetworking.send(jogador, pacote);
    }
}
