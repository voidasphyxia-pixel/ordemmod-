package com.exemplo.ordemmod.aparencia;

import java.util.List;

import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.Rede;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

/**
 * Lado do servidor: guarda a aparência dentro dos dados do jogador (chave própria, separada do
 * personagem), manda para quem está por perto e oferece o comando de teste /aparencia.
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID)
public final class AparenciaServidor {
    private AparenciaServidor() {
    }

    /** A aparência salva do jogador, ou null se ele nunca escolheu uma. */
    public static AparenciaData obter(Player jogador) {
        CompoundTag dados = jogador.getPersistentData();
        if (!dados.contains(AparenciaData.RAIZ, Tag.TAG_COMPOUND)) {
            return null;
        }
        return AparenciaData.deNbt(dados.getCompound(AparenciaData.RAIZ));
    }

    /** Salva e avisa o próprio jogador e quem está olhando para ele. {@code dados} nulo apaga. */
    public static void definir(ServerPlayer jogador, AparenciaData dados) {
        if (dados == null) {
            jogador.getPersistentData().remove(AparenciaData.RAIZ);
        } else {
            jogador.getPersistentData().put(AparenciaData.RAIZ, dados.paraNbt());
        }
        Rede.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> jogador),
                new PacoteAparencia(jogador.getUUID(), dados));
    }

    private static void reenviar(ServerPlayer jogador) {
        AparenciaData dados = obter(jogador);
        if (dados != null) {
            Rede.CANAL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> jogador),
                    new PacoteAparencia(jogador.getUUID(), dados));
        }
    }

    @SubscribeEvent
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            reenviar(jogador);
        }
    }

    @SubscribeEvent
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent evento) {
        if (evento.getEntity() instanceof ServerPlayer jogador) {
            reenviar(jogador);
        }
    }

    /** Ao morrer e renascer o jogador é recriado: leva a aparência junto. */
    @SubscribeEvent
    public static void aoClonar(PlayerEvent.Clone evento) {
        CompoundTag antigo = evento.getOriginal().getPersistentData();
        if (antigo.contains(AparenciaData.RAIZ, Tag.TAG_COMPOUND)) {
            evento.getEntity().getPersistentData().put(AparenciaData.RAIZ,
                    antigo.getCompound(AparenciaData.RAIZ).copy());
        }
    }

    /** Quando um jogador passa a ver outro, manda a aparência do outro para ele. */
    @SubscribeEvent
    public static void aoComecarASeguir(PlayerEvent.StartTracking evento) {
        if (evento.getTarget() instanceof ServerPlayer alvo && evento.getEntity() instanceof ServerPlayer observador) {
            AparenciaData dados = obter(alvo);
            if (dados != null) {
                Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> observador),
                        new PacoteAparencia(alvo.getUUID(), dados));
            }
        }
    }

    // ---- comandos de teste (antes de a tela de aparência existir) ----

    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent evento) {
        evento.getDispatcher().register(Commands.literal("aparencia")
                .then(Commands.literal("aleatoria").executes(c -> aleatoria(c.getSource())))
                .then(Commands.literal("limpar").executes(c -> {
                    ServerPlayer jogador = c.getSource().getPlayerOrException();
                    definir(jogador, null);
                    c.getSource().sendSuccess(() -> Component.literal("Aparência removida: voltou a skin normal."), false);
                    return 1;
                }))
                .then(Commands.literal("pele")
                        .then(Commands.argument("indice", IntegerArgumentType.integer(0, Paletas.PELE.length - 1))
                                .executes(c -> {
                                    ServerPlayer jogador = c.getSource().getPlayerOrException();
                                    AparenciaData atual = obter(jogador);
                                    if (atual == null) {
                                        atual = AparenciaData.padrao();
                                    }
                                    definir(jogador, atual.comPele(IntegerArgumentType.getInteger(c, "indice")));
                                    return 1;
                                }))));
    }

    private static int aleatoria(CommandSourceStack origem) throws CommandSyntaxException {
        ServerPlayer jogador = origem.getPlayerOrException();
        RandomSource sorteio = jogador.getRandom();
        AparenciaData dados = AparenciaData.padrao().comPele(sorteio.nextInt(Paletas.PELE.length));
        for (String slot : CatalogoPecas.SLOTS) {
            if (slot.equals("facial_hair") && sorteio.nextInt(5) != 0) {
                continue; // barba: 1 em 5
            }
            if (slot.equals("outerwear") && sorteio.nextInt(5) >= 2) {
                continue; // casaco: 2 em 5
            }
            List<CatalogoPecas.Peca> opcoes = CatalogoPecas.doSlot(slot);
            CatalogoPecas.Peca escolhida = opcoes.get(sorteio.nextInt(opcoes.size()));
            dados = dados.comPeca(slot, escolhida.id());
            if (escolhida.tingivel()) {
                dados = dados.comCor(slot, Paletas.CABELO[sorteio.nextInt(Paletas.CABELO.length)]);
            }
        }
        definir(jogador, dados);
        origem.sendSuccess(() -> Component.literal("Aparência aleatória aplicada."), false);
        return 1;
    }
}
