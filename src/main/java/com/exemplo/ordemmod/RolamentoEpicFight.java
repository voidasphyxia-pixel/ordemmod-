package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

/**
 * Faz o jogador rolar no chão com a animação do Epic Fight (a rolagem para a frente do próprio mod).
 * A animação toca 1 tick depois do pedido: no pouso o Epic Fight também mexe na animação do jogador,
 * então esperar um tick garante que a rolada é a que fica.
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID)
public final class RolamentoEpicFight {
    private static final int ATRASO_EM_TICKS = 1;

    private static final class Pedido {
        final ServerPlayer jogador;
        int restante = ATRASO_EM_TICKS;

        Pedido(ServerPlayer jogador) {
            this.jogador = jogador;
        }
    }

    private static final List<Pedido> PEDIDOS = new ArrayList<>();

    private RolamentoEpicFight() {
    }

    /** Pede para o jogador rolar no chão (vale só no servidor). */
    public static void rolar(ServerPlayer jogador) {
        for (Pedido p : PEDIDOS) {
            if (p.jogador == jogador) {
                return; // já tem uma rolada a caminho
            }
        }
        PEDIDOS.add(new Pedido(jogador));
    }

    @SubscribeEvent
    public static void aoTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || PEDIDOS.isEmpty()) {
            return;
        }
        Iterator<Pedido> it = PEDIDOS.iterator();
        while (it.hasNext()) {
            Pedido p = it.next();
            if (p.jogador.isRemoved() || !p.jogador.isAlive()) {
                it.remove();
                continue;
            }
            if (--p.restante > 0) {
                continue;
            }
            it.remove();
            tocar(p.jogador);
        }
    }

    private static void tocar(ServerPlayer jogador) {
        try {
            ServerPlayerPatch patch = EpicFightCapabilities.getEntityPatch(jogador, ServerPlayerPatch.class);
            if (patch != null) {
                patch.playAnimationSynchronized(Animations.BIPED_ROLL_FORWARD, 0.0F);
            }
        } catch (RuntimeException erro) { // a rolada é estética: se falhar, o jogo segue
            System.err.println("[OrdemMod] não consegui tocar a rolada do Epic Fight: " + erro);
        }
    }
}
