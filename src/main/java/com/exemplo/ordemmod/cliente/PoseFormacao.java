package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.ArmaduraSangueJogadorEntity;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Pose de "formacao" da Armadura de Sangue: enquanto a armadura se forma (3,7 s) o braco direito do PERSONAGEM sobe,
 * fica erguido e desce no fim. O mixin do PlayerModel aplica isso no braco real e o ArmaduraSangueJogadorRenderer
 * aplica o mesmo valor no osso right_arm da armadura, entao os dois ficam juntos.
 * Ajuste ANGULO_X / ANGULO_Y e os tempos abaixo para mudar o gesto.
 */
public final class PoseFormacao {
    /** Rotacao do braco levantado (radianos, convencao do ModelPart do jogo): -1,9 = braco para frente e para cima. */
    public static final float ANGULO_X = -1.9F;
    public static final float ANGULO_Y = -0.25F;
    public static final float ANGULO_Z = 0.0F;

    private static final float SOBE_ATE = 10.0F;   // ticks
    private static final float DESCE_DE = 55.0F;
    private static final float FIM = 74.0F;        // 3,7083 s da animacao de formacao

    private PoseFormacao() {
    }

    /** 0 = braco normal, 1 = braco totalmente levantado. Usa a armadura do jogador de id donoId (0 se nao houver). */
    public static float progresso(int donoId, float parcial) {
        ArmaduraSangueJogadorEntity armadura = ArmaduraSangueJogadorEntity.doDono(donoId);
        if (armadura == null) {
            return 0.0F;
        }
        float t = armadura.tickCount + parcial;
        if (t <= 0.0F || t >= FIM) {
            return 0.0F;
        }
        float p;
        if (t < SOBE_ATE) {
            p = t / SOBE_ATE;
        } else if (t < DESCE_DE) {
            p = 1.0F;
        } else {
            p = 1.0F - (t - DESCE_DE) / (FIM - DESCE_DE);
        }
        return p * p * (3.0F - 2.0F * p); // suaviza
    }

    public static float mesclarX(float p, float atual) {
        return Mth.lerp(p, atual, ANGULO_X);
    }

    public static float mesclarY(float p, float atual) {
        return Mth.lerp(p, atual, ANGULO_Y);
    }

    public static float mesclarZ(float p, float atual) {
        return Mth.lerp(p, atual, ANGULO_Z);
    }

    /** Aplica a pose no braco do modelo do jogador. */
    public static void aplicar(ModelPart braco, float p) {
        braco.xRot = mesclarX(p, braco.xRot);
        braco.yRot = mesclarY(p, braco.yRot);
        braco.zRot = mesclarZ(p, braco.zRot);
    }
}
