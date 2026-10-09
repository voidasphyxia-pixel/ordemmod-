package com.exemplo.ordemmod.entidade;

import com.exemplo.ordemmod.OrdemMod;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Registro das entidades do mod (e dos ovos de spawn delas). */
public final class EntidadesOrdem {

    private EntidadesOrdem() {
    }

    public static final DeferredRegister<EntityType<?>> ENTIDADES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OrdemMod.MOD_ID);
    public static final DeferredRegister<Item> ITENS =
            DeferredRegister.create(ForgeRegistries.ITEMS, OrdemMod.MOD_ID);

    public static final RegistryObject<EntityType<ZumbiSangueEntity>> ZUMBI_SANGUE = ENTIDADES.register(
            "zumbi_sangue",
            () -> EntityType.Builder.<ZumbiSangueEntity>of(ZumbiSangueEntity::new, MobCategory.MONSTER)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(8)
                    .build("zumbi_sangue"));

    public static final RegistryObject<Item> OVO_ZUMBI_SANGUE = ITENS.register(
            "zumbi_sangue_spawn_egg",
            () -> new ForgeSpawnEggItem(ZUMBI_SANGUE, 0x4A0808, 0xC41E1E, new Item.Properties()));

    /** Chamado no construtor do OrdemMod. */
    public static void registrar(IEventBus bus) {
        ENTIDADES.register(bus);
        ITENS.register(bus);
        bus.addListener(EntidadesOrdem::registrarAtributos);
        bus.addListener(EntidadesOrdem::abaCriativa);
    }

    private static void registrarAtributos(EntityAttributeCreationEvent e) {
        e.put(ZUMBI_SANGUE.get(), ZumbiSangueEntity.criarAtributos().build());
    }

    private static void abaCriativa(BuildCreativeModeTabContentsEvent e) {
        if (e.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            e.accept(OVO_ZUMBI_SANGUE);
        }
    }
}
