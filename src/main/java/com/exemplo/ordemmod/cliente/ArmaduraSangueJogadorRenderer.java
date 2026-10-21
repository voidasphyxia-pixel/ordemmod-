package com.exemplo.ordemmod.cliente;

import com.exemplo.ordemmod.entidade.ArmaduraSangueJogadorEntity;
import com.exemplo.ordemmod.entidade.EntidadesOrdem;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoEntityRenderer;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.RenderPassInfo;
import com.google.common.reflect.TypeToken;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Renderer da Armadura de Sangue vestida no personagem.
 *  - posicao: usa a posicao SUAVIZADA do dono (mesma que o jogo usa para desenhar o jogador), sem atraso;
 *  - em primeira pessoa nao desenha (placas e espinhos ficariam voando na frente da camera);
 *  - escala 0,9375 como o modelo do jogador;
 *  - POSE: os ossos do Steve (head, body, left_arm, right_arm, left_leg, right_leg) copiam a pose do jogador
 *    (balanco de bracos/pernas, ataque, olhar, agachar), como uma peca de armadura. As placas sao filhas desses
 *    ossos, entao acompanham tudo. A animacao de formacao so mexe nas placas e continua tocando normal.
 *  - A pose vem, de preferencia, do PlayerModel REAL (PoseFormacao.Capturada, lida no fim do setupAnim), entao acompanha
 *    resource packs/mods que mudam a animacao do jogador (Fresh Animations Player/EMF, Not Enough Animations...).
 *    Sem captura recente, usa uma pose calculada aqui (calcularPropria).
 * Arquivos: veja {@link ArmaduraSangueJogadorEntity}.
 */
