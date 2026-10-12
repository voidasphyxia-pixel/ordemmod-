package com.exemplo.ordemmod;

import java.util.List;
import java.util.Random;

import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;

/**
 * Etapa 4b: o que no Forge eram LivingHurtEvent, MobEffectEvent.Applicable e PlayerWakeUpEvent, mais o estado
 * mental (efeitos da sanidade baixa).
 *
 * O Fabric so deixa CANCELAR dano (ALLOW_DAMAGE) ou observar (AFTER_DAMAGE); nao deixa ALTERAR. Por isso
 * {@link #aoSofrerDano} e {@link #resisteAoEfeito} sao chamados por um mixin em LivingEntity
 * (veja mixin/LivingEntityMixin). O resto usa eventos do Fabric API.
 */
public final class EventosDano {
    /** O acerto critico multiplica o dano final por isso. */
    private static final double MULTIPLICADOR_CRITICO = 2.0;

    /** Sorteio dos criticos e das resistencias. */
    private static final Random ALEATORIO = new Random();

    /** Fica true enquanto o proprio mod aplica um efeito (a sanidade), para o jogador nao "resistir" a ele. */
    private static boolean aplicandoEfeitoProprio = false;

    private EventosDano() {
    }

    /** Liga os callbacks do Fabric. Chamar uma vez, depois de EventosOrdem.registrar(). */
    public static void registrar() {
        // Ainda na tela de criacao: nada machuca o jogador (era e.setCanceled(true)).
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entidade, fonte, quantidade) ->
                !(entidade instanceof ServerPlayer jogador) || EventosOrdem.de(jogador).isCriado());

        // Dormir uma noite de verdade devolve toda a sanidade (era PlayerWakeUpEvent).
        EntitySleepEvents.STOP_SLEEPING.register((entidade, posicao) -> {
            if (entidade instanceof ServerPlayer jogador && jogador.getSleepTimer() >= 100) {
                EventosOrdem.de(jogador).descansar();
            }
        });

        // Estado mental, 1 vez por segundo. Registrado depois do tick do EventosOrdem, entao roda depois
        // do passarSegundo, como no original.
        ServerTickEvents.END_SERVER_TICK.register(servidor -> {
            for (ServerPlayer jogador : List.copyOf(servidor.getPlayerList().getPlayers())) {
                if (jogador.tickCount % 20 == 0 && !jogador.isCreative() && !jogador.isSpectator()) {
                    aplicarEstadoMental(jogador, EventosOrdem.de(jogador));
                }
            }
        });
    }

    // ------------------------------------------------------------------ dano

    /**
     * Chamado pelo mixin antes de a vida ser tirada. Recebe o dano e devolve o dano novo: reduz o que o
     * jogador recebe e aumenta o que ele causa.
     */
    public static float aoSofrerDano(LivingEntity alvo, DamageSource fonte, float quantidade) {
        float resultado = quantidade;

        if (alvo instanceof ServerPlayer vitima) {
            Personagem p = EventosOrdem.de(vitima);
            if (p.isCriado()) {
                double recebido = p.habilidades.danoRecebidoFinal(resultado, false);
                recebido *= reducaoDePericias(vitima, p, fonte); // Fortitude, Ciencias, Religiao, Pilotagem
                resultado = (float) recebido;
                Entity origem = fonte.getEntity();
                if (origem instanceof Enemy && !vitima.isCreative()) {
                    int perda = perdaDeSanidade(origem);
                    // Vontade (e Ocultismo, se for paranormal) amortecem o susto
                    p.perderSanidade(p.perdaDeSanidadeAjustada(perda, perda > 1, ALEATORIO));
                }
            }
        }

        if (fonte.getEntity() instanceof ServerPlayer atacante) {
            Personagem p = EventosOrdem.de(atacante);
            TipoDano tipo;
            if (fonte.getDirectEntity() != atacante) {
                tipo = TipoDano.A_DISTANCIA; // flecha, projetil...
            } else if (atacante.getMainHandItem().isEmpty()) {
                tipo = TipoDano.DESARMADO;
            } else {
                tipo = TipoDano.CORPO_A_CORPO;
            }
            double base = resultado;
            if (tipo != TipoDano.A_DISTANCIA) {
                base += EfeitosAtributos.bonusDano(p.atributos); // Forca
            }
            base += p.danoFixoDeOrigem(tipo); // Lutador, Militar
            base *= p.pericias.multiplicadorDano(tipo); // Luta ou Pontaria: +% por patamar
            double dano = p.habilidades.danoFinal(base, tipo, p.efeitos);
            dano *= multiplicadorSituacional(atacante, p, alvo); // Ocultismo, Tatica
            if (p.sorteouCritico(tipo, ALEATORIO)) {
                dano *= MULTIPLICADOR_CRITICO + p.pericias.bonusMultiplicadorCritico()
                        + p.origem(Origem.Efeito.CRITICO_MULT); // Investigacao, T.I.
                avisarCritico(atacante, alvo);
            }
            resultado = (float) dano;
        }
        return resultado;
    }

    /** Mortos-vivos e criaturas do Outro Lado (alvo de Ocultismo, origem do medo de Religiao). */
    private static boolean ehDoOutroLado(Entity entidade) {
        // O MobType.UNDEAD do Forge 1.20.1 virou a tag minecraft:undead.
        return entidade instanceof LivingEntity vivo
                && (vivo.getType().is(EntityTypeTags.UNDEAD) || vivo.getType() == EntityType.WARDEN);
    }

    /** Quanto do dano o jogador ainda recebe depois das pericias defensivas (1.0 = tudo, 0.8 = -20%). */
    private static double reducaoDePericias(ServerPlayer vitima, Personagem p, DamageSource fonte) {
        if (fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { // vazio e /kill nao se reduzem
            return 1.0;
        }
        double fator = (1.0 - p.pericias.valor(Beneficio.FORTITUDE_REDUCAO))
                * (1.0 - p.origem(Origem.Efeito.REDUCAO_DANO)); // Vitima
        if (fonte.is(DamageTypeTags.IS_FIRE) || fonte.is(DamageTypeTags.IS_EXPLOSION)) {
            fator *= 1.0 - p.pericias.valor(Beneficio.CIENCIAS_AMBIENTE);
        }
        if (fonte.is(DamageTypeTags.IS_EXPLOSION)) {
            fator *= 1.0 - p.origem(Origem.Efeito.REDUCAO_EXPLOSAO); // Engenheiro
        }
        if (ehDoOutroLado(fonte.getEntity())) {
            fator *= 1.0 - p.pericias.valor(Beneficio.RELIGIAO_MORTOS_VIVOS);
        }
        if (vitima.isPassenger()) {
            fator *= 1.0 - p.pericias.valor(Beneficio.PILOTAGEM_MONTADO);
        }
        return fator;
    }

    /** Bonus de dano que dependem da situacao: alvo morto-vivo (Ocultismo) e aliado por perto (Tatica). */
    private static double multiplicadorSituacional(ServerPlayer atacante, Personagem p, LivingEntity alvo) {
        double fator = 1.0;
        if (ehDoOutroLado(alvo)) {
            fator += p.pericias.valor(Beneficio.OCULTISMO_MORTOS_VIVOS) + p.origem(Origem.Efeito.DANO_MORTOS_VIVOS);
        }
        double tatica = p.pericias.valor(Beneficio.TATICA_ALIADOS);
        if (tatica > 0 && !atacante.level().getEntitiesOfClass(ServerPlayer.class,
                atacante.getBoundingBox().inflate(12.0),
                o -> o != atacante && o.isAlive() && !o.isSpectator()).isEmpty()) {
            fator += tatica;
        }
        return fator;
    }

    /** Mostra "CRITICO!" e faz faiscas no alvo. */
    private static void avisarCritico(ServerPlayer atacante, LivingEntity alvo) {
        atacante.sendOverlayMessage(Component.literal("CRÍTICO!"));
        if (alvo.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.CRIT, alvo.getX(), alvo.getY(0.5), alvo.getZ(),
                    15, 0.3, 0.3, 0.3, 0.2);
        }
    }

    /** Quanta SAN se perde ao apanhar desta criatura. */
    private static int perdaDeSanidade(Entity fonte) {
        EntityType<?> tipo = fonte.getType();
        if (tipo == EntityType.ENDERMAN || tipo == EntityType.PHANTOM || tipo == EntityType.WARDEN
                || tipo == EntityType.WITHER || tipo == EntityType.ENDER_DRAGON) {
            return 3; // criaturas mais "paranormais"
        }
        return 1;
    }

    // ------------------------------------------------------------------ efeitos

    /**
     * Chamado pelo mixin quando um efeito vai atingir alguem. Devolve true se o jogador RESISTIU
     * (veneno, fraqueza, lentidao... ruins): Vontade (mental) ou Fortitude (fisico).
     */
    public static boolean resisteAoEfeito(LivingEntity alvo, MobEffectInstance instancia) {
        if (aplicandoEfeitoProprio || !(alvo instanceof ServerPlayer jogador)) {
            return false;
        }
        MobEffect efeito = instancia.getEffect().value();
        if (efeito.getCategory() != MobEffectCategory.HARMFUL) {
            return false;
        }
        Personagem p = EventosOrdem.de(jogador);
        // Na 1.21.2+ CONFUSION virou NAUSEA e MOVEMENT_SLOWDOWN virou SLOWNESS.
        boolean mental = instancia.is(MobEffects.DARKNESS)
                || instancia.is(MobEffects.NAUSEA)
                || instancia.is(MobEffects.BLINDNESS);
        if (p.resistiuAoEfeito(ALEATORIO, mental)) {
            Avisos.enviar(jogador, "Resistiu a " + efeito.getDisplayName().getString() + "!", ChatFormatting.GREEN);
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ estado mental

    /** Avisa quando a mente muda de estado e aplica os efeitos da sanidade baixa. */
    private static void aplicarEstadoMental(ServerPlayer j, Personagem p) {
        Personagem.Estado mudou = p.verificarMudancaDeEstado();
        if (mudou != null) {
            String aviso = switch (mudou) {
                case ESTAVEL -> "Sua mente se acalma.";
                case ABALADO -> "Sua mente vacila...";
                case ENLOUQUECENDO -> "Você está enlouquecendo!";
            };
            ChatFormatting corAviso = switch (mudou) {
                case ESTAVEL -> ChatFormatting.GREEN;
                case ABALADO -> ChatFormatting.YELLOW;
                case ENLOUQUECENDO -> ChatFormatting.RED;
            };
            Avisos.enviar(j, aviso, corAviso);
        }
        Personagem.Estado estado = p.estadoMental();
        if (estado == Personagem.Estado.ESTAVEL) {
            return;
        }
        efeito(j, MobEffects.DARKNESS, 80);
        if (estado == Personagem.Estado.ENLOUQUECENDO) {
            efeito(j, MobEffects.WEAKNESS, 80);
            efeito(j, MobEffects.SLOWNESS, 80);
            if ((j.tickCount / 20) % 10 == 0) { // enjoo so de vez em quando
                efeito(j, MobEffects.NAUSEA, 100);
            }
        }
    }

    private static void efeito(ServerPlayer j, Holder<MobEffect> efeito, int duracao) {
        aplicandoEfeitoProprio = true;
        try {
            j.addEffect(new MobEffectInstance(efeito, duracao, 0, false, false, true));
        } finally {
            aplicandoEfeitoProprio = false;
        }
    }
}
