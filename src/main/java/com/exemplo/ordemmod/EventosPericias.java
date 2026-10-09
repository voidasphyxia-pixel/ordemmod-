package com.exemplo.ordemmod;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.AnimalTameEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Onde as perícias viram jogo. Cada perícia encontra um gancho no Minecraft:
 *
 *  Acrobacia    → rola ao cair e reduz o dano da queda
 *  Furtividade  → agachado, monstros podem "não te notar"; o primeiro golpe vira ataque furtivo
 *  Iniciativa   → quando um monstro te percebe, você pode agir primeiro (Velocidade)
 *  Reflexos     → teste de esquiva contra todo ataque (DT 20, sobe com vários inimigos): passou, não leva o golpe
 *  Atletismo    → Veterano ganha Salto, Expert ganha Graça do Golfinho
 *  Percepção    → a cada 10 s, sente monstros por perto (Brilho)
 *  Sobrevivência→ comida rende mais saturação
 *  Medicina     → papel vira atadura (clique direito em alguém, ou agachado em si mesmo)
 *  Crime        → pepita de ferro vira gazua para portas de ferro
 *  Adestramento → bichos domados ganham vida extra
 *  Luta/Pontaria→ dano e crítico (em Personagem / EventosOrdem)
 *  Vontade/Ocultismo/Fortitude → sanidade e resistência a efeitos (em Personagem / EventosOrdem)
 *
 * Comandos: /ordem pericias | treinar <pericia> | grau <pericia> | testar <pericia> [dt]
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID)
public class EventosPericias {
    private static final Random ALEATORIO = new Random();
    private static final UUID ID_ADESTRAMENTO = UUID.fromString("5d0c6f4e-2b7a-4f0e-8c1d-3a9b7e2f1c64");
    private static final String CHAVE_FURTIVO = "ordem_furt_";
    private static final String CHAVE_INICIATIVA = "ordem_ini_ate";
    private static final List<String> IDS = Arrays.stream(Pericia.values()).map(Pericia::getId).toList();

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

    private static void faiscas(ServerPlayer j, net.minecraft.core.particles.ParticleOptions tipo, int quantidade) {
        if (j.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(tipo, j.getX(), j.getY(0.5), j.getZ(), quantidade, 0.4, 0.4, 0.4, 0.05);
        }
    }

    // ------------------------------------------------------------------ Acrobacia

