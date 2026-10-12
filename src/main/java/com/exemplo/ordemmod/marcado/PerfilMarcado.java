package com.exemplo.ordemmod.marcado;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exemplo.ordemmod.AnexosOrdem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;

/**
 * O que a cena do Marcado guardou do jogador: as respostas (indice da opcao em cada pergunta), as tags
 * ocultas somadas e se a cena ja terminou. Antes ficava no NBT do jogador (getPersistentData); agora e um
 * Data Attachment (veja AnexosOrdem.MARCADO). A API da classe continua a mesma.
 * As tags NAO mexem em afinidade elemental (esse campo nao existe, de proposito).
 */
public final class PerfilMarcado {
    /** O que realmente e salvo. Imutavel, como o Fabric exige. */
    public record Dados(boolean concluido, List<Integer> respostas, Map<String, Integer> tags) {
        public static final Codec<Dados> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.BOOL.optionalFieldOf("concluido", false).forGetter(Dados::concluido),
                Codec.INT.listOf().optionalFieldOf("respostas", List.of()).forGetter(Dados::respostas),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("tags", Map.of())
                        .forGetter(Dados::tags)
        ).apply(i, Dados::new));
    }

    public boolean concluido;
    public int[] respostas = new int[0];
    public final Map<String, Integer> tags = new LinkedHashMap<>();

    public static PerfilMarcado carregar(ServerPlayer j) {
        PerfilMarcado p = new PerfilMarcado();
        Dados d = j.getAttached(AnexosOrdem.MARCADO);
        if (d != null) {
            p.concluido = d.concluido();
            p.respostas = d.respostas().stream().mapToInt(Integer::intValue).toArray();
            p.tags.putAll(d.tags());
        }
        return p;
    }

    public void salvar(ServerPlayer j) {
        List<Integer> lista = new ArrayList<>();
        for (int r : respostas) {
            lista.add(r);
        }
        j.setAttached(AnexosOrdem.MARCADO, new Dados(concluido, List.copyOf(lista),
                Collections.unmodifiableMap(new LinkedHashMap<>(tags))));
    }

    /** Apaga tudo: a cena volta a abrir na proxima criacao (usado pelo /ordem resetar). */
    public static void resetar(ServerPlayer j) {
        j.removeAttached(AnexosOrdem.MARCADO);
    }

    /**
     * No Forge copiava o perfil do corpo antigo para o novo ao morrer. Agora o attachment ja e
     * criado com copyOnDeath(), entao nao ha mais nada a fazer. Fica so para nao quebrar quem chama.
     */
    public static void copiar(ServerPlayer de, ServerPlayer para) {
    }
}
