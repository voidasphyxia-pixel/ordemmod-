package com.exemplo.ordemmod;

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

    /** Anti-spam do clique de perícia na ficha (tick do jogo em que libera de novo). Não persiste. */
    public static final AttachmentType<Long> RECARGA_ROLAR = AttachmentRegistry.create(id("recarga_rolar"));

    /** Só existe para forçar o carregamento desta classe (e portanto o registro acima) na inicialização. */
    public static void registrar() {
    }
}
