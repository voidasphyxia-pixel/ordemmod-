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
 * O ovo de spawn (zumbi_sangue_spawn_egg, cores 0x4A0808 / 0xC41E1E) fica em ItensOrdem (etapa 5c).
 * Tambem da para testar com: /summon ordemmod:zumbi_sangue
 */
public final class EntidadesOrdem {
    private EntidadesOrdem() {
    }

    public static final EntityType<ZumbiSangueEntity> ZUMBI_SANGUE = registrarZumbiSangue();
    /** Armadura de Sangue (versao de teste, /ordem armadura). */
    public static final EntityType<ArmaduraSangueEntity> ARMADURA_SANGUE = registrarArmaduraSangue();

    /** Armadura de Sangue vestida no personagem (criada pela habilidade). */
    public static final EntityType<ArmaduraSangueJogadorEntity> ARMADURA_SANGUE_JOGADOR =
            registrarArmaduraSangueJogador();

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

    private static EntityType<ArmaduraSangueEntity> registrarArmaduraSangue() {
        ResourceKey<EntityType<?>> chave = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "armadura_sangue"));
        EntityType<ArmaduraSangueEntity> tipo = EntityType.Builder
                .<ArmaduraSangueEntity>of(ArmaduraSangueEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .build(chave);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, chave, tipo);
    }

    private static EntityType<ArmaduraSangueJogadorEntity> registrarArmaduraSangueJogador() {
        ResourceKey<EntityType<?>> chave = ResourceKey.create(Registries.ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "armadura_sangue_jogador"));
        EntityType<ArmaduraSangueJogadorEntity> tipo = EntityType.Builder
                .<ArmaduraSangueJogadorEntity>of(ArmaduraSangueJogadorEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(10)
                .build(chave);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, chave, tipo);
    }

    /** Chamado no OrdemMod.onInitialize: carrega a classe (registra o tipo acima) e registra os atributos. */
    public static void registrar() {
        FabricDefaultAttributeRegistry.register(ZUMBI_SANGUE, ZumbiSangueEntity.criarAtributos());
        FabricDefaultAttributeRegistry.register(ARMADURA_SANGUE, ArmaduraSangueEntity.criarAtributos());
        FabricDefaultAttributeRegistry.register(ARMADURA_SANGUE_JOGADOR, ArmaduraSangueEntity.criarAtributos());
    }
}
