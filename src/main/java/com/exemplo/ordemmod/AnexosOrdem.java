package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.exemplo.ordemmod.marcado.PerfilMarcado;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;

/**
 * Dados que ficam "grudados" no jogador (Data Attachment API do Fabric). Substitui o
 * getPersistentData() do Forge.
 */
public final class AnexosOrdem {
    private AnexosOrdem() {
    }

    private static Identifier id(String nome) {
        return Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, nome);
    }

    /** O personagem salvo. Persiste com o mundo e continua depois que o jogador morre. */
    public static final AttachmentType<SalvamentoOrdem.DadosSalvos> PERSONAGEM = AttachmentRegistry.create(
            id("personagem"),
            builder -> builder
                    .persistent(SalvamentoOrdem.DadosSalvos.CODEC)
                    .copyOnDeath());

    /** O que a cena do Marcado guardou (respostas, tags, se terminou). Persiste e sobrevive à morte. */
    public static final AttachmentType<PerfilMarcado.Dados> MARCADO = AttachmentRegistry.create(
            id("marcado"),
            builder -> builder
                    .persistent(PerfilMarcado.Dados.CODEC)
                    .copyOnDeath());

    /** Anti-spam do clique de perícia na ficha (tick do jogo em que libera de novo). Não persiste. */
    public static final AttachmentType<Long> RECARGA_ROLAR = AttachmentRegistry.create(id("recarga_rolar"));

    /**
     * Furtividade (etapa 4d). Fica no MONSTRO: até que tick do jogo cada jogador (UUID) continua sem ser notado.
     * Não persiste (no Forge era getPersistentData() do mob).
     */
    public static final AttachmentType<Map<UUID, Long>> FURTIVO = AttachmentRegistry.create(
            id("furtivo"),
            builder -> builder.initializer(HashMap::new));

    /** Iniciativa (etapa 4d). Fica no JOGADOR: tick do jogo em que ele pode testar Iniciativa de novo. Não persiste. */
    public static final AttachmentType<Long> INICIATIVA_ATE = AttachmentRegistry.create(id("iniciativa_ate"));

    /** Só existe para forçar o carregamento desta classe (e portanto o registro acima) na inicialização. */
    public static void registrar() {
    }
}
