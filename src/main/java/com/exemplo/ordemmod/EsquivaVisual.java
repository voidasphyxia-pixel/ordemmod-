package com.exemplo.ordemmod;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * PROVISORIO (etapa 4d). No Forge a esquiva e a rolada no chao tocavam animacoes do Epic Fight
 * (EsquivaEpicFight e RolamentoEpicFight, que continuam na lista de pendencias da etapa 7).
 * Sem o Epic Fight, aqui so sai uma nuvenzinha de fumaca; a decisao (esquivou ou nao, reduziu a queda ou nao)
 * ja foi tomada pelo servidor e aparece no painel da rolagem.
 * Na etapa 7 isto vira o passo de esquiva e a rolada de verdade.
 */
public final class EsquivaVisual {
    private EsquivaVisual() {
    }

    /** O jogador esquivou de um ataque. {@code fonte} sera usada na etapa 7 para escolher o passo (veja EsquivaReflexos.Passo). */
    public static void esquivar(ServerPlayer jogador, DamageSource fonte) {
        fumaca(jogador, 8);
    }

    /** O jogador reduziu o dano da queda e rola no chao. */
    public static void rolar(ServerPlayer jogador) {
        fumaca(jogador, 4);
    }

    private static void fumaca(ServerPlayer jogador, int quantidade) {
        if (jogador.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.POOF, jogador.getX(), jogador.getY(0.2), jogador.getZ(),
                    quantidade, 0.3, 0.1, 0.3, 0.02);
        }
    }
}