public class ArmaduraSangueJogadorRenderer
        extends GeoEntityRenderer<ArmaduraSangueJogadorEntity, LivingEntityRenderState> {

    /** Pose do dono capturada em extractRenderState: 18 rotacoes (x,y,z por osso) + 18 translacoes, na ordem de OSSOS. */
    private static final DataTicket<float[]> POSE_DONO =
            DataTicket.create("ordemmod_pose_dono", new TypeToken<float[]>() {});

    private static final int CABECA = 0, CORPO = 1, BRACO_E = 2, BRACO_D = 3, PERNA_E = 4, PERNA_D = 5;
    private static final String[] OSSOS = {"head", "body", "left_arm", "right_arm", "left_leg", "right_leg"};

    public ArmaduraSangueJogadorRenderer(EntityRendererProvider.Context contexto) {
        super(contexto, EntidadesOrdem.ARMADURA_SANGUE_JOGADOR);
        this.shadowRadius = 0.0F;
        this.scaleWidth = 0.9375F;
        this.scaleHeight = 0.9375F;
    }

    @Override
    public void extractRenderState(ArmaduraSangueJogadorEntity entidade, LivingEntityRenderState estado, float parcial) {
        super.extractRenderState(entidade, estado, parcial);
        Entity dono = entidade.getDono();
        if (dono == null) {
            return;
        }
        Vec3 p = dono.getPosition(parcial);
        estado.x = p.x;
        estado.y = p.y;
        estado.z = p.z;

        Minecraft mc = Minecraft.getInstance();
        if (dono == mc.player && mc.options.getCameraType().isFirstPerson()) {
            estado.isInvisible = true;
            estado.addGeckolibData(DataTickets.INVISIBLE_TO_PLAYER, true);
        }

        if (dono instanceof LivingEntity vivo) {
            estado.addGeckolibData(POSE_DONO, calcularPose(vivo, parcial));
        }
    }

    /** Monta a pose dos 6 ossos: da captura do modelo real do jogador, ou calculada aqui se nao houver captura recente. */
    private static float[] calcularPose(LivingEntity vivo, float parcial) {
        float[] r = new float[OSSOS.length * 6]; // 0..17 rotacao (x,y,z por osso) / 18..35 translacao (x,y,z por osso)

        float[] cap = PoseFormacao.Capturada.obter(vivo.getId());
        if (cap != null) {
            lerCapturada(cap, r);
        } else {
            calcularPropria(vivo, parcial, r);
        }

        // Os bracos sao FILHOS do corpo no modelo da armadura (as placas seguem o torso), entao tira do braco o que o corpo ja aplica.
        for (int k : new int[] {BRACO_E, BRACO_D}) {
            r[k * 3] -= r[CORPO * 3];
            r[k * 3 + 1] -= r[CORPO * 3 + 1];
            r[18 + k * 3] -= r[18 + CORPO * 3];
            r[18 + k * 3 + 1] -= r[18 + CORPO * 3 + 1];
            r[18 + k * 3 + 2] -= r[18 + CORPO * 3 + 2];
        }
        return r;
    }

    /** Converte a pose real do modelo do jogador (PoseFormacao.Capturada) para rotacao/translacao dos ossos. */
    private static void lerCapturada(float[] cap, float[] r) {
        // Compensacao do deslocamento padrao de cada parte do modelo do jogador (mesmas contas do GeoArmorRenderer):
        // esquerdo x-5 / x-2, direito x+5 / x+2, y = base - y (cabeca/corpo: -y). Ordem: head, body, l_arm, r_arm, l_leg, r_leg.
        final float[] somaX = {0, 0, -5, 5, -2, 2};
        final float[] baseY = {0, 0, 2, 2, 12, 12};
        for (int k = 0; k < OSSOS.length; k++) {
            int o = k * PoseFormacao.Capturada.CAMPOS;
            r[k * 3] = cap[o + 3];
            r[k * 3 + 1] = cap[o + 4];
            r[k * 3 + 2] = cap[o + 5];
            r[18 + k * 3] = cap[o] + somaX[k];
            r[18 + k * 3 + 1] = baseY[k] - cap[o + 1];
            r[18 + k * 3 + 2] = cap[o + 2];
        }
    }

    /** Pose propria (sem captura): reproduz o basico do modelo humanoide do jogo (andar, ataque, olhar, agachar). */
    private static void calcularPropria(LivingEntity vivo, float parcial, float[] r) {
        float pos = vivo.walkAnimation.position(parcial);
        float vel = Math.min(1.0F, vivo.walkAnimation.speed(parcial));
        float idade = vivo.tickCount + parcial;
        boolean agachado = vivo.isCrouching();

        // cabeca: giro relativo ao corpo + inclinacao
        float giroCabeca = Mth.wrapDegrees(
                Mth.rotLerp(parcial, vivo.yHeadRotO, vivo.getYHeadRot())
                        - Mth.rotLerp(parcial, vivo.yBodyRotO, vivo.yBodyRot));
        giroCabeca = Mth.clamp(giroCabeca, -85.0F, 85.0F);
        float inclina = Mth.lerp(parcial, vivo.xRotO, vivo.getXRot());
        r[CABECA * 3] = inclina * Mth.DEG_TO_RAD;
        r[CABECA * 3 + 1] = giroCabeca * Mth.DEG_TO_RAD;

        // bracos e pernas (formula do HumanoidModel)
        float c = pos * 0.6662F;
        r[BRACO_D * 3] = Mth.cos(c + Mth.PI) * 2.0F * vel * 0.5F;
        r[BRACO_E * 3] = Mth.cos(c) * 2.0F * vel * 0.5F;
        r[PERNA_D * 3] = Mth.cos(c) * 1.4F * vel;
        r[PERNA_E * 3] = Mth.cos(c + Mth.PI) * 1.4F * vel;

        // balanco parado (respirar)
        r[BRACO_D * 3 + 2] += Mth.cos(idade * 0.09F) * 0.05F + 0.05F;
        r[BRACO_E * 3 + 2] -= Mth.cos(idade * 0.09F) * 0.05F + 0.05F;
        r[BRACO_D * 3] += Mth.sin(idade * 0.067F) * 0.05F;
        r[BRACO_E * 3] -= Mth.sin(idade * 0.067F) * 0.05F;

        // ataque / usar item com o braco principal
        float ataque = vivo.getAttackAnim(parcial);
        if (ataque > 0.0F) {
            float balanco = Mth.sin(ataque * Mth.PI) * 1.6F;
            if (vivo.getMainArm() == HumanoidArm.RIGHT) {
                r[BRACO_D * 3] -= 0.6F + balanco;
                r[CORPO * 3 + 1] = Mth.sin(Mth.sqrt(ataque) * Mth.TWO_PI) * 0.2F;
            } else {
                r[BRACO_E * 3] -= 0.6F + balanco;
                r[CORPO * 3 + 1] = -Mth.sin(Mth.sqrt(ataque) * Mth.TWO_PI) * 0.2F;
            }
        }

        // formacao: braco direito levantado (igual ao do personagem, veja PoseFormacao)
        float levanta = PoseFormacao.progresso(vivo.getId(), parcial);
        if (levanta > 0.0F) {
            r[BRACO_D * 3] = PoseFormacao.mesclarX(levanta, r[BRACO_D * 3]);
            r[BRACO_D * 3 + 1] = PoseFormacao.mesclarY(levanta, r[BRACO_D * 3 + 1]);
            r[BRACO_D * 3 + 2] = PoseFormacao.mesclarZ(levanta, r[BRACO_D * 3 + 2]);
        }

        if (agachado) {
            r[CORPO * 3] = 0.5F;
            r[BRACO_D * 3] += 0.4F;
            r[BRACO_E * 3] += 0.4F;
            r[18 + CABECA * 3 + 1] = -4.2F; // cabeca e corpo descem
            r[18 + CORPO * 3 + 1] = -3.2F;
            r[18 + BRACO_E * 3 + 1] = -3.2F;
            r[18 + BRACO_D * 3 + 1] = -3.2F;
        }
    }

    @Override
    public void adjustModelBonesForRender(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots snapshots) {
        super.adjustModelBonesForRender(info, snapshots);
        float[] pose = info.renderState().getGeckolibData(POSE_DONO);
        if (pose == null) {
            return;
        }
        for (int i = 0; i < OSSOS.length; i++) {
            final int k = i;
            snapshots.get(OSSOS[i]).ifPresent(osso -> {
                // mesma convencao do GeoArmorRenderer: rotacao (-xRot, -yRot, +zRot)
                osso.setRotX(-pose[k * 3]).setRotY(-pose[k * 3 + 1]).setRotZ(pose[k * 3 + 2]);
                osso.setTranslateX(pose[18 + k * 3]).setTranslateY(pose[18 + k * 3 + 1]).setTranslateZ(pose[18 + k * 3 + 2]);
            });
        }
    }

    @Override
    protected float getDeathMaxRotation(GeoRenderState estadoDeRender) {
        return 0.0F;
    }

    @Override
    protected AABB getBoundingBoxForCulling(ArmaduraSangueJogadorEntity entidade) {
        return super.getBoundingBoxForCulling(entidade).inflate(2.0D);
    }
}