    @SubscribeEvent
    public static void aoCair(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer j) || e.getDistance() < 4.0F || j.isCreative() || j.isSpectator()) {
            return;
        }
        Personagem p = de(j);
        if (!p.isCriado()) {
            return;
        }
        // o patamar de Acrobacia e a origem Atleta já reduzem o dano sozinhos (passivos)
        float multiplicador = e.getDamageMultiplier() * (float) (1.0 - p.pericias.valor(Beneficio.ACROBACIA_QUEDA))
                * (float) (1.0 - p.origem(Origem.Efeito.REDUCAO_QUEDA));
        // o teste: total 15+ reduz o dano (20%, 40%, 60%... de 5 em 5) e 20 natural zera (veja QuedaAcrobacia)
        ResultadoTeste r = p.pericias.testar(Pericia.ACROBACIA, QuedaAcrobacia.MINIMO, ALEATORIO);
        multiplicador *= (float) (1.0 - QuedaAcrobacia.reducao(r));
        e.setDamageMultiplier(multiplicador);
        RolagemNaHud.mostrar(j, r, QuedaAcrobacia.reducaoPercentual(r), "Queda"); // só estética: o servidor já decidiu
        if (QuedaAcrobacia.rola(r)) {
            faiscas(j, ParticleTypes.CLOUD, 12);
            RolamentoEpicFight.rolar(j);
        }
    }

    // ------------------------------------------------------------------ Furtividade e Iniciativa

    /** Um monstro vai te escolher como alvo: agachado você pode passar despercebido; senão, pode agir primeiro. */
    @SubscribeEvent
    public static void aoMirar(LivingChangeTargetEvent e) {
        if (!(e.getNewTarget() instanceof ServerPlayer j) || !(e.getEntity() instanceof Mob mob)
                || !(mob instanceof Enemy) || j.isCreative() || j.isSpectator()) {
            return;
        }
        Personagem p = de(j);
        long agora = mob.level().getGameTime();
        CompoundTag dados = mob.getPersistentData();
        String chave = CHAVE_FURTIVO + j.getUUID();

        if (agora < dados.getLong(chave)) { // ainda não te notou
            e.setCanceled(true);
            return;
        }
        double desvio = p.pericias.valor(Beneficio.ENGANACAO_DESVIO) + p.origem(Origem.Efeito.ENGANACAO_DESVIO); // Enganação: o monstro se engana sozinho
        if (desvio > 0 && ALEATORIO.nextDouble() < desvio) {
            dados.putLong(chave, agora + 60); // 3 segundos sem te notar
            e.setCanceled(true);
            dizer(j, "Enganação: " + mob.getDisplayName().getString() + " te perdeu de vista.", ChatFormatting.GRAY);
            return;
        }
        if (j.isCrouching()) {
            int luz = j.level().getMaxLocalRawBrightness(j.blockPosition());
            int extra = (luz >= 12 ? 5 : 0) + (mob.distanceTo(j) < 6.0F ? 5 : 0); // claro e perto = mais difícil
            ResultadoTeste r = p.pericias.testar(Pericia.FURTIVIDADE, Pericias.DT_MEDIA + extra, ALEATORIO);
            if (r.sucesso()) {
                double tempo = p.pericias.valor(Beneficio.FURTIVIDADE_TEMPO); // 5, 7 ou 10 s (5 se destreinada)
                dados.putLong(chave, agora + (long) (20 * (tempo > 0 ? tempo : 5)));
                e.setCanceled(true);
                dizer(j, "Furtividade: " + mob.getDisplayName().getString() + " não te notou.", ChatFormatting.GRAY);
                return;
            }
        }
        double recarga = p.pericias.valor(Beneficio.INICIATIVA_RECARGA); // 30, 20 ou 10 s (30 se destreinada)
        if (agora >= j.getPersistentData().getLong(CHAVE_INICIATIVA)) {
            j.getPersistentData().putLong(CHAVE_INICIATIVA, agora + (long) (20 * (recarga > 0 ? recarga : 30)));
            ResultadoTeste r = p.pericias.testar(Pericia.INICIATIVA, Pericias.DT_MEDIA, ALEATORIO);
            if (r.sucesso()) {
                int duracao = 120 + p.pericias.bonus(Pericia.INICIATIVA) * 8;
                int nivel = Math.max(1, (int) p.pericias.valor(Beneficio.INICIATIVA_NIVEL)); // Expert = Velocidade II
                j.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, duracao, nivel - 1, false, false, true));
                dizer(j, "Iniciativa: você agiu primeiro!", ChatFormatting.YELLOW);
            }
        }
    }

    /** Bater num monstro que não te notou é ataque furtivo (dano extra); de qualquer forma, quebra a furtividade. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void aoAtacar(LivingHurtEvent e) {
        // bichos domados causam mais dano com o Adestramento do dono
        if (e.getSource().getEntity() instanceof TamableAnimal bicho && bicho.getOwner() instanceof ServerPlayer dono) {
            Personagem pd = de(dono);
            double extra = pd.pericias.valor(Beneficio.ADESTRAMENTO_DANO) + pd.origem(Origem.Efeito.DANO_BICHOS);
            if (extra > 0) {
                e.setAmount((float) (e.getAmount() * (1.0 + extra)));
            }
            return;
        }
        if (!(e.getSource().getEntity() instanceof ServerPlayer atacante)) {
            return;
        }
        LivingEntity alvo = e.getEntity();
        Personagem p = de(atacante);

        // Intimidação: chance de o monstro ficar com medo (Fraqueza por 5 s)
        double medo = p.pericias.valor(Beneficio.INTIMIDACAO_FRAQUEZA);
        if (medo > 0 && alvo instanceof Enemy && ALEATORIO.nextDouble() < medo) {
            alvo.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0, false, true, true));
        }

        CompoundTag dados = alvo.getPersistentData();
        String chave = CHAVE_FURTIVO + atacante.getUUID();
        if (!dados.contains(chave)) {
            return;
        }
        if (dados.getLong(chave) > alvo.level().getGameTime()) {
            // 2 de dano fixo + % do golpe (Furtividade: +50%, +100% ou +150%)
            float extra = 2.0F + e.getAmount() * (float) p.pericias.valor(Beneficio.FURTIVIDADE_DANO);
            e.setAmount(e.getAmount() + extra);
            dizer(atacante, "Ataque furtivo! +" + Math.round(extra) + " de dano", ChatFormatting.GOLD);
        }
        dados.putLong(chave, 0L);
    }

    // ------------------------------------------------------------------ Reflexos

    /**
     * Todo ataque que vai atingir o jogador faz um teste de Reflexos (DT {@link EsquivaReflexos#DT_PADRAO}, mais com vários inimigos).
     * Passou: o ataque é cancelado (sem dano, sem tranco) e o personagem dá um passo de esquiva (Epic Fight).
     * Roda no LivingAttackEvent, antes de o dano existir, então a esquiva também poupa a sanidade e os efeitos do golpe.
     */
    @SubscribeEvent
    public static void aoEsquivar(LivingAttackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer j) || j.isCreative() || j.isSpectator() || !j.isAlive()) {
            return;
        }
        DamageSource fonte = e.getSource();
        if (fonte.getEntity() == null || fonte.getEntity() == j || e.getAmount() <= 0
                || fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (j.invulnerableTime > 10) { // ainda nos quadros de invulnerabilidade do golpe anterior: não é um ataque novo
            return;
        }
        Personagem p = de(j);
        if (!p.isCriado()) {
            return;
        }
        // quanto mais inimigos mirando em você (num raio curto), mais difícil esquivar de cada golpe
        int cercando = j.level().getEntitiesOfClass(Mob.class, j.getBoundingBox().inflate(EsquivaReflexos.RAIO_CERCO),
                m -> m instanceof Enemy && m.isAlive() && m.getTarget() == j).size();
        ResultadoTeste r = p.pericias.testar(Pericia.REFLEXOS, EsquivaReflexos.dt(cercando), ALEATORIO);
        RolagemNaHud.mostrar(j, r, -1, "Esquiva"); // só estética: o servidor já decidiu
        if (r.sucesso()) {
            e.setCanceled(true);
            EsquivaEpicFight.esquivar(j, fonte);
        }
    }

    // ------------------------------------------------------------------ passivas por segundo

    @SubscribeEvent
    public static void aoTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer j)
                || j.isSpectator() || j.tickCount % 20 != 0) {
            return;
        }
        Personagem p = de(j);
        GrauTreinamento atletismo = p.pericias.getGrau(Pericia.ATLETISMO);
        if (atletismo.pelomenos(GrauTreinamento.VETERANO)) {
            j.addEffect(new MobEffectInstance(MobEffects.JUMP, 60, 0, true, false, false));
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

    @SubscribeEvent
    public static void aoComer(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof ServerPlayer j) || !e.getItem().isEdible()) {
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

    @SubscribeEvent
    public static void aoCurarOutro(PlayerInteractEvent.EntityInteract e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer j)
                || !e.getItemStack().is(Items.PAPER) || !(e.getTarget() instanceof LivingEntity alvo)
                || alvo.getHealth() >= alvo.getMaxHealth()) {
            return;
        }
        primeirosSocorros(j, alvo, e.getItemStack());
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    @SubscribeEvent
    public static void aoCurarASiMesmo(PlayerInteractEvent.RightClickItem e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer j)
                || !j.isCrouching() || !e.getItemStack().is(Items.PAPER) || j.getHealth() >= j.getMaxHealth()) {
            return;
        }
        primeirosSocorros(j, j, e.getItemStack());
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
    }

    private static void primeirosSocorros(ServerPlayer j, LivingEntity alvo, ItemStack papel) {
        if (j.getCooldowns().isOnCooldown(Items.PAPER)) {
            dizer(j, "Curativo em recarga.", ChatFormatting.GRAY);
            return;
        }
        Personagem p = de(j);
        ResultadoTeste r = p.pericias.testar(Pericia.MEDICINA, Pericias.DT_MEDIA, ALEATORIO);
        avisarTeste(j, r);
        if (!j.isCreative()) {
            papel.shrink(1);
        }
        double recarga = p.pericias.valor(Beneficio.MEDICINA_RECARGA); // 5, 4 ou 3 s (5 se destreinada)
        j.getCooldowns().addCooldown(Items.PAPER, (int) (20 * (recarga > 0 ? recarga : 5)));
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

    @SubscribeEvent
    public static void aoArrombar(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer j)
                || !e.getItemStack().is(Items.IRON_NUGGET)) {
            return;
        }
        BlockState estado = e.getLevel().getBlockState(e.getPos());
        if (!estado.is(Blocks.IRON_DOOR)) {
            return;
        }
        e.setCanceled(true);
        e.setCancellationResult(InteractionResult.SUCCESS);
        Personagem p = de(j);
        if (!p.pericias.estaTreinada(Pericia.CRIME)) {
            dizer(j, "Requer Crime treinada.", ChatFormatting.GRAY);
            return;
        }
        ResultadoTeste r = p.pericias.testar(Pericia.CRIME, Pericias.DT_MEDIA, ALEATORIO);
        avisarTeste(j, r);
        if (r.sucesso()) {
            ((DoorBlock) estado.getBlock()).setOpen(j, e.getLevel(), estado, e.getPos(),
                    !estado.getValue(DoorBlock.OPEN));
        } else if (ALEATORIO.nextDouble() < p.pericias.valor(Beneficio.CRIME_GAZUA)) {
            dizer(j, "A gazua escorregou.", ChatFormatting.GRAY);
        } else {
            if (!j.isCreative()) {
                e.getItemStack().shrink(1);
            }
            j.level().playSound(null, j.blockPosition(), SoundEvents.ITEM_BREAK,
                    net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
            dizer(j, "A gazua quebrou.", ChatFormatting.GRAY);
        }
    }

    // ------------------------------------------------------------------ Atualidades (XP)

    @SubscribeEvent
    public static void aoGanharXp(PlayerXpEvent.XpChange e) {
        if (!(e.getEntity() instanceof ServerPlayer j) || e.getAmount() <= 0) {
            return;
        }
        Personagem px = de(j);
        double extra = px.pericias.valor(Beneficio.ATUALIDADES_XP) + px.origem(Origem.Efeito.XP);
        if (extra > 0) {
            e.setAmount((int) Math.ceil(e.getAmount() * (1.0 + extra)));
        }
    }

    // ------------------------------------------------------------------ Adestramento

    @SubscribeEvent
    public static void aoDomar(AnimalTameEvent e) {
        if (!(e.getTamer() instanceof ServerPlayer j)) {
            return;
        }
        Personagem dono = de(j);
        double bonus = dono.pericias.valor(Beneficio.ADESTRAMENTO_VIDA)
                + dono.origem(Origem.Efeito.ADESTRAMENTO_VIDA); // 4, 8 ou 12 de vida (+4 do Trabalhador Rural)
        Animal bicho = e.getAnimal();
        AttributeInstance vida = bicho.getAttribute(Attributes.MAX_HEALTH);
        if (bonus == 0 || vida == null || vida.getModifier(ID_ADESTRAMENTO) != null) {
            return;
        }
        vida.addPermanentModifier(new AttributeModifier(ID_ADESTRAMENTO, "ordemmod_adestramento",
                bonus, AttributeModifier.Operation.ADDITION));
        dizer(j, "Adestramento: companheiro mais forte!", ChatFormatting.GREEN);
    }

    // ------------------------------------------------------------------ comandos

    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent e) {
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

        e.getDispatcher().register(raiz); // junta com o /ordem que já existe em EventosOrdem
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
