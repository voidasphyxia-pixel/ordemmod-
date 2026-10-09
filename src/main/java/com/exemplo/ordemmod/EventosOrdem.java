package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerWakeUpEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A ponte entre o jogo e as suas regras. O Forge chama os métodos @SubscribeEvent
 * sozinho quando algo acontece (um tick passa, alguém apanha, um comando é digitado).
 * O personagem é salvo no próprio jogador (veja SalvamentoOrdem).
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID)
public class EventosOrdem {
    private static final Map<UUID, Personagem> PERSONAGENS = new HashMap<>();
    private static final UUID ID_VELOCIDADE =
            UUID.fromString("7b1f2c9e-4a53-4c1d-9a51-0d2f6e3b8a11");

    /** Luz (0 a 15) a partir da qual o lugar é considerado escuro / bem iluminado. */
    private static final int LUZ_ESCURO = 4;
    private static final int LUZ_CLARO = 9;

    /** O acerto crítico multiplica o dano final por isso. */
    private static final double MULTIPLICADOR_CRITICO = 2.0;

    /** Sorteio dos críticos e das resistências. */
    private static final Random ALEATORIO = new Random();

    /** Fica true enquanto o próprio mod aplica um efeito (a sanidade), para o jogador não "resistir" a ele. */
    private static boolean aplicandoEfeitoProprio = false;

    /** O que já foi mandado a cada jogador para a HUD, para só reenviar quando mudar. */
    private static final Map<UUID, PacoteStatus> ULTIMO_STATUS = new HashMap<>();
    private static final Map<UUID, String> ULTIMA_LINHA = new HashMap<>();

    /** Pega o personagem do jogador; se ainda não existe, lê o salvo ou cria um Combatente. */
    public static Personagem de(ServerPlayer jogador) {
        Personagem p = PERSONAGENS.get(jogador.getUUID());
        if (p == null) {
            p = SalvamentoOrdem.carregar(jogador); // tenta ler o que foi salvo
            boolean novo = p == null;
            if (novo) {
                p = new Personagem(ClasseOP.COMBATENTE);
            }
            PERSONAGENS.put(jogador.getUUID(), p);
            aplicar(jogador, p);
            if (novo) {
                jogador.setHealth(jogador.getMaxHealth());
            }
        }
        return p;
    }

