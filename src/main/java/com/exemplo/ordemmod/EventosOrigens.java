package com.exemplo.ordemmod;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * As origens no jogo. A maioria dos poderes é lida direto nos outros eventos (dano, comida, cura...).
 * Aqui ficam o comando e o que precisa de um evento só dele.
 *
 * Comandos: /ordem origem            (lista, ou mostra a sua)
 *           /ordem origem info <id>  (detalhes de uma origem)
 *           /ordem origem <id>       (escolhe; só dá para escolher uma vez)
 */
@Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID)
public class EventosOrigens {
    private static final Random ALEATORIO = new Random();
    private static final List<String> IDS = Arrays.stream(Origem.values()).map(Origem::getId).toList();

    // ------------------------------------------------------------------ Criminoso: o crime compensa

    @SubscribeEvent
    public static void aoMatarMonstro(LivingDropsEvent e) {
        if (!(e.getSource().getEntity() instanceof ServerPlayer j) || !(e.getEntity() instanceof Enemy)) {
            return;
        }
        double chance = EventosOrdem.de(j).origem(Origem.Efeito.DROP_OURO);
        if (chance > 0 && ALEATORIO.nextDouble() < chance) {
            e.getDrops().add(new ItemEntity(e.getEntity().level(), e.getEntity().getX(), e.getEntity().getY(),
                    e.getEntity().getZ(), new ItemStack(Items.GOLD_NUGGET, 1 + ALEATORIO.nextInt(3))));
        }
    }

    // ------------------------------------------------------------------ comandos

    @SubscribeEvent
    public static void comandos(RegisterCommandsEvent e) {
        LiteralArgumentBuilder<CommandSourceStack> raiz = Commands.literal("ordem");

        raiz.then(Commands.literal("origem")
                .executes(EventosOrigens::mostrar)
                .then(Commands.literal("info")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(IDS, b))
                                .executes(ctx -> info(ctx, StringArgumentType.getString(ctx, "id")))))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(IDS, b))
                        .executes(ctx -> escolher(ctx, StringArgumentType.getString(ctx, "id")))));

        e.getDispatcher().register(raiz); // junta com o /ordem que já existe
    }

    private static int ok(CommandContext<CommandSourceStack> ctx, String texto) {
        ctx.getSource().sendSuccess(() -> Component.literal(texto), false);
        return 1;
    }

    private static int erro(CommandContext<CommandSourceStack> ctx, String texto) {
        ctx.getSource().sendFailure(Component.literal(texto));
        return 0;
    }

    private static String resumo(Origem o) {
        return o.getNome() + " | Perícias: " + o.getPericias().get(0).getNome() + " e "
                + o.getPericias().get(1).getNome() + " (treinadas de graça)"
                + "\nPoder - " + o.getPoder() + ": " + o.getDescricao();
    }

    private static int mostrar(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        Personagem p = EventosOrdem.de(ctx.getSource().getPlayerOrException());
        if (p.getOrigem() != null) {
            return ok(ctx, "Sua origem: " + resumo(p.getOrigem()));
        }
        StringBuilder sb = new StringBuilder("Escolha sua origem com /ordem origem <id> (só uma vez):");
        for (Origem o : Origem.values()) {
            sb.append("\n").append(o.getId()).append(" - ").append(o.getNome()).append(" (")
                    .append(o.getPericias().get(0).getNome()).append(", ")
                    .append(o.getPericias().get(1).getNome()).append(")");
        }
        sb.append("\nDetalhes: /ordem origem info <id>");
        return ok(ctx, sb.toString());
    }

    private static int info(CommandContext<CommandSourceStack> ctx, String texto) {
        Origem o = Origem.porTexto(texto);
        if (o == null) {
            return erro(ctx, "Origem não encontrada: " + texto + ". Use /ordem origem para ver a lista.");
        }
        return ok(ctx, resumo(o));
    }

    private static int escolher(CommandContext<CommandSourceStack> ctx, String texto) throws CommandSyntaxException {
        ServerPlayer j = ctx.getSource().getPlayerOrException();
        Personagem p = EventosOrdem.de(j);
        Origem o = Origem.porTexto(texto);
        if (o == null) {
            return erro(ctx, "Origem não encontrada: " + texto + ". Use /ordem origem para ver a lista.");
        }
        if (p.getOrigem() != null) {
            return erro(ctx, "Você já tem a origem " + p.getOrigem().getNome()
                    + ". Para recomeçar, use /ordem resetar.");
        }
        p.escolherOrigem(o);
        int esmeraldas = (int) p.origem(Origem.Efeito.ESMERALDAS_INICIAIS);
        if (esmeraldas > 0) { // Magnata: o patrocínio chega na hora
            ItemStack dinheiro = new ItemStack(Items.EMERALD, esmeraldas);
            if (!j.getInventory().add(dinheiro)) {
                j.drop(dinheiro, false);
            }
        }
        EventosOrdem.aplicar(j, p);
        SalvamentoOrdem.salvar(j, p);
        return ok(ctx, "Origem escolhida: " + resumo(o) + "\nVagas de treino livres: " + p.pericias.vagasRestantes());
    }
}
