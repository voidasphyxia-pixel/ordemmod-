package com.exemplo.ordemmod.entidade;

import com.exemplo.ordemmod.OrdemMod;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registro das entidades do mod (Fabric 26.2).
 *
 * No Forge eram DeferredRegister + RegistryObject + eventos no bus do mod. Aqui o EntityType e registrado direto
 * quando a classe carrega (campo ZUMBI_SANGUE) e os atributos sao registrados em registrar(), chamado pelo
 * OrdemMod.onInitialize.
 *
 * PENDENTE (etapa 5c): o ovo de spawn (zumbi_sangue_spawn_egg, cores 0x4A0808 / 0xC41E1E) e a entrada dele na aba
 * criativa. Ate la, para testar use: /summon ordemmod:zumbi_sangue
 */
public final class EntidadesOrdem {
    private EntidadesOrdem() {
    }

    public static final EntityType<ZumbiSangueEntity> ZUMBI_SANGUE = registrarZumbiSangue();

    private static EntityType<ZumbiSangueEntity> registrarZumbiSangue() {
        ResourceKey<EntityType<?>> chave = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "zumbi_sangue"));
        EntityType<ZumbiSangueEntity> tipo = EntityType.Builder
                .<ZumbiSangueEntity>of(ZumbiSangueEntity::new, MobCategory.MONSTER)
                .sized(0.6F, 1.95F)
                .clientTrackingRange(8)
                .build(chave);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, chave, tipo);
    }

    /** Chamado no OrdemMod.onInitialize: carrega a classe (registra o tipo acima) e registra os atributos. */
    public static void registrar() {
        FabricDefaultAttributeRegistry.register(ZUMBI_SANGUE, ZumbiSangueEntity.criarAtributos());
    }
}
