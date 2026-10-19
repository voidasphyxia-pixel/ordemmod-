package com.exemplo.ordemmod;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/**
 * Os sons do mod (Fabric 26.2). Os arquivos .ogg ficam em assets/ordemmod/sounds/ e são ligados aos nomes em sounds.json.
 * Para trocar um som, troque o arquivo .ogg (mesmo nome) ou aponte o nome para outro arquivo no sounds.json.
 *
 * No Forge cada som era um RegistryObject (DeferredRegister); aqui cada campo e o proprio SoundEvent, registrado direto
 * em BuiltInRegistries.SOUND_EVENT quando a classe carrega (OrdemMod.onInitialize chama registrar()).
 * Quem usava SonsOrdem.X.get() passa a usar SonsOrdem.X.
 */
public final class SonsOrdem {
    private SonsOrdem() {
    }

    public static final SoundEvent DADO_ROLANDO = som("dado_rolando");
    public static final SoundEvent BONUS_SOMA = som("bonus_soma");
    public static final SoundEvent SUCESSO = som("sucesso");
    public static final SoundEvent FALHA = som("falha");
    public static final SoundEvent CRITICO = som("critico");
    public static final SoundEvent FALHA_CRITICA = som("falha_critica");

    /** HUD de habilidades: som ao usar uma habilidade e som de "sem PE" (arquivos em sounds/habilidade/). */
    public static final SoundEvent HABILIDADE_USAR = som("habilidade.usar");
    public static final SoundEvent HABILIDADE_SEM_PE = som("habilidade.sem_pe");

    /** Tema em laco da tela inicial (arquivo sounds/menu/tema_inicial.ogg). */
    public static final SoundEvent TEMA_INICIAL = som("menu.tema_inicial");

    /** Sons da cena do Marcado: nome curto -> evento "marcado.<nome>" (arquivos em sounds/marcado/<nome>.ogg). */
    public static final String[] NOMES_MARCADO = { "tum", "ambiente_drone", "blip_texto", "ui_hover", "ui_select",
            "glitch", "batimento_loop", "revelacao_marcado", "sussuros_1", "sussuros_2", "sussuros_3", "sussuros_4",
            "sangue", "sombra", "transicao_mundo", "reacao_entidade", "anomalia_passos", "anomalia_arranhado",
            "distorcao" };
    public static final Map<String, SoundEvent> MARCADO = new LinkedHashMap<>();

    static {
        for (String nome : NOMES_MARCADO) {
            MARCADO.put(nome, som("marcado." + nome));
        }
    }

    private static SoundEvent som(String nome) {
        Identifier id = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, nome);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Só existe para forçar o carregamento desta classe (e portanto o registro acima) na inicialização. */
    public static void registrar() {
    }
}