    /** Pede ao cliente do jogador que abra a tela de criação de personagem. */
    public static void abrirCriacao(ServerPlayer jogador) {
        Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador), new PacoteAbrirCriacao(!com.exemplo.ordemmod.marcado.PerfilMarcado.carregar(jogador).concluido));
    }

    /** Ao entrar no mundo: quem ainda não criou o personagem vê a tela de criação. */
    @SubscribeEvent
    public static void aoEntrar(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer entrou) {
            ULTIMO_STATUS.remove(entrou.getUUID()); // o cliente é novo: manda tudo de novo
            ULTIMA_LINHA.remove(entrou.getUUID());
        }
        if (e.getEntity() instanceof ServerPlayer jogador && !de(jogador).isCriado()) {
            abrirCriacao(jogador);
        }
    }

    /** Coloca o personagem recém-criado (vindo da tela de criação) no lugar do atual. */
    public static void definir(ServerPlayer jogador, Personagem novo) {
        PERSONAGENS.put(jogador.getUUID(), novo);
        aplicar(jogador, novo);
        jogador.setHealth(jogador.getMaxHealth());
        int esmeraldas = (int) novo.origem(Origem.Efeito.ESMERALDAS_INICIAIS); // Magnata
        if (esmeraldas > 0) {
            net.minecraft.world.item.ItemStack dinheiro =
                    new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, esmeraldas);
            if (!jogador.getInventory().add(dinheiro)) {
                jogador.drop(dinheiro, false);
            }
        }
        SalvamentoOrdem.salvar(jogador, novo);
    }

    /** Passa a vida máxima e a velocidade calculadas pelo mod para o jogador. */
    static void aplicar(ServerPlayer jogador, Personagem p) {
        AttributeInstance vida = jogador.getAttribute(Attributes.MAX_HEALTH);
        vida.setBaseValue(p.vidaMaxima());

        AttributeInstance velocidade = jogador.getAttribute(Attributes.MOVEMENT_SPEED);
        velocidade.removeModifier(ID_VELOCIDADE);
        velocidade.addTransientModifier(new AttributeModifier(ID_VELOCIDADE,
                "ordemmod_velocidade", p.velocidadeExtra(),
                AttributeModifier.Operation.MULTIPLY_BASE));
    }

    /** 20 vezes por segundo: PE se recupera, efeitos temporários passam e a barra é mostrada. */
    @SubscribeEvent
    public static void aoTick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer jogador)) {
            return;
        }
        Personagem p = de(jogador);
        p.pe.tick(p.classe);
        p.efeitos.tick();
        if (jogador.tickCount % 20 == 0 && !jogador.isCreative() && !jogador.isSpectator()) {
            int luz = jogador.level().getMaxLocalRawBrightness(jogador.blockPosition());
            p.passarSegundo(luz <= LUZ_ESCURO, luz >= LUZ_CLARO);
            aplicarEstadoMental(jogador, p);
        }
        if (jogador.tickCount % 200 == 0) {
            SalvamentoOrdem.salvar(jogador, p); // a cada 10 segundos
        }
        atualizarTela(jogador, p);
    }

    /**
     * Manda para o cliente o que a HUD precisa: os números das barras (só quando mudam) e a linha de texto
     * com a habilidade selecionada e os efeitos ativos (a única coisa que ainda aparece na barra de ação).
     */
    private static void atualizarTela(ServerPlayer j, Personagem p) {
        PacoteStatus status = PacoteStatus.de(p);
        if (j.tickCount % 100 == 0 || !status.equals(ULTIMO_STATUS.get(j.getUUID()))) {
            ULTIMO_STATUS.put(j.getUUID(), status);
            Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> j), status);
        }
        String linha = p.linhaDeHabilidades();
        String antes = ULTIMA_LINHA.getOrDefault(j.getUUID(), "");
        if (!linha.equals(antes) || (!linha.isEmpty() && j.tickCount % 40 == 0)) {
            ULTIMA_LINHA.put(j.getUUID(), linha);
            j.displayClientMessage(Component.literal(linha), true); // true = barra de texto acima da hotbar
        }
    }

    /** Toda vez que alguém vai sofrer dano: reduz o que o jogador recebe e aumenta o que ele causa. */
    @SubscribeEvent
    public static void aoSofrerDano(LivingHurtEvent e) {
        if (e.getEntity() instanceof ServerPlayer vitima) {
            Personagem p = de(vitima);
            if (!p.isCriado()) { // ainda na tela de criação: nada o machuca
                e.setCanceled(true);
                return;
            }
            double recebido = p.habilidades.danoRecebidoFinal(e.getAmount(), false);
            recebido *= reducaoDePericias(vitima, p, e.getSource()); // Fortitude, Ciências, Religião, Pilotagem
            e.setAmount((float) recebido);
            Entity fonte = e.getSource().getEntity();
            if (fonte instanceof Enemy && !vitima.isCreative()) {
                int perda = perdaDeSanidade(fonte);
                // Vontade (e Ocultismo, se for paranormal) amortecem o susto
                p.perderSanidade(p.perdaDeSanidadeAjustada(perda, perda > 1, ALEATORIO));
            }
        }
        if (e.getSource().getEntity() instanceof ServerPlayer atacante) {
            Personagem p = de(atacante);
            TipoDano tipo;
            if (e.getSource().getDirectEntity() != atacante) {
                tipo = TipoDano.A_DISTANCIA; // flecha, projétil...
            } else if (atacante.getMainHandItem().isEmpty()) {
                tipo = TipoDano.DESARMADO;
            } else {
                tipo = TipoDano.CORPO_A_CORPO;
            }
            double base = e.getAmount();
            if (tipo != TipoDano.A_DISTANCIA) {
                base += EfeitosAtributos.bonusDano(p.atributos); // Força
            }
            base += p.danoFixoDeOrigem(tipo); // Lutador, Militar
            base *= p.pericias.multiplicadorDano(tipo); // Luta ou Pontaria: +% por patamar
            double dano = p.habilidades.danoFinal(base, tipo, p.efeitos);
            dano *= multiplicadorSituacional(atacante, p, e.getEntity()); // Ocultismo, Tática
            if (p.sorteouCritico(tipo, ALEATORIO)) {
                dano *= MULTIPLICADOR_CRITICO + p.pericias.bonusMultiplicadorCritico()
                        + p.origem(Origem.Efeito.CRITICO_MULT); // Investigação, T.I.
                avisarCritico(atacante, e.getEntity());
            }
            e.setAmount((float) dano);
        }
    }

    /** Mortos-vivos e criaturas do Outro Lado (alvo de Ocultismo, origem do medo de Religião). */
    private static boolean ehDoOutroLado(Entity entidade) {
        return entidade instanceof LivingEntity vivo
                && (vivo.getMobType() == MobType.UNDEAD || vivo instanceof Warden);
    }

    /** Quanto do dano o jogador ainda recebe depois das perícias defensivas (1.0 = tudo, 0.8 = -20%). */
    private static double reducaoDePericias(ServerPlayer vitima, Personagem p, DamageSource fonte) {
        if (fonte.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) { // vazio e /kill não se reduzem
            return 1.0;
        }
        double fator = (1.0 - p.pericias.valor(Beneficio.FORTITUDE_REDUCAO))
                * (1.0 - p.origem(Origem.Efeito.REDUCAO_DANO)); // Vítima
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

    /** Bônus de dano que dependem da situação: alvo morto-vivo (Ocultismo) e aliado por perto (Tática). */
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

    /** Ao morrer, o jogo zera a vida máxima: aplica de novo. */
    @SubscribeEvent
    public static void aoRenascer(PlayerEvent.PlayerRespawnEvent e) {
        if (e.getEntity() instanceof ServerPlayer jogador) {
            aplicar(jogador, de(jogador));
            jogador.setHealth(jogador.getMaxHealth());
        }
    }

    /** Mostra "CRÍTICO!" e faz faíscas no alvo. */
    private static void avisarCritico(ServerPlayer atacante, LivingEntity alvo) {
        atacante.displayClientMessage(Component.literal("CRÍTICO!"), true);
        if (alvo.level() instanceof ServerLevel nivel) {
            nivel.sendParticles(ParticleTypes.CRIT, alvo.getX(), alvo.getY(0.5), alvo.getZ(),
                    15, 0.3, 0.3, 0.3, 0.2);
        }
    }

    /** Quando um efeito ruim (veneno, fraqueza, lentidão...) vai atingir o jogador, ele pode resistir. */
    @SubscribeEvent
    public static void aoReceberEfeito(MobEffectEvent.Applicable e) {
        if (aplicandoEfeitoProprio || !(e.getEntity() instanceof ServerPlayer jogador)) {
            return;
        }
        MobEffectInstance instancia = e.getEffectInstance();
        if (instancia.getEffect().getCategory() != MobEffectCategory.HARMFUL) {
            return;
        }
        Personagem p = de(jogador);
        boolean mental = instancia.getEffect() == MobEffects.DARKNESS
                || instancia.getEffect() == MobEffects.CONFUSION
                || instancia.getEffect() == MobEffects.BLINDNESS;
        if (p.resistiuAoEfeito(ALEATORIO, mental)) { // Vontade (mental) ou Fortitude (físico)
            e.setResult(Event.Result.DENY);
            Avisos.enviar(jogador, "Resistiu a " + instancia.getEffect().getDisplayName().getString() + "!",
                    net.minecraft.ChatFormatting.GREEN);
        }
    }

    /** Dormir uma noite de verdade devolve toda a sanidade. */
    @SubscribeEvent
    public static void aoAcordar(PlayerWakeUpEvent e) {
        if (e.getEntity() instanceof ServerPlayer jogador && !e.wakeImmediately()
                && jogador.getSleepTimer() >= 100) {
            de(jogador).descansar();
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

    /** Avisa quando a mente muda de estado e aplica os efeitos da sanidade baixa. */
    private static void aplicarEstadoMental(ServerPlayer j, Personagem p) {
        Personagem.Estado mudou = p.verificarMudancaDeEstado();
        if (mudou != null) {
            String aviso = switch (mudou) {
                case ESTAVEL -> "Sua mente se acalma.";
                case ABALADO -> "Sua mente vacila...";
                case ENLOUQUECENDO -> "Você está enlouquecendo!";
            };
            net.minecraft.ChatFormatting corAviso = switch (mudou) {
                case ESTAVEL -> net.minecraft.ChatFormatting.GREEN;
                case ABALADO -> net.minecraft.ChatFormatting.YELLOW;
                case ENLOUQUECENDO -> net.minecraft.ChatFormatting.RED;
            };
            Avisos.enviar(j, aviso, corAviso);
        }
        Personagem.Estado estado = p.estadoMental();
        if (estado == Personagem.Estado.ESTAVEL) {
            return;
        }
        efeito(j, MobEffects.DARKNESS);
        if (estado == Personagem.Estado.ENLOUQUECENDO) {
            efeito(j, MobEffects.WEAKNESS);
            efeito(j, MobEffects.MOVEMENT_SLOWDOWN);
            if ((j.tickCount / 20) % 10 == 0) { // enjoo só de vez em quando
                efeito(j, MobEffects.CONFUSION, 100);
            }
        }
    }

    private static void efeito(ServerPlayer j, MobEffect efeito) {
        efeito(j, efeito, 80);
    }

    private static void efeito(ServerPlayer j, MobEffect efeito, int duracao) {
        aplicandoEfeitoProprio = true;
        try {
            j.addEffect(new MobEffectInstance(efeito, duracao, 0, false, false, true));
        } finally {
            aplicandoEfeitoProprio = false;
        }
    }

    /** Ao sair do mundo, salva o personagem e tira da memória. */
    @SubscribeEvent
    public static void aoSair(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer jogador) {
            ULTIMO_STATUS.remove(jogador.getUUID());
            ULTIMA_LINHA.remove(jogador.getUUID());
            Personagem p = PERSONAGENS.remove(jogador.getUUID());
            if (p != null) {
                SalvamentoOrdem.salvar(jogador, p);
            }
        }
    }

    /** Segurança: ao fechar o mundo, limpa a memória para não vazar para outro mundo. */
    @SubscribeEvent
    public static void aoFecharServidor(ServerStoppedEvent e) {
        PERSONAGENS.clear();
    }

    // ------------------------------------------------------------------ comandos

    /**
     * /ordem status | nex | san <valor> | trilha [nome] | habilidades | aprender <id> | usar <id> | resetar
     */
    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("ordem");

        raiz.then(Commands.literal("status").executes(ctx -> {
            ServerPlayer j = ctx.getSource().getPlayerOrException();
            Personagem p = de(j);
            return ok(ctx, p.classe + (p.getOrigem() == null ? "" : " (" + p.getOrigem().getNome() + ")")
                    + " | " + p.barra(j.getHealth())
                    + " | pontos de habilidade: " + p.habilidades.getPontos()
                    + " | crítico (corpo a corpo): " + Math.round(p.chanceCritico(TipoDano.CORPO_A_CORPO) * 100)
                    + "% | resistência: " + Math.round(p.chanceResistencia() * 100) + "%");
        }));

        raiz.then(Commands.literal("nex").executes(ctx -> {
            ServerPlayer j = ctx.getSource().getPlayerOrException();
            Personagem p = de(j);
            boolean subiu = p.ganharNex(5);
            aplicar(j, p);
            SalvamentoOrdem.salvar(j, p);
            return ok(ctx, (subiu ? "Subiu de nível! " : "") + p.nex);
        }));

        raiz.then(Commands.literal("trilha")
                .executes(ctx -> listarTrilhas(ctx))
                .then(Commands.argument("nome", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                nomesDasTrilhas(de(ctx.getSource().getPlayerOrException())), b))
                        .executes(ctx -> escolherTrilha(ctx, StringArgumentType.getString(ctx, "nome")))));

        raiz.then(Commands.literal("habilidades").executes(ctx -> listarHabilidades(ctx)));

        raiz.then(Commands.literal("aprender")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            Personagem p = de(ctx.getSource().getPlayerOrException());
                            List<String> ids = new ArrayList<>();
                            for (Habilidade h : p.habilidades.disponiveis(p.nivel())) {
                                ids.add(h.getId());
                            }
                            return SharedSuggestionProvider.suggest(ids, b);
                        })
                        .executes(ctx -> aprender(ctx, StringArgumentType.getString(ctx, "id")))));

        raiz.then(Commands.literal("usar")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> {
                            Personagem p = de(ctx.getSource().getPlayerOrException());
                            List<String> ids = new ArrayList<>();
                            for (Habilidade h : p.habilidades.getAtivas()) {
                                ids.add(h.getId());
                            }
                            return SharedSuggestionProvider.suggest(ids, b);
                        })
                        .executes(ctx -> usar(ctx, StringArgumentType.getString(ctx, "id")))));

        raiz.then(Commands.literal("san")
                .then(Commands.argument("valor", IntegerArgumentType.integer(0, 999)).executes(ctx -> {
                    ServerPlayer j = ctx.getSource().getPlayerOrException();
                    Personagem p = de(j);
                    p.setSanidade(IntegerArgumentType.getInteger(ctx, "valor"));
                    SalvamentoOrdem.salvar(j, p);
                    return ok(ctx, "Sanidade: " + p.getSanidade() + "/" + p.sanidadeMaxima());
                })));

        raiz.then(Commands.literal("resetar").executes(ctx -> {
            ServerPlayer j = ctx.getSource().getPlayerOrException();
            Personagem novo = new Personagem(ClasseOP.COMBATENTE);
            PERSONAGENS.put(j.getUUID(), novo);
            aplicar(j, novo);
            j.setHealth(j.getMaxHealth());
            SalvamentoOrdem.salvar(j, novo);
            com.exemplo.ordemmod.marcado.PerfilMarcado.resetar(j); // a cena do Marcado também repete
            abrirCriacao(j); // recomeçar = passar pela criação de novo
            return ok(ctx, "Personagem reiniciado. Crie o novo personagem na tela que abriu.");
        }));

        e.getDispatcher().register(raiz);
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String texto) {
        ctx.getSource().sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int erro(CommandContext<CommandSourceStack> ctx, String texto) {
        ctx.getSource().sendFailure(Component.literal(texto));
        return 0;
    }

    private static List<String> nomesDasTrilhas(Personagem p) {
        List<String> nomes = new ArrayList<>();
        for (Trilha t : Trilha.values()) {
            if (t.getClasse() == p.classe) {
                nomes.add(t.name().toLowerCase(Locale.ROOT));
            }
        }
        return nomes;
    }

    private static int listarTrilhas(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Personagem p = de(ctx.getSource().getPlayerOrException());
        if (p.habilidades.getTrilha() != null) {
            return ok(ctx, "Sua trilha: " + p.habilidades.getTrilha().getNome());
        }
        StringBuilder sb = new StringBuilder("Trilhas de " + p.classe + " (escolha com /ordem trilha <nome>,"
                + " a partir do NEX 10%, não dá para trocar):");
        for (Trilha t : Trilha.values()) {
            if (t.getClasse() == p.classe) {
                sb.append("\n- ").append(t.name().toLowerCase(Locale.ROOT)).append(" (").append(t.getNome()).append(")");
            }
        }
        return ok(ctx, sb.toString());
    }

    private static int escolherTrilha(CommandContext<CommandSourceStack> ctx, String nome)
            throws CommandSyntaxException {
        ServerPlayer j = ctx.getSource().getPlayerOrException();
        Personagem p = de(j);
        Trilha alvo = null;
        for (Trilha t : Trilha.values()) {
            if (t.name().equalsIgnoreCase(nome)) {
                alvo = t;
            }
        }
        if (alvo == null) {
            return erro(ctx, "Trilha não encontrada. Use /ordem trilha para ver a lista.");
        }
        if (!p.habilidades.podeEscolherTrilha(alvo, p.nivel())) {
            return erro(ctx, p.habilidades.getTrilha() != null
                    ? "Você já escolheu a trilha " + p.habilidades.getTrilha().getNome() + "."
                    : "Não dá: precisa ser da sua classe e ter NEX 10% ou mais (use /ordem nex).");
        }
        List<Habilidade> antes = p.habilidades.getAprendidas();
        p.habilidades.escolherTrilha(alvo, p.nivel());
        StringBuilder sb = new StringBuilder("Trilha escolhida: " + alvo.getNome() + ".");
        for (Habilidade h : p.habilidades.getAprendidas()) {
            if (!antes.contains(h)) {
                sb.append("\nLiberada: ").append(h.getNome());
            }
        }
        aplicar(j, p);
        SalvamentoOrdem.salvar(j, p);
        return ok(ctx, sb.toString());
    }

    private static int listarHabilidades(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Personagem p = de(ctx.getSource().getPlayerOrException());
        StringBuilder sb = new StringBuilder("Pontos de habilidade: " + p.habilidades.getPontos());
        sb.append("\nTrilha: ").append(p.habilidades.getTrilha() == null
                ? "nenhuma" : p.habilidades.getTrilha().getNome());
        sb.append("\nAtivas:");
        for (Habilidade h : p.habilidades.getAtivas()) {
            sb.append("\n- ").append(h.getNome()).append(" [").append(h.getId()).append("] ")
                    .append(h.getCustoPe()).append(" PE");
        }
        sb.append("\nPassivas:");
        for (Habilidade h : p.habilidades.getPassivas()) {
            sb.append("\n- ").append(h.getNome());
        }
        sb.append("\nPode aprender (1 ponto):");
        for (Habilidade h : p.habilidades.disponiveis(p.nivel())) {
            sb.append("\n- ").append(h.getNome()).append(" [").append(h.getId()).append("]");
        }
        return ok(ctx, sb.toString());
    }

    private static int aprender(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        ServerPlayer j = ctx.getSource().getPlayerOrException();
        Personagem p = de(j);
        Habilidade h = CatalogoHabilidades.porId(id);
        if (h == null) {
            return erro(ctx, "Habilidade não encontrada: " + id);
        }
        if (!p.habilidades.aprender(h, p.nivel())) {
            return erro(ctx, "Não dá para aprender " + h.getNome()
                    + " agora (falta ponto, nível, pré-requisito ou atributo, ou já foi aprendida).");
        }
        aplicar(j, p);
        SalvamentoOrdem.salvar(j, p);
        return ok(ctx, "Aprendeu: " + h.getNome() + ". Pontos restantes: " + p.habilidades.getPontos());
    }

    private static int usar(CommandContext<CommandSourceStack> ctx, String id) throws CommandSyntaxException {
        ServerPlayer j = ctx.getSource().getPlayerOrException();
        Personagem p = de(j);
        Habilidade h = CatalogoHabilidades.porId(id);
        if (h == null) {
            return erro(ctx, "Habilidade não encontrada: " + id);
        }
        Resultado r = tentarUsar(j, p, h);
        return r.ok() ? ok(ctx, r.texto()) : erro(ctx, r.texto());
    }

    // ------------------------------------------------------------------ usar habilidade

    private record Resultado(boolean ok, String texto) {
    }

    /** Confere tudo, gasta o PE e ativa o efeito. Serve para o comando e para a tecla. */
    private static Resultado tentarUsar(ServerPlayer j, Personagem p, Habilidade h) {
        if (!p.habilidades.temAprendida(h)) {
            return new Resultado(false, "Você ainda não aprendeu " + h.getNome() + ".");
        }
        Habilidade efetiva = p.habilidades.melhorVersao(h);
        if (!efetiva.isAtiva()) {
            return new Resultado(false, efetiva.getNome() + " é passiva: já funciona sozinha.");
        }
        if (p.pe.getAtual() < efetiva.getCustoPe()) {
            return new Resultado(false, "PE insuficiente: " + efetiva.getNome() + " custa "
                    + efetiva.getCustoPe() + " e você tem " + p.pe.getAtual() + ".");
        }
        p.habilidades.usar(h, p.pe, p.efeitos);
        SalvamentoOrdem.salvar(j, p);
        return new Resultado(true, "Usou " + efetiva.getNome() + " (-" + efetiva.getCustoPe() + " PE).");
    }

    /** Chamado quando o jogador aperta uma tecla do mod (a mensagem chega pela Rede). */
    public static void aoTeclaHabilidade(ServerPlayer j, int acao) {
        Personagem p = de(j);
        Habilidade selecionada = p.habilidadeSelecionada();
        if (selecionada == null) {
            Avisos.enviar(j, "Sem habilidades ativas.", net.minecraft.ChatFormatting.GRAY);
            return;
        }
        if (acao == PacoteHabilidade.TROCAR) {
            p.proximaHabilidade();
        } else if (acao == PacoteHabilidade.USAR) {
            Resultado r = tentarUsar(j, p, selecionada);
            if (!r.ok()) {
                Avisos.enviar(j, r.texto(), net.minecraft.ChatFormatting.GRAY);
            }
        } else {
            return;
        }
        // atualiza a tela na hora, sem esperar o próximo tick
        atualizarTela(j, p);
    }
}
