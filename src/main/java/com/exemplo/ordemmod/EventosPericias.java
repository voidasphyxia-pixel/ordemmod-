package com.exemplo.ordemmod;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Onde as perícias viram jogo. Cada perícia encontra um gancho no Minecraft:
 *
 *  Acrobacia    → rola ao cair e reduz o dano da queda (chamado por EventosDano, que ja altera o dano)
 *  Furtividade  → agachado, monstros podem "não te notar"; o primeiro golpe vira ataque furtivo
 *  Iniciativa   → quando um monstro te percebe, você pode agir primeiro (Velocidade)
 *  Reflexos     → teste de esquiva contra todo ataque (DT 20, sobe com vários inimigos): passou, não leva o golpe
 *  Atletismo    → Veterano ganha Salto, Expert ganha Graça do Golfinho
 *  Percepção    → a cada 10 s, sente monstros por perto (Brilho)
 *  Sobrevivência→ comida rende mais saturação
 *  Medicina     → papel vira atadura (clique direito em alguém, ou agachado em si mesmo)
 *  Crime        → pepita de ferro vira gazua para portas de ferro
 *  Adestramento → bichos domados ganham vida extra
 *  Atualidades  → mais XP
 *  Luta/Pontaria→ dano e crítico (em Personagem / EventosDano)
 *  Vontade/Ocultismo/Fortitude → sanidade e resistência a efeitos (em Personagem / EventosDano)
 *
 * No Forge eram metodos @SubscribeEvent; no Fabric (etapa 4d) ficam assim:
 *  - LivingAttackEvent (Reflexos)        -> ServerLivingEntityEvents.ALLOW_DAMAGE (em {@link #registrar()})
 *  - PlayerTickEvent                      -> ServerTickEvents.END_SERVER_TICK
 *  - PlayerInteractEvent.*                -> UseEntityCallback / UseItemCallback / UseBlockCallback
 *  - LivingHurtEvent (adestramento, intimidacao, furtivo) -> {@link #aoCausarDano}, chamado por EventosDano
 *  - LivingFallEvent                      -> {@link #aoCair}, chamado por EventosDano (dano do tipo FALL)
 *  - LivingChangeTargetEvent              -> {@link #aoMirar}, chamado por mixin/MobMixin
 *  - LivingEntityUseItemEvent.Finish      -> {@link #aoComer}, chamado por mixin/LivingEntityMixin
 *  - PlayerXpEvent.XpChange               -> {@link #ajustarXp}, chamado por mixin/PlayerMixin
 *  - AnimalTameEvent                      -> {@link #aoDomar}, chamado por mixin/TamableAnimalMixin
 *
 * Comandos: /ordem pericias | beneficios [pericia] | treinar <pericia> | grau <pericia> | testar <pericia> [dt]
 */
public final class EventosPericias {
    private static final Random ALEATORIO = new Random();
    private static final Identifier ID_ADESTRAMENTO =
            Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "adestramento");
    private static final List<String> IDS = Arrays.stream(Pericia.values()).map(Pericia::getId).toList();

    private EventosPericias() {
    }

    /** Liga os callbacks do Fabric. Chamar uma vez, depois de EventosOrdem.registrar(). */
    public static void registrar() {
        // Reflexos: roda antes de o dano existir; false cancela o ataque (sem dano, sem tranco)
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(EventosPericias::permitirDano);

        ServerTickEvents.END_SERVER_TICK.register(servidor -> {
            for (ServerPlayer jogador : List.copyOf(servidor.getPlayerList().getPlayers())) {
                aoTick(jogador);
            }
        });

        // Medicina em outra criatura (era PlayerInteractEvent.EntityInteract)
        UseEntityCallback.EVENT.register((jogador, nivel, mao, alvo, hit) -> {
            if (mao != InteractionHand.MAIN_HAND || !(jogador instanceof ServerPlayer j)
                    || !(alvo instanceof LivingEntity vivo)) {
                return InteractionResult.PASS;
            }
            ItemStack item = j.getItemInHand(mao);
            if (!item.is(Items.PAPER) || vivo.getHealth() >= vivo.getMaxHealth()) {
                return InteractionResult.PASS;
            }
            primeirosSocorros(j, vivo, item);
            return InteractionResult.SUCCESS;
        });

        // Medicina em si mesmo, agachado (era PlayerInteractEvent.RightClickItem)
        UseItemCallback.EVENT.register((jogador, nivel, mao) -> {
            if (mao != InteractionHand.MAIN_HAND || !(jogador instanceof ServerPlayer j) || !j.isCrouching()) {
                return InteractionResult.PASS;
            }
            ItemStack item = j.getItemInHand(mao);
            if (!item.is(Items.PAPER) || j.getHealth() >= j.getMaxHealth()) {
                return InteractionResult.PASS;
            }
            primeirosSocorros(j, j, item);
            return InteractionResult.SUCCESS;
        });

        // Crime: gazua em porta de ferro (era PlayerInteractEvent.RightClickBlock)
        UseBlockCallback.EVENT.register((jogador, nivel, mao, hit) -> {
            if (mao != InteractionHand.MAIN_HAND || !(jogador instanceof ServerPlayer j)) {
                return InteractionResult.PASS;
            }
            ItemStack item = j.getItemInHand(mao);
            if (!item.is(Items.IRON_NUGGET)) {
                return InteractionResult.PASS;
            }
            BlockPos posicao = hit.getBlockPos();
            BlockState estado = nivel.getBlockState(posicao);
            if (!estado.is(Blocks.IRON_DOOR)) {
                return InteractionResult.PASS;
            }
            arrombar(j, nivel, estado, posicao, item);
            return InteractionResult.SUCCESS;
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, acessoRegistros, ambiente) ->
                registrarComandos(dispatcher));
    }

    private static Personagem de(ServerPlayer j) {
        return EventosOrdem.de(j);
    }

    private static void dizer(ServerPlayer j, String texto, ChatFormatting cor) {
        Avisos.enviar(j, texto, cor);
    }

    /** Versão curta de um teste, para o aviso embaixo da mira (o comando continua mostrando tudo no chat). */
    private static String curto(ResultadoTeste r) {
        String veredito = r.critico() ? "CRÍTICO!" : r.sucesso() ? "SUCESSO" : "FALHA";
        return r.pericia().getNome() + " " + r.total() + "/" + r.dt() + " · " + veredito;
    }

    private static void avisarTeste(ServerPlayer j, ResultadoTeste r) {
        dizer(j, curto(r), r.critico() ? ChatFormatting.GOLD : r.sucesso() ? ChatFormatting.GREEN : ChatFormatting.RED);
    }

    private static Component formatar(ResultadoTeste r) {
        ChatFormatting cor = r.critico() ? ChatFormatting.GOLD : r.sucesso() ? ChatFormatting.GREEN : ChatFormatting.RED;
        return Component.literal(r.toString()).withStyle(cor);
    }

    private static void faiscas(ServerPlayer j, ParticleOptions tipo, int quantidade) {
        if (j.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(tipo, j.getX(), j.getY(0.5), j.getZ(), quantidade, 0.4, 0.4, 0.4, 0.05);
        }
    }

    // ------------------------------------------------------------------ Acrobacia

    /**
     * Chamado por EventosDano quando o jogador vai levar dano de queda (DamageTypes.FALL). Devolve o dano novo.
     * No Forge era o LivingFallEvent (distancia >= 4); aqui o gatilho e o dano ja calculado (>= 1, ou seja, queda
     * acima de 3 blocos). A reducao e aplicada no dano final, nao no multiplicador antes do arredondamento.
     */
    static float aoCair(ServerPlayer j, Personagem p, float dano) {
        if (dano < 1.0F || j.isCreative() || j.isSpectator()) {
            return dano;
        }
        // o patamar de Acrobacia e a origem Atleta já reduzem o dano sozinhos (passivos)
        float multiplicador = (float) (1.0 - p.pericias.valor(Beneficio.ACROBACIA_QUEDA))
                * (float) (1.0 - p.origem(Origem.Efeito.REDUCAO_QUEDA));
        // o teste: total 15+ reduz o dano (20%, 40%, 60%... de 5 em 5) e 20 natural zera (veja QuedaAcrobacia)
        ResultadoTeste r = p.pericias.testar(Pericia.ACROBACIA, QuedaAcrobacia.MINIMO, ALEATORIO);
        multiplicador *= (float) (1.0 - QuedaAcrobacia.reducao(r));
        RolagemNaHud.mostrar(j, r, QuedaAcrobacia.reducaoPercentual(r), "Queda"); // só estética: o servidor já decidiu
        if (QuedaAcrobacia.rola(r)) {
            faiscas(j, ParticleTypes.CLOUD, 12);
            EsquivaVisual.rolar(j);
        }
        return dano * multiplicador;
    }

    // ------------------------------------------------------------------ Furtividade e Iniciativa

    /**
     * Um monstro vai te escolher como alvo (chamado por mixin/MobMixin no inicio de Mob.setTarget).
     * Agachado você pode passar despercebido; senão, pode agir primeiro.
     *
     * @return true para CANCELAR a mudanca de alvo (o monstro nao te notou)
     */
    public static boolean aoMirar(Mob mob, LivingEntity novoAlvo) {
        if (!(novoAlvo instanceof ServerPlayer j) || !(mob instanceof Enemy) || j.isCreative() || j.isSpectator()) {
            return false;
        }
        Personagem p = de(j);
        long agora = mob.level().getGameTime();
        Map<UUID, Long> furtivo = mob.getAttachedOrCreate(AnexosOrdem.FURTIVO);
        UUID chave = j.getUUID();

        if (agora < furtivo.getOrDefault(chave, 0L)) { // ainda não te notou
            return true;
        }
        double desvio = p.pericias.valor(Beneficio.ENGANACAO_DESVIO) + p.origem(Origem.Efeito.ENGANACAO_DESVIO); // Enganação: o monstro se engana sozinho
        if (desvio > 0 && ALEATORIO.nextDouble() < desvio) {
            furtivo.put(chave, agora + 60); // 3 segundos sem te notar
            dizer(j, "Enganação: " + mob.getDisplayName().getString() + " te perdeu de vista.", ChatFormatting.GRAY);
            return true;
        }
        if (j.isCrouching()) {
            int luz = j.level().getMaxLocalRawBrightness(j.blockPosition());
            int extra = (luz >= 12 ? 5 : 0) + (mob.distanceTo(j) < 6.0F ? 5 : 0); // claro e perto = mais difícil
            ResultadoTeste r = p.pericias.testar(Pericia.FURTIVIDADE, Pericias.DT_MEDIA + extra, ALEATORIO);
            if (r.sucesso()) {
                double tempo = p.pericias.valor(Beneficio.FURTIVIDADE_TEMPO); // 5, 7 ou 10 s (5 se destreinada)
                furtivo.put(chave, agora + (long) (20 * (tempo > 0 ? tempo : 5)));
                dizer(j, "Furtividade: " + mob.getDisplayName().getString() + " não te notou.", ChatFormatting.GRAY);
                return true;
            }
        }
        double recarga = p.pericias.valor(Beneficio.INICIATIVA_RECARGA); // 30, 20 ou 10 s (30 se destreinada)
        if (agora >= j.getAttachedOrElse(AnexosOrdem.INICIATIVA_ATE, 0L)) {
            j.setAttached(AnexosOrdem.INICIATIVA_ATE, agora + (long) (20 * (recarga > 0 ? recarga : 30)));
            ResultadoTeste r = p.pericias.testar(Pericia.INICIATIVA, Pericias.DT_MEDIA, ALEATORIO);
            if (r.sucesso()) {
                int duracao = 120 + p.pericias.bonus(Pericia.INICIATIVA) * 8;
                int nivel = Math.max(1, (int) p.pericias.valor(Beneficio.INICIATIVA_NIVEL)); // Expert = Velocidade II
                j.addEffect(new MobEffectInstance(MobEffects.SPEED, duracao, nivel - 1, false, false, true));
                dizer(j, "Iniciativa: você agiu primeiro!", ChatFormatting.YELLOW);
            }
        }
        return false;
    }

    /**
     * Chamado por EventosDano no fim do calculo do dano (era o LivingHurtEvent de prioridade LOW).
     * Bichos domados batem mais forte; Intimidação assusta; bater num monstro que não te notou é ataque furtivo
     * (dano extra) e, de qualquer forma, quebra a furtividade. Devolve o dano novo.
     */
    static float aoCausarDano(LivingEntity alvo, DamageSource fonte, float quantidade) {
        // bichos domados causam mais dano com o Adestramento do dono
        if (fonte.getEntity() instanceof TamableAnimal bicho && bicho.getOwner() instanceof ServerPlayer dono) {
            Personagem pd = de(dono);
            double extra = pd.pericias.valor(Beneficio.ADESTRAMENTO_DANO) + pd.origem(Origem.Efeito.DANO_BICHOS);
            return extra > 0 ? (float) (quantidade * (1.0 + extra)) : quantidade;
        }
        if (!(fonte.getEntity() instanceof ServerPlayer atacante)) {
            return quantidade;
        }
        float resultado = quantidade;
        Personagem p = de(atacante);

        // Intimidação: chance de o monstro ficar com medo (Fraqueza por 5 s)
        double medo = p.pericias.valor(Beneficio.INTIMIDACAO_FRAQUEZA);
        if (medo > 0 && alvo instanceof Enemy && ALEATORIO.nextDouble() < medo) {
            alvo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true, true));
        }

        Map<UUID, Long> furtivo = alvo.getAttached(AnexosOrdem.FURTIVO);
        UUID chave = atacante.getUUID();
        if (furtivo == null || !furtivo.containsKey(chave)) {
            return resultado;
        }
        if (furtivo.get(chave) > alvo.level().getGameTime()) {
            // 2 de dano fixo + % do golpe (Furtividade: +50%, +100% ou +150%)
            float extra = 2.0F + resultado * (float) p.pericias.valor(Beneficio.FURTIVIDADE_DANO);
            resultado += extra;
            dizer(atacante, "Ataque furtivo! +" + Math.round(extra) + " de dano", ChatFormatting.GOLD);
        }
        furtivo.put(chave, 0L);
        return resultado;
    }

    // ------------------------------------------------------------------ Reflexos

    /**
     * Todo ataque que vai atingir o jogador faz um teste de Reflexos (DT {@link EsquivaReflexos#DT_PADRAO}, mais com vários inimigos).
     * Passou: o ataque é cancelado (sem dano, sem tranco) e o personagem dá um passo de esquiva (provisorio: EsquivaVisual).
     * Roda no ALLOW_DAMAGE, antes de o dano existir, então a esquiva também poupa a sanidade e os efeitos do golpe.
     *
     * @return false para cancelar o ataque
     */
    private static boolean permitirDano(LivingEntity entidade, DamageSource fonte, float quantidade) {
        if (!(entidade instanceof ServerPlayer j) || j.isCreative() || j.isSpectator() || !j.isAlive()) {
            return true;
        }
        if (fonte.getEntity() == null || fonte.getEntity() == j || quantidade <= 0
                || fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return true;
        }
        if (j.invulnerableTime > 10) { // ainda nos quadros de invulnerabilidade do golpe anterior: não é um ataque novo
            return true;
        }
        Personagem p = de(j);
        if (!p.isCriado()) {
            return true;
        }
        // quanto mais inimigos mirando em você (num raio curto), mais difícil esquivar de cada golpe
        int cercando = j.level().getEntitiesOfClass(Mob.class, j.getBoundingBox().inflate(EsquivaReflexos.RAIO_CERCO),
                m -> m instanceof Enemy && m.isAlive() && m.getTarget() == j).size();
        ResultadoTeste r = p.pericias.testar(Pericia.REFLEXOS, EsquivaReflexos.dt(cercando), ALEATORIO);
        RolagemNaHud.mostrar(j, r, -1, "Esquiva"); // só estética: o servidor já decidiu
        if (r.sucesso()) {
            EsquivaVisual.esquivar(j, fonte);
            return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ passivas por segundo

    private static void aoTick(ServerPlayer j) {
        if (j.isSpectator() || j.tickCount % 20 != 0) {
            return;
        }
        Personagem p = de(j);
        GrauTreinamento atletismo = p.pericias.getGrau(Pericia.ATLETISMO);
        if (atletismo.pelomenos(GrauTreinamento.VETERANO)) {
            j.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 0, true, false, false));
        }
        if (atletismo == GrauTreinamento.EXPERT) {
            j.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 60, 0, true, false, false));
        }
        if (j.tickCount % 200 == 0 && p.pericias.estaTreinada(Pericia.PERCEPCAO)) {
            pulsoDePercepcao(j, p);
        }
    }

    /** A cada 10 segundos: se há monstros por perto e o teste passa, eles brilham por 3 segundos. */
    private static void pulsoDePercepcao(ServerPlayer j, Personagem p) {
        double raio = 10 + p.pericias.valor(Beneficio.PERCEPCAO_RAIO) + p.origem(Origem.Efeito.PERCEPCAO_RAIO);
        List<LivingEntity> perto = j.level().getEntitiesOfClass(LivingEntity.class,
                j.getBoundingBox().inflate(raio), m -> m instanceof Enemy && m.isAlive());
        if (perto.isEmpty()) {
            return;
        }
        ResultadoTeste r = p.pericias.testar(Pericia.PERCEPCAO, Pericias.DT_MEDIA, ALEATORIO);
        if (!r.sucesso()) {
            return;
        }
        int brilho = (int) (20 * p.pericias.valor(Beneficio.PERCEPCAO_BRILHO)); // 3, 5 ou 8 s
        for (LivingEntity m : perto) {
            m.addEffect(new MobEffectInstance(MobEffects.GLOWING, brilho, 0, false, false, false));
        }
        dizer(j, "Percepção: " + perto.size() + (perto.size() == 1 ? " presença" : " presenças")
                + " por perto", ChatFormatting.AQUA);
    }

    // ------------------------------------------------------------------ Sobrevivência

    /**
     * Chamado por mixin/LivingEntityMixin quando o jogador termina de usar um item (completeUsingItem).
     * Se o item e comida, Sobrevivencia e a origem Chef dao fome e saturacao extras.
     */
    static void aoComer(ServerPlayer j, ItemStack item) {
        if (!item.has(DataComponents.FOOD)) {
            return;
        }
        Personagem p = de(j);
        int fome = (int) (p.pericias.valor(Beneficio.SOBREVIVENCIA_FOME) + p.origem(Origem.Efeito.COMIDA_FOME));
        float saturacaoExtra = (float) (p.pericias.valor(Beneficio.SOBREVIVENCIA_SATURACAO)
                + p.origem(Origem.Efeito.COMIDA_SATURACAO)); // Sobrevivência e Chef
        if (fome == 0 && saturacaoExtra == 0) {
            return;
        }
        FoodData comida = j.getFoodData();
        comida.setFoodLevel(Math.min(20, comida.getFoodLevel() + fome));
        comida.setSaturation(Math.min(comida.getSaturationLevel() + saturacaoExtra, (float) comida.getFoodLevel()));
    }

    // ------------------------------------------------------------------ Medicina (papel = atadura)

    private static void primeirosSocorros(ServerPlayer j, LivingEntity alvo, ItemStack papel) {
        if (j.getCooldowns().isOnCooldown(papel)) {
            dizer(j, "Curativo em recarga.", ChatFormatting.GRAY);
            return;
        }
        Personagem p = de(j);
        ResultadoTeste r = p.pericias.testar(Pericia.MEDICINA, Pericias.DT_MEDIA, ALEATORIO);
        avisarTeste(j, r);
        double recarga = p.pericias.valor(Beneficio.MEDICINA_RECARGA); // 5, 4 ou 3 s (5 se destreinada)
        // a recarga vem ANTES do shrink: no 26.2 ela e por item (stack), e uma pilha vazia nao serve de chave
        j.getCooldowns().addCooldown(papel, (int) (20 * (recarga > 0 ? recarga : 5)));
        if (!j.isCreative()) {
            papel.shrink(1);
        }
        if (!r.sucesso()) {
            dizer(j, "O curativo não ficou bom.", ChatFormatting.GRAY);
            return;
        }
        float cura = (float) p.pericias.valor(Beneficio.MEDICINA_CURA); // 4, 6 ou 8 de vida
        if (cura <= 0) {
            cura = 2.0F; // sem treino, o curativo ainda ajuda um pouco
        }
        // Agente de Saúde: + Intelecto de vida extra
        cura += (float) (p.origem(Origem.Efeito.CURA_POR_INTELECTO) * p.atributos.get(Atributo.INTELECTO));
        if (r.critico()) {
            cura *= 1.5F;
        }
        alvo.heal(cura);
        GrauTreinamento grau = p.pericias.getGrau(Pericia.MEDICINA);
        if (grau.pelomenos(GrauTreinamento.VETERANO)) { // Veterano: estanca veneno
            alvo.removeEffect(MobEffects.POISON);
        }
        if (grau == GrauTreinamento.EXPERT) { // Expert: também limpa definhamento e fraqueza
            alvo.removeEffect(MobEffects.WITHER);
            alvo.removeEffect(MobEffects.WEAKNESS);
        }
        if (alvo.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.HEART, alvo.getX(), alvo.getY(1.0), alvo.getZ(), 6, 0.3, 0.3, 0.3, 0.02);
        }
    }

    // ------------------------------------------------------------------ Crime (pepita de ferro = gazua)

    private static void arrombar(ServerPlayer j, Level nivel, BlockState estado, BlockPos posicao, ItemStack pepita) {
        Personagem p = de(j);
        if (!p.pericias.estaTreinada(Pericia.CRIME)) {
            dizer(j, "Requer Crime treinada.", ChatFormatting.GRAY);
            return;
        }
        ResultadoTeste r = p.pericias.testar(Pericia.CRIME, Pericias.DT_MEDIA, ALEATORIO);
        avisarTeste(j, r);
        if (r.sucesso()) {
            ((DoorBlock) estado.getBlock()).setOpen(j, nivel, estado, posicao, !estado.getValue(DoorBlock.OPEN));
        } else if (ALEATORIO.nextDouble() < p.pericias.valor(Beneficio.CRIME_GAZUA)) {
            dizer(j, "A gazua escorregou.", ChatFormatting.GRAY);
        } else {
            if (!j.isCreative()) {
                pepita.shrink(1);
            }
            // na 1.21.5+ ITEM_BREAK e um Holder: o som em si vem de .value()
            j.level().playSound(null, j.blockPosition(), SoundEvents.ITEM_BREAK.value(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
            dizer(j, "A gazua quebrou.", ChatFormatting.GRAY);
        }
    }

    // ------------------------------------------------------------------ Atualidades (XP)

    /**
     * Chamado por mixin/PlayerMixin no inicio de Player.giveExperiencePoints (era PlayerXpEvent.XpChange).
     * Devolve a quantidade de pontos ja com o bonus de Atualidades e da origem.
     */
    static int ajustarXp(Player jogador, int pontos) {
        if (!(jogador instanceof ServerPlayer j) || pontos <= 0) {
            return pontos;
        }
        Personagem px = de(j);
        double extra = px.pericias.valor(Beneficio.ATUALIDADES_XP) + px.origem(Origem.Efeito.XP);
        return extra > 0 ? (int) Math.ceil(pontos * (1.0 + extra)) : pontos;
    }

    // ------------------------------------------------------------------ Adestramento

    /**
     * Chamado por mixin/TamableAnimalMixin no fim de TamableAnimal.tame (era AnimalTameEvent).
     * LIMITE: so cobre lobos, gatos e papagaios (TamableAnimal). Cavalos e afins (AbstractHorse) nao passam por aqui.
     */
    static void aoDomar(TamableAnimal bicho, Player jogador) {
        if (!(jogador instanceof ServerPlayer j)) {
            return;
        }
        Personagem dono = de(j);
        double bonus = dono.pericias.valor(Beneficio.ADESTRAMENTO_VIDA)
                + dono.origem(Origem.Efeito.ADESTRAMENTO_VIDA); // 4, 8 ou 12 de vida (+4 do Trabalhador Rural)
        AttributeInstance vida = bicho.getAttribute(Attributes.MAX_HEALTH);
        if (bonus == 0 || vida == null || vida.getModifier(ID_ADESTRAMENTO) != null) {
            return;
        }
        vida.addPermanentModifier(new AttributeModifier(ID_ADESTRAMENTO, bonus, AttributeModifier.Operation.ADD_VALUE));
        dizer(j, "Adestramento: companheiro mais forte!", ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ comandos

    private static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("ordem");

        raiz.then(Commands.literal("pericias").executes(EventosPericias::listar));

        raiz.then(Commands.literal("beneficios")
                .executes(ctx -> beneficios(ctx, null))
                .then(Commands.argument("pericia", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(IDS, b))
                        .executes(ctx -> beneficios(ctx, StringArgumentType.getString(ctx, "pericia")))));

        raiz.then(Commands.literal("treinar").then(Commands.argument("pericia", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(IDS, b))
                .executes(ctx -> treinar(ctx, StringArgumentType.getString(ctx, "pericia")))));

        raiz.then(Commands.literal("grau").then(Commands.argument("pericia", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(IDS, b))
                .executes(ctx -> melhorar(ctx, StringArgumentType.getString(ctx, "pericia")))));

        raiz.then(Commands.literal("testar").then(Commands.argument("pericia", StringArgumentType.word())
                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(IDS, b))
                .executes(ctx -> testar(ctx, StringArgumentType.getString(ctx, "pericia"), Pericias.DT_MEDIA))
                .then(Commands.argument("dt", IntegerArgumentType.integer(1, 60))
                        .executes(ctx -> testar(ctx, StringArgumentType.getString(ctx, "pericia"),
                                IntegerArgumentType.getInteger(ctx, "dt"))))));

        dispatcher.register(raiz); // junta com o /ordem que já existe em EventosOrdem
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, Component texto) {
        ctx.getSource().sendSuccess(() -> texto, false);
        return 1;
    }

    private static int erro(CommandContext<CommandSourceStack> ctx, String texto) {
        ctx.getSource().sendFailure(Component.literal(texto));
        return 0;
    }

    private static int listar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Personagem p = de(ctx.getSource().getPlayerOrException());
        StringBuilder sb = new StringBuilder();
        sb.append("Vagas de treino: ").append(p.pericias.vagasRestantes()).append(" livres (")
                .append(p.pericias.treinadas()).append('/').append(p.pericias.vagasDeTreino()).append(")")
                .append(" | Melhorias: ").append(p.pericias.getMelhoriasVeterano()).append(" p/ Veterano, ")
                .append(p.pericias.getMelhoriasExpert()).append(" p/ Expert");
        for (Pericia pericia : Pericia.values()) {
            GrauTreinamento g = p.pericias.getGrau(pericia);
            if (g != GrauTreinamento.DESTREINADO) {
                sb.append("\n").append(pericia.getNome()).append(" (").append(pericia.getAtributo().getSigla())
                        .append(") ").append(g.getNome()).append(" +").append(g.getBonus());
            }
        }
        if (p.pericias.treinadas() == 0) {
            sb.append("\nNenhuma treinada ainda. Use /ordem treinar <pericia>.");
        }
        return ok(ctx, Component.literal(sb.toString()));
    }

    /** /ordem beneficios [pericia]: mostra o que cada patamar dá (sem argumento, só das perícias treinadas). */
    private static int beneficios(CommandContext<CommandSourceStack> ctx, String nome) throws CommandSyntaxException {
        Personagem p = de(ctx.getSource().getPlayerOrException());
        StringBuilder sb = new StringBuilder();
        if (nome != null) {
            Pericia pericia = Pericia.porTexto(nome);
            if (pericia == null) {
                return erro(ctx, "Perícia não encontrada: " + nome);
            }
            sb.append(pericia.getNome()).append(" (").append(p.pericias.getGrau(pericia).getNome()).append(")");
            List<Beneficio> lista = Beneficio.de(pericia);
            if (lista.isEmpty()) {
                sb.append("\nAinda sem efeito de jogo além do bônus nos testes.");
            }
            for (Beneficio b : lista) {
                sb.append("\n").append(b.linha());
            }
        } else {
            sb.append("Benefícios ativos (T/V/E = Treinado/Veterano/Expert):");
            boolean algum = false;
            for (Pericia pericia : Pericia.values()) {
                GrauTreinamento g = p.pericias.getGrau(pericia);
                for (Beneficio b : Beneficio.de(pericia)) {
                    if (g != GrauTreinamento.DESTREINADO) {
                        sb.append("\n").append(pericia.getNome()).append(" - ").append(b.getDescricao())
                                .append(": ").append(b.formatar(g));
                        algum = true;
                    }
                }
            }
            if (!algum) {
                sb.append("\nNenhum ainda. Treine perícias com /ordem treinar <pericia>.");
            }
        }
        return ok(ctx, Component.literal(sb.toString()));
    }

    private static int treinar(CommandContext<CommandSourceStack> ctx, String nome) throws CommandSyntaxException {
        ServerPlayer j = ctx.getSource().getPlayerOrException();
        Personagem p = de(j);
        Pericia pericia = Pericia.porTexto(nome);
        if (pericia == null) {
            return erro(ctx, "Perícia não encontrada: " + nome);
        }
        if (!p.pericias.treinar(pericia)) {
            return erro(ctx, "Não dá para treinar " + pericia.getNome()
                    + " (já treinada ou sem vagas: " + p.pericias.vagasRestantes() + ").");
        }
        EventosOrdem.aplicar(j, p);
        SalvamentoOrdem.salvar(j, p);
        return ok(ctx, Component.literal(pericia.getNome() + " agora é Treinado (+5). Vagas livres: "
                + p.pericias.vagasRestantes()));
    }

    private static int melhorar(CommandContext<CommandSourceStack> ctx, String nome) throws CommandSyntaxException {
        ServerPlayer j = ctx.getSource().getPlayerOrException();
        Personagem p = de(j);
        Pericia pericia = Pericia.porTexto(nome);
        if (pericia == null) {
            return erro(ctx, "Perícia não encontrada: " + nome);
        }
        if (!p.pericias.melhorar(pericia)) {
            return erro(ctx, "Não dá para melhorar " + pericia.getNome()
                    + ": precisa ser Treinado (para Veterano, NEX 35%) ou Veterano (para Expert, NEX 70%), "
                    + "e ter melhoria sobrando.");
        }
        EventosOrdem.aplicar(j, p);
        SalvamentoOrdem.salvar(j, p);
        GrauTreinamento g = p.pericias.getGrau(pericia);
        return ok(ctx, Component.literal(pericia.getNome() + " agora é " + g.getNome() + " (+" + g.getBonus() + ")."));
    }

    private static int testar(CommandContext<CommandSourceStack> ctx, String nome, int dt) throws CommandSyntaxException {
        Personagem p = de(ctx.getSource().getPlayerOrException());
        Pericia pericia = Pericia.porTexto(nome);
        if (pericia == null) {
            return erro(ctx, "Perícia não encontrada: " + nome);
        }
        return ok(ctx, formatar(p.pericias.testar(pericia, dt, ALEATORIO)));
    }
}
