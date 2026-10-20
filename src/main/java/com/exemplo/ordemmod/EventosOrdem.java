package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import com.exemplo.ordemmod.entidade.ArmaduraSangueEntity;
import com.exemplo.ordemmod.marcado.PerfilMarcado;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * A ponte entre o jogo e as suas regras: o CICLO DE VIDA do personagem (entrar, tick, sair, renascer), o uso
 * de habilidades pela tecla e os comandos /ordem. No Forge eram metodos @SubscribeEvent; no Fabric sao callbacks
 * registrados em {@link #registrar()}. O personagem e salvo no proprio jogador (veja SalvamentoOrdem).
 *
 * AINDA FALTA PORTAR (continua na main, no EventosOrdem.java do Forge): dano e critico, resistencia a efeitos,
 * dormir e estado mental (efeitos da sanidade baixa). Entram na parte 4b.
 */
public class EventosOrdem {
    private static final Map<UUID, Personagem> PERSONAGENS = new HashMap<>();
    private static final Identifier ID_VELOCIDADE =
            Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "velocidade");

    /** Luz (0 a 15) a partir da qual o lugar e considerado escuro / bem iluminado. */
    private static final int LUZ_ESCURO = 4;
    private static final int LUZ_CLARO = 9;

    /** O que ja foi mandado a cada jogador para a HUD, para so reenviar quando mudar. */
    private static final Map<UUID, PacoteStatus> ULTIMO_STATUS = new HashMap<>();
    private static final Map<UUID, String> ULTIMA_LINHA = new HashMap<>();

    private EventosOrdem() {
    }

    /** Liga os callbacks do Fabric. Chamar uma vez, no inicializador comum. */
    public static void registrar() {
        ServerPlayConnectionEvents.JOIN.register((handler, enviador, servidor) -> aoEntrar(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, servidor) -> aoSair(handler.getPlayer()));
        ServerPlayerEvents.AFTER_RESPAWN.register((antigo, novo, vivo) -> aoRenascer(novo));
        ServerTickEvents.END_SERVER_TICK.register(servidor -> {
            for (ServerPlayer jogador : List.copyOf(servidor.getPlayerList().getPlayers())) {
                aoTick(jogador);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(EventosOrdem::salvarTodos);
        // Seguranca: ao fechar o mundo, limpa a memoria para nao vazar para outro mundo.
        ServerLifecycleEvents.SERVER_STOPPED.register(servidor -> {
            PERSONAGENS.clear();
            ULTIMO_STATUS.clear();
            ULTIMA_LINHA.clear();
        });
        // No Forge era o RegisterCommandsEvent.
        CommandRegistrationCallback.EVENT.register((dispatcher, acessoRegistros, ambiente) ->
                registrarComandos(dispatcher));
    }

    /** Pega o personagem do jogador; se ainda nao existe, le o salvo ou cria um Combatente. */
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
            System.out.println("[Ordem servidor] personagem " + (novo ? "NOVO" : "CARREGADO do save")
                    + " para " + jogador.getName().getString() + " (criado=" + p.isCriado() + ")");
        }
        return p;
    }

    /** Pede ao cliente do jogador que abra a tela de criacao de personagem. */
    public static void abrirCriacao(ServerPlayer jogador) {
        Rede.paraJogador(jogador, new PacoteAbrirCriacao(!PerfilMarcado.carregar(jogador).concluido));
    }

    /** Ao entrar no mundo: quem ainda nao criou o personagem ve a tela de criacao. */
    private static void aoEntrar(ServerPlayer jogador) {
        ULTIMO_STATUS.remove(jogador.getUUID()); // o cliente e novo: manda tudo de novo
        ULTIMA_LINHA.remove(jogador.getUUID());
        if (!de(jogador).isCriado()) {
            abrirCriacao(jogador);
        }
    }

    /** Coloca o personagem recem-criado (vindo da tela de criacao) no lugar do atual. */
    public static void definir(ServerPlayer jogador, Personagem novo) {
        PERSONAGENS.put(jogador.getUUID(), novo);
        aplicar(jogador, novo);
        jogador.setHealth(jogador.getMaxHealth());
        int esmeraldas = (int) novo.origem(Origem.Efeito.ESMERALDAS_INICIAIS); // Magnata
        if (esmeraldas > 0) {
            ItemStack dinheiro = new ItemStack(Items.EMERALD, esmeraldas);
            if (!jogador.getInventory().add(dinheiro)) {
                jogador.drop(dinheiro, false);
            }
        }
        SalvamentoOrdem.salvar(jogador, novo);
    }

    /** Passa a vida maxima e a velocidade calculadas pelo mod para o jogador. */
    static void aplicar(ServerPlayer jogador, Personagem p) {
        AttributeInstance vida = jogador.getAttribute(Attributes.MAX_HEALTH);
        if (vida != null) {
            vida.setBaseValue(p.vidaMaxima());
        }
        AttributeInstance velocidade = jogador.getAttribute(Attributes.MOVEMENT_SPEED);
        if (velocidade != null) {
            velocidade.removeModifier(ID_VELOCIDADE);
            velocidade.addTransientModifier(new AttributeModifier(ID_VELOCIDADE,
                    p.velocidadeExtra(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }

    /** 20 vezes por segundo: PE se recupera, efeitos temporarios passam e a barra e mostrada. */
    private static void aoTick(ServerPlayer jogador) {
        Personagem p = de(jogador);
        p.pe.tick(p.classe);
        p.efeitos.tick();
        if (jogador.tickCount % 20 == 0 && !jogador.isCreative() && !jogador.isSpectator()) {
            int luz = jogador.level().getMaxLocalRawBrightness(jogador.blockPosition());
            p.passarSegundo(luz <= LUZ_ESCURO, luz >= LUZ_CLARO);
            // TODO parte 4b: aplicarEstadoMental(jogador, p) (efeitos da sanidade baixa)
        }
        if (jogador.tickCount % 200 == 0) {
            SalvamentoOrdem.salvar(jogador, p); // a cada 10 segundos
        }
        atualizarTela(jogador, p);
    }

    /**
     * Manda para o cliente o que a HUD precisa: os numeros das barras (so quando mudam) e a linha de texto
     * com a habilidade selecionada e os efeitos ativos (a unica coisa que ainda aparece na barra de acao).
     */
    private static void atualizarTela(ServerPlayer j, Personagem p) {
        PacoteStatus status = PacoteStatus.de(p);
        if (j.tickCount % 100 == 0 || !status.equals(ULTIMO_STATUS.get(j.getUUID()))) {
            ULTIMO_STATUS.put(j.getUUID(), status);
            Rede.paraJogador(j, status);
        }
        String linha = p.linhaDeHabilidades();
        String antes = ULTIMA_LINHA.getOrDefault(j.getUUID(), "");
        if (!linha.equals(antes) || (!linha.isEmpty() && j.tickCount % 40 == 0)) {
            ULTIMA_LINHA.put(j.getUUID(), linha);
            // Na 26.2 o displayClientMessage(texto, true) virou sendOverlayMessage(texto): barra de texto acima da hotbar
            j.sendOverlayMessage(Component.literal(linha));
        }
    }

    /** Ao morrer, o jogo zera a vida maxima: aplica de novo. */
    private static void aoRenascer(ServerPlayer jogador) {
        aplicar(jogador, de(jogador));
        jogador.setHealth(jogador.getMaxHealth());
    }

    /** Ao sair do mundo, salva o personagem e tira da memoria. */
    private static void aoSair(ServerPlayer jogador) {
        ULTIMO_STATUS.remove(jogador.getUUID());
        ULTIMA_LINHA.remove(jogador.getUUID());
        Personagem p = PERSONAGENS.remove(jogador.getUUID());
        if (p != null) {
            SalvamentoOrdem.salvar(jogador, p);
        }
    }

    /** Antes de fechar o servidor, salva todo mundo que ainda esta online. */
    private static void salvarTodos(MinecraftServer servidor) {
        for (ServerPlayer jogador : servidor.getPlayerList().getPlayers()) {
            Personagem p = PERSONAGENS.get(jogador.getUUID());
            if (p != null) {
                SalvamentoOrdem.salvar(jogador, p);
            }
        }
    }

    // ------------------------------------------------------------------ comandos

    /**
     * /ordem status | nex | san <valor> | trilha [nome] | habilidades | aprender <id> | usar <id> | resetar | criar
     */
    private static void registrarComandos(CommandDispatcher<CommandSourceStack> dispatcher) {
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
            PerfilMarcado.resetar(j); // a cena do Marcado tambem repete
            abrirCriacao(j); // recomecar = passar pela criacao de novo
            return ok(ctx, "Personagem reiniciado. Crie o novo personagem na tela que abriu.");
        }));

        // PROVISORIO (ate a etapa 6): sem a tela de criacao, nada marca o personagem como criado, e Reflexos,
        // Acrobacia e o dano das regras so rodam para personagem criado. Isto faz o que a tela faria.
        raiz.then(Commands.literal("criar").executes(ctx -> {
            ServerPlayer j = ctx.getSource().getPlayerOrException();
            Personagem p = de(j);
            p.setCriado(true);
            SalvamentoOrdem.salvar(j, p);
            return ok(ctx, "Personagem marcado como criado (provisorio, ate a tela de criacao da etapa 6).");
        }));

        // TESTE da Armadura de Sangue: /ordem armadura (toca a animacao na sua frente e deixa a pose final) | limpar
        raiz.then(Commands.literal("armadura")
                .executes(ctx -> {
                    ServerPlayer j = ctx.getSource().getPlayerOrException();
                    ArmaduraSangueEntity.invocarPara(j);
                    return ok(ctx, "Armadura de Sangue: a animação toca na sua frente (uns 4 segundos) e o modelo fica parado"
                            + " na pose final. Use /ordem armadura de novo para repetir ou /ordem armadura limpar.");
                })
                .then(Commands.literal("limpar").executes(ctx -> {
                    ServerPlayer j = ctx.getSource().getPlayerOrException();
                    int n = ArmaduraSangueEntity.limparPerto(j);
                    return ok(ctx, "Armaduras de teste removidas: " + n);
                })));

        dispatcher.register(raiz);
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
            Avisos.enviar(j, "Sem habilidades ativas.", ChatFormatting.GRAY);
            return;
        }
        if (acao == PacoteHabilidade.TROCAR) {
            p.proximaHabilidade();
        } else if (acao == PacoteHabilidade.USAR) {
            Resultado r = tentarUsar(j, p, selecionada);
            if (!r.ok()) {
                Avisos.enviar(j, r.texto(), ChatFormatting.GRAY);
            }
        } else {
            return;
        }
        // atualiza a tela na hora, sem esperar o proximo tick
        atualizarTela(j, p);
    }
}
