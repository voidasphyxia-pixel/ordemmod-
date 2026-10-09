package com.exemplo.ordemmod.marcado;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * O que a cena do Marcado guardou do jogador: as respostas (índice da opção em cada pergunta), as tags
 * ocultas somadas e se a cena já terminou. Fica no NBT do jogador, ao lado do resto do personagem.
 * As tags NÃO mexem em afinidade elemental (esse campo não existe, de propósito).
 */
public final class PerfilMarcado {
    private static final String RAIZ = "OrdemMarcado";

    public boolean concluido;
    public int[] respostas = new int[0];
    public final Map<String, Integer> tags = new LinkedHashMap<>();

    public static PerfilMarcado carregar(ServerPlayer j) {
        PerfilMarcado p = new PerfilMarcado();
        CompoundTag nbt = j.getPersistentData().getCompound(RAIZ);
        p.concluido = nbt.getBoolean("concluido");
        p.respostas = nbt.getIntArray("respostas");
        CompoundTag t = nbt.getCompound("tags");
        for (String k : t.getAllKeys()) {
            p.tags.put(k, t.getInt(k));
        }
        return p;
    }

    public void salvar(ServerPlayer j) {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("concluido", concluido);
        nbt.putIntArray("respostas", respostas);
        CompoundTag t = new CompoundTag();
        tags.forEach(t::putInt);
        nbt.put("tags", t);
        j.getPersistentData().put(RAIZ, nbt);
    }

    /** Apaga tudo: a cena volta a abrir na próxima criação (usado pelo /ordem resetar). */
    public static void resetar(ServerPlayer j) {
        j.getPersistentData().remove(RAIZ);
    }

    /** Usado ao morrer: copia o perfil do corpo antigo para o novo. */
    public static void copiar(ServerPlayer de, ServerPlayer para) {
        CompoundTag nbt = de.getPersistentData().getCompound(RAIZ);
        if (!nbt.isEmpty()) {
            para.getPersistentData().put(RAIZ, nbt.copy());
        }
    }
}
