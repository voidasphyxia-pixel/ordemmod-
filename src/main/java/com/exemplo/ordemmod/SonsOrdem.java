package com.exemplo.ordemmod;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Os sons do mod. Os arquivos .ogg ficam em assets/ordemmod/sounds/ e são ligados aos nomes em sounds.json.
 * Para trocar um som, troque o arquivo .ogg (mesmo nome) ou aponte o nome para outro arquivo no sounds.json.
 */
public final class SonsOrdem {
    private SonsOrdem() {
    }

    public static final DeferredRegister<SoundEvent> SONS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, OrdemMod.MOD_ID);

    public static final RegistryObject<SoundEvent> DADO_ROLANDO = som("dado_rolando");
    public static final RegistryObject<SoundEvent> BONUS_SOMA = som("bonus_soma");
    public static final RegistryObject<SoundEvent> SUCESSO = som("sucesso");
    public static final RegistryObject<SoundEvent> FALHA = som("falha");
    public static final RegistryObject<SoundEvent> CRITICO = som("critico");
    public static final RegistryObject<SoundEvent> FALHA_CRITICA = som("falha_critica");

    /** Sons da cena do Marcado: nome curto -> evento "marcado.<nome>" (arquivos em sounds/marcado/<nome>.ogg). */
    public static final String[] NOMES_MARCADO = { "tum", "ambiente_drone", "blip_texto", "ui_hover", "ui_select",
            "glitch", "batimento_loop", "revelacao_marcado", "sussuros_1", "sussuros_2", "sussuros_3", "sussuros_4",
            "sangue", "sombra", "transicao_mundo", "reacao_entidade", "anomalia_passos", "anomalia_arranhado",
            "distorcao" };
    public static final Map<String, RegistryObject<SoundEvent>> MARCADO = new LinkedHashMap<>();

    static {
        for (String nome : NOMES_MARCADO) {
            MARCADO.put(nome, som("marcado." + nome));
        }
    }

    private static RegistryObject<SoundEvent> som(String nome) {
        return SONS.register(nome,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(OrdemMod.MOD_ID, nome)));
    }

    /** Chamado no construtor do OrdemMod. */
    public static void registrar(IEventBus bus) {
        SONS.register(bus);
    }
}
