package com.exemplo.ordemmod;

import java.util.function.Function;

import com.exemplo.ordemmod.entidade.EntidadesOrdem;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

/**
 * Itens do mod (Fabric 26.2). Hoje so o ovo de spawn do Zumbi de Sangue (etapa 5c).
 *
 * No Forge era DeferredRegister<Item> + ForgeSpawnEggItem(tipo, 0x4A0808, 0xC41E1E, ...) e o evento
 * BuildCreativeModeTabContentsEvent. No 26.x:
 *  - o item precisa do proprio id nas propriedades (Item.Properties.setId(chave)), feito em registrar(...);
 *  - o ovo e criado com new Item.Properties().spawnEgg(tipoDaEntidade) e a classe SpawnEggItem;
 *  - as CORES do ovo NAO ficam mais no item: ficam na definicao de modelo do item, em
 *    assets/ordemmod/items/zumbi_sangue_spawn_egg.json (campo "tints": 0x4A0808 = 4851720 e 0xC41E1E = 12852766);
 *  - a aba criativa usa CreativeModeTabEvents (Fabric API, pacote net.fabricmc.fabric.api.creativetab.v1).
 * O modelo (assets/ordemmod/models/item/zumbi_sangue_spawn_egg.json, parent item/template_spawn_egg) e o nome
 * (lang: item.ordemmod.zumbi_sangue_spawn_egg) ja existiam.
 */
public final class ItensOrdem {
    private ItensOrdem() {
    }

    public static final ResourceKey<Item> ZUMBI_SANGUE_SPAWN_EGG_ID = chave("zumbi_sangue_spawn_egg");

    // Carregar esta classe carrega EntidadesOrdem primeiro (o ovo precisa do tipo ja registrado).
    public static final Item ZUMBI_SANGUE_SPAWN_EGG = registrar(ZUMBI_SANGUE_SPAWN_EGG_ID, SpawnEggItem::new,
            new Item.Properties().spawnEgg(EntidadesOrdem.ZUMBI_SANGUE));

    private static ResourceKey<Item> chave(String nome) {
        return ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, nome));
    }

    private static Item registrar(ResourceKey<Item> chave, Function<Item.Properties, Item> fabrica, Item.Properties propriedades) {
        Item item = fabrica.apply(propriedades.setId(chave));
        return Registry.register(BuiltInRegistries.ITEM, chave, item);
    }

    /** Chamado no OrdemMod.onInitialize: carrega a classe (registra o item acima) e poe o ovo na aba de ovos. */
    public static void registrar() {
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
                .register(aba -> aba.accept(ZUMBI_SANGUE_SPAWN_EGG));
    }
}
