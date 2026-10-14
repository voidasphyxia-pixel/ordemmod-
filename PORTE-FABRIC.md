# Porte para Fabric 26.2

Branch `fabric-26.2`. O Forge 1.20.1 continua intacto na `main`.

## Como testar
1. Instale o **JDK 25**.
2. `gradlew.bat build` (ou `./gradlew build`).
3. `gradlew.bat runClient`, crie um mundo novo e entre nele. Espere uns 10 segundos.

No log (ou no console do `runClient`) deve aparecer, nesta ordem:
- `Ordem Mod carregado no Fabric! Nucleo OK ...`
- `Ordem Mod etapa 3: salvamento OK, pacote OK.`
- `[Ordem servidor] personagem NOVO para <seu nome> (criado=false)`
- `[Ordem cliente] servidor pediu a tela de criacao (com Marcado: true)`
- `[Ordem cliente] status recebido: PacoteStatus[...]`
- `[Ordem cliente] enviando tecla de teste (USAR) ao servidor`
- `[Ordem cliente] aviso recebido: ...`

Para testar o salvamento: saia do mundo e entre de novo; a linha do servidor deve dizer `CARREGADO do save`.

Para testar os comandos (etapa 4c), no mundo, abra o chat com `T` e digite (o `/ordem` nao tem restricao de permissao, como no original):
- `/ordem status`, `/ordem nex` (repita ate subir de nivel), `/ordem habilidades`
- `/ordem aprender <id>` (o chat sugere os ids com Tab), `/ordem usar <id>`
- `/ordem san 5`, `/ordem trilha`, `/ordem resetar`

Para testar a 4b: `/ordem san 0` (avisa "Voce esta enlouquecendo!" e aplica escuridao/fraqueza/lentidao), bater em um mob (as vezes aparece CRITICO!), `/effect give @s minecraft:poison` (as vezes "Resistiu a Veneno!"), dormir uma noite (devolve a sanidade).

As etapas 3, 4a, 4b e 4c foram compiladas e testadas no jogo. Se der erro, cole aqui a saida do `gradlew build`.

### Para testar a 4d (NAO compilada ainda)
Primeiro `gradlew build`; se der erro de compilacao ou de mixin, cole a saida aqui. Como a HUD ainda nao existe, os paineis de rolagem aparecem no log como `[Ordem cliente] rolagem recebida: ...`, e esquiva/rolada no chao aparecem como uma nuvem de fumaca (EsquivaVisual, provisorio).
- Origem e pericias (comandos): `/ordem origem`, `/ordem origem info <id>`, `/ordem origem <id>`, `/ordem pericias`, `/ordem beneficios`, `/ordem treinar <pericia>`, `/ordem grau <pericia>`, `/ordem testar <pericia> [dt]`.
- Acrobacia: caia de mais de 3 blocos (`/ordem treinar acrobacia` ajuda); no log aparece `rolagem recebida: Queda ...` e o dano reduz conforme o total.
- Reflexos: leve um golpe de um mob; no log aparece `rolagem recebida: Esquiva ...` e, se passar, nao ha dano nem sanidade perdida (fumaca no personagem).
- Furtividade e Iniciativa: treine as pericias, fique agachado perto de um zumbi (`Furtividade: ... nao te notou.` / `Iniciativa: voce agiu primeiro!`); bater num mob que nao te notou da `Ataque furtivo!`.
- Medicina: papel na mao, clique direito em um mob ferido (ou agachado em si mesmo ferido).
- Crime: pepita de ferro na mao, clique direito numa porta de ferro.
- Sobrevivencia: treine a pericia e coma algo; a barra de comida sobe mais.
- Atualidades: treine a pericia e pegue orbes de XP (ou `/xp add @s 10 points`).
- Adestramento: treine a pericia e domestique um lobo (`Adestramento: companheiro mais forte!`).
- Criminoso (origem): mate monstros; as vezes cai pepita de ouro.
- Atletismo e Percepcao: passivas por segundo (Salto/Graca do Golfinho; a cada 10 s os monstros por perto brilham).
- Marcado: a cena grava o resultado no servidor quando termina (so da para ver com a tela, etapa 6).

### Para testar a 5a (NAO compilada ainda)
A 5a so tem duas coisas: os sons (`SonsOrdem`) e a dependencia do GeckoLib no build.
1. `gradlew build`. Se o Gradle nao achar `com.geckolib:geckolib-fabric-26.2:5.5.5`, cole o erro aqui (pode ser a versao, o repositorio ou o nome do artefato).
2. `gradlew runClient`. No log deve aparecer `Ordem Mod etapa 5a: sons OK (dado_rolando = ordemmod:dado_rolando, 19 sons do Marcado).`
3. Dentro do mundo: `/playsound ordemmod:sucesso master @s`, `/playsound ordemmod:marcado.tum master @s` e `/playsound ordemmod:menu.tema_inicial master @s` (o tema e longo; pare saindo do mundo).

### Para testar a 5b (NAO compilada ainda)
**Antes de tudo, mova 2 arquivos** (o GeckoLib 5 procura os assets em `geckolib/`; sem isto o zumbi aparece como o modelo "faltando"/roxo-preto). Na raiz do projeto, no PowerShell:
```
New-Item -ItemType Directory -Force src/main/resources/assets/ordemmod/geckolib/models/entity, src/main/resources/assets/ordemmod/geckolib/animations/entity
git mv src/main/resources/assets/ordemmod/geo/entity/zumbi_sangue.geo.json src/main/resources/assets/ordemmod/geckolib/models/entity/zumbi_sangue.geo.json
git mv src/main/resources/assets/ordemmod/animations/entity/zumbi_sangue.animation.json src/main/resources/assets/ordemmod/geckolib/animations/entity/zumbi_sangue.animation.json
```
(A textura continua em `textures/entity/zumbi_sangue.png`. Nao mexa na `main`, que e Forge e usa os caminhos antigos.)

Depois: `gradlew build` (cole aqui os erros de compilacao, se houver; os mais provaveis estao listados em "Pontos de risco da 5b" abaixo) e `gradlew runClient`.
- No log: `Ordem Mod etapa 5b: entidade OK (zumbi_sangue = ordemmod:zumbi_sangue).` e o GeckoLib carregando como mod. Se o jogo reclamar de dependencia `geckolib`, cole o trecho.
- No mundo (o ovo de spawn e da 5c, abaixo): `/summon ordemmod:zumbi_sangue ~ ~ ~`. Ele deve nascer DEITADO e imovel, intocavel.
- Chegue a menos de 8 blocos (em sobrevivencia): ele se levanta (animacao de 5,4 s) e ganha a hitbox normal depois de ~3,6 s.
- Em pe: persegue correndo, ataca com 3 golpes diferentes (garra, arrasto, ataque), a 1,5x; o dano so aparece no momento do impacto da animacao; se voce se afastar antes, o golpe pega no ar.
- Ao leva-lo a dano: tranco de dano (animacao `dano`) e o som de zumbi.
- Ao mata-lo: animacao de morte completa (~2,2 s) e o corpo NAO tomba 90 graus; some so depois.
- Cabeca: em pe, ela acompanha o olhar. Se girar ao contrario (olha para onde voce NAO esta), troque `-` por `+` nas duas linhas do `adjustModelBonesForRender` em `ZumbiSangueRenderer`.
- Sair do mundo e entrar de novo: ele volta como estava (levantando volta a deitado; atacando volta a normal).

### Para testar a 5c (NAO compilada ainda)
So tem 3 arquivos novos/alterados: `ItensOrdem.java` (registro do ovo e da aba criativa), `assets/ordemmod/items/zumbi_sangue_spawn_egg.json` (definicao de modelo com as cores) e a chamada em `OrdemMod`. O modelo (`models/item/zumbi_sangue_spawn_egg.json`) e o nome (lang) ja existiam.
1. `gradlew build`. Pontos de risco (assinaturas do 26.2 tiradas da documentacao do Fabric, nao do jogo): `new Item.Properties().spawnEgg(EntityType)`, `Item.Properties.setId(ResourceKey)`, `SpawnEggItem::new` como `Function<Item.Properties, Item>` e `CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)` com `aba.accept(item)`.
2. `gradlew runClient`. No log: `Ordem Mod etapa 5c: item OK (zumbi_sangue_spawn_egg = ordemmod:zumbi_sangue_spawn_egg).`
3. Modo criativo, aba **Ovos de criacao** (Spawn Eggs): deve haver o "Ovo de spawn: Zumbi de Sangue" (no fim da aba, onde ficam os de mods), com o ovo vermelho-escuro (casca `0x4A0808`, manchas `0xC41E1E`).
   - Se o ovo aparecer SEM cor (cinza/branco): as cores vem do campo `tints` do `items/zumbi_sangue_spawn_egg.json`; confira se o jogo reclama desse arquivo no log (`Failed to load item model`...) e cole aqui.
   - Se aparecer o nome cru (`item.ordemmod.zumbi_sangue_spawn_egg`): o lang nao carregou.
4. Clique direito no chao com o ovo: o zumbi nasce deitado, como no `/summon`.
5. Dispenser com o ovo dentro tambem deve gerar o zumbi (comportamento padrao do `SpawnEggItem`).

## Etapas
- [x] 1. Esqueleto: Loom sem remapeamento, `fabric.mod.json`, entrypoint `OrdemMod`.
- [x] 2. Nucleo em Java puro (lista em `build.gradle`, variavel `nucleo`).
- [x] 3. Rede e salvamento (pacotes como payloads, `Rede.registrarTipos()`, Data Attachment). Testado.
- [~] 4. Eventos e pericias em jogo.
  - [x] 4a. Ciclo de vida do personagem em `EventosOrdem` (entrar, tick, sair, renascer, tecla de habilidade, status para a HUD), `Avisos`, `TesteNaTela`, `PerfilMarcado` (agora com attachment), `ReceptoresServidor` registrado. Testado (salvar e recarregar, status e aviso chegam ao cliente).
  - [x] 4b. `EventosDano` + `mixin/LivingEntityMixin`: dano e critico, resistencia a efeitos, dormir, estado mental (efeitos da sanidade baixa). Testado (enlouquecendo, mente se acalma, resistiu a veneno). Obs.: no 26.2 nao existem `EntityType.WARDEN` etc. nem `EntityType.is(tag)`; mortos-vivos sao comparados pelo id do registro. O dano e alterado ANTES da armadura (no Forge era depois).
  - [x] 4c. Comandos `/ordem ...` em `EventosOrdem.registrarComandos` (`CommandRegistrationCallback`). Testado.
  - [~] 4d. Escrita, NAO compilada nem testada: `EventosPericias`, `EventosOrigens`, `ReceptorMarcado` (+ `RoteiroMarcado`, `QuedaAcrobacia`, `EsquivaReflexos`, que ja eram Java puro), `RolagemNaHud`, `EsquivaVisual` (provisorio) e os mixins `MobMixin`, `PlayerMixin`, `TamableAnimalMixin` (mais dois ganchos novos no `LivingEntityMixin`). Veja "O que muda" abaixo para o que virou o que. (Ha commits posteriores corrigindo `PlayerMixin` e metodos publicos, o que sugere que ja foi compilada; confirmar e marcar como [x] depois de testar.)
- [~] 5. Entidades e sons (GeckoLib 5.x para 26.2).
  - [~] 5a. `SonsOrdem` (sem GeckoLib: cada campo e o proprio `SoundEvent`, registrado em `BuiltInRegistries.SOUND_EVENT`) e a dependencia `com.geckolib:geckolib-fabric-26.2:5.5.5` no build (repositorio Cloudsmith do GeckoLib, `geckolib_version` no `gradle.properties`). NAO compilada nem testada.
  - [~] 5b. Zumbi de Sangue: `EntidadesOrdem` (tipo registrado direto + atributos via `FabricDefaultAttributeRegistry`), `ZumbiSangueEntity` (GeckoLib 5, `hurtServer`, NBT com `ValueInput/ValueOutput`), `ZumbiSangueAtaqueGoal` (`doHurtTarget(ServerLevel, alvo)`), e o cliente: `ZumbiSangueRenderer` (faz o papel do antigo `ZumbiSangueModel`: modelo criado pelo id do tipo, cabeca via `adjustModelBonesForRender`, sem tombar na morte, culling inflado) e `ClienteEntidades.registrar()` (chamado por `OrdemModClienteFabric`). `fabric.mod.json` agora exige `geckolib >= 5.5.3`. NAO compilada nem testada. Exige mover 2 arquivos de assets (veja "Para testar a 5b"). Ja houve uma correcao de compilacao (`getDimensions` -> `getDefaultDimensions`).
  - [~] 5c. Ovo de spawn do zumbi: `ItensOrdem` (registro com `Item.Properties().spawnEgg(tipo)` + `setId`, entrada na aba criativa de ovos via `CreativeModeTabEvents`) e `assets/ordemmod/items/zumbi_sangue_spawn_egg.json` (definicao de modelo do 26.x; as cores 0x4A0808 / 0xC41E1E ficam aqui, em `tints`, e nao mais no item). NAO compilada nem testada.
- [ ] 6. HUD, teclas e telas (`HudOrdem`, `TeclasOrdem`, `Tela*`, `Cliente*`). Aqui trocar o `cliente/OrdemModClienteFabric` (provisorio) por `ReceptoresCliente.registrar()` e apagar o provisorio (mantendo a chamada `ClienteEntidades.registrar()`).
- [ ] 7. Esquiva e rolamento sem Epic Fight (`EsquivaEpicFight`, `RolamentoEpicFight`, hoje substituidos pelo provisorio `EsquivaVisual`) e armas sem TACZ.

## Pendencias pequenas
- Os arquivos `font/c-OFL.txt`, `font/Tektur-OFL.txt`, `font/z-OFL.txt` e `sounds/menu/CREDITOS-MUSICA.txt` tem maiuscula e geram "Invalid path in mod resource-pack" no log (inofensivo). Renomear para minusculas ou mover para fora de `assets/`.
- 4d: `TamableAnimalMixin` so cobre lobo, gato e papagaio; cavalos e afins ainda nao ganham o bonus de Adestramento.
- 4d: a queda (Acrobacia) agora testa quando o dano da queda e >= 1 (queda acima de 3 blocos); no Forge o gatilho era distancia >= 4.
- 5a: o codigo ainda nao portado que usa `SonsOrdem.X.get()` (telas, `RolagemHud`, cena do Marcado) precisa trocar para `SonsOrdem.X` quando for portado na etapa 6.
- 5b: `cliente/ZumbiSangueModel.java` ficou obsoleto (Forge/GeckoLib 4) e fora do build; pode ser apagado quando voce quiser. `ZumbiSangueRenderer` o substitui.
- 5b: os arquivos antigos em `assets/ordemmod/geo/` e `animations/` (caminho do GeckoLib 4) so servem para a `main` (Forge); neste branch eles saem de la pelo `git mv`.
- 5b: o arquivo `zumbi_sangue.animation.json` e do formato do GeckoLib 4; o GeckoLib 5 le o mesmo formato Bedrock, mas se alguma animacao nao tocar, o log do GeckoLib avisa o nome. Os nomes usados no codigo sao `animation.zumbi.` + deitado, levantar, idle, andar, correr, ataque_garra, ataque_arrasto, ataque, morte, dano.
- 5c: o ovo nao tem textura propria: usa as texturas padrao do jogo (`item/spawn_egg` e `item/spawn_egg_overlay`) pintadas pelas cores do `tints`, igual ao ovo de qualquer mob de vanilla.

## Pontos de risco da 5b (escrita sem compilar)
Usei como referencia o codigo-fonte do GeckoLib 5 (`bernie-g/geckolib`, pacotes `com.geckolib.*`) e mods 26.x reais que o usam, mas as assinaturas do Minecraft 26.2 abaixo vieram desses mods e nao do jogo em si:
- `Mob.xpReward` (campo) e `Monster.createMonsterAttributes()` no construtor/atributos da entidade.
- `getDimensions(Pose)` e `isImmobile()` sobrescritos como no 1.21; `tickDeath()` com `broadcastEntityEvent(this, (byte) 60)`. (Atualizacao: `getDimensions` e final no 26.2, o codigo agora sobrescreve `getDefaultDimensions`.)
- `getBoundingBoxForCulling(ZumbiSangueEntity)` no renderer (o metodo da entidade nao existe mais; no renderer e `protected AABB`).
- `EntityType.Builder.build(ResourceKey)` e `FabricDefaultAttributeRegistry.register`.
- `EntityRendererRegistry.register(tipo, ZumbiSangueRenderer::new)` com o renderer parametrizado por `LivingEntityRenderState`.
- GeckoLib: `AnimationController<ZumbiSangueEntity>("nome", ticksDeTransicao, predicado)`, `AnimationTest.setControllerSpeed(float)` (a velocidade 1,5x dos golpes passou a ser definida no predicado, a cada quadro; no GeckoLib 4 era um handler separado), `triggerableAnim`, `triggerAnim("dano", "dano")`.

## GeckoLib 5 (o que a 5b usou)
- Artefato `com.geckolib:geckolib-fabric-26.2` (5.5.1 a 5.5.5; usamos 5.5.5), id do mod `geckolib`. Classes: `com.geckolib.animatable.GeoEntity`, `...animatable.instance.AnimatableInstanceCache`, `...animatable.manager.AnimatableManager`, `...animation.AnimationController`, `...animation.RawAnimation`, `...animation.object.{LoopType,PlayState}`, `...animation.state.AnimationTest`, `...util.GeckoLibUtil`, `...renderer.GeoEntityRenderer<T, R extends EntityRenderState>`, `...renderer.base.{BoneSnapshots,GeoRenderState,RenderPassInfo}`, `...constant.DataTickets`, `...constant.dataticket.DataTicket`.
- Assets: `assets/ordemmod/geckolib/models/entity/<id>.geo.json`, `assets/ordemmod/geckolib/animations/entity/<id>.animation.json` e `textures/entity/<id>.png`, onde `<id>` e o id do tipo da entidade (`zumbi_sangue`).
- O renderer nao enxerga a entidade na hora de desenhar (RenderState): o que precisa vem de `addRenderData`, que copia dados para o estado via `DataTicket`.
- `setCustomAnimations` nao existe; ossos sao ajustados em `adjustModelBonesForRender(RenderPassInfo, BoneSnapshots)` com `ossos.ifPresent("head", osso -> osso.setRotX(...))`.

## O que muda ao portar o resto
- `Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador), pacote)` vira `Rede.paraJogador(jogador, pacote)`.
- `Rede.CANAL.sendToServer(pacote)` vira `ReceptoresCliente.enviar(pacote)`.
- `PacoteFicha.enviar(jogador)` virou `ReceptoresServidor.enviarFicha(jogador)`.
- `jogador.getPersistentData()` nao existe mais: usar attachments (`AnexosOrdem`). Na 4d: `AnexosOrdem.FURTIVO` (no monstro, mapa jogador->tick) e `AnexosOrdem.INICIATIVA_ATE` (no jogador), ambos sem persistir.
- Eventos do Forge (`@SubscribeEvent`) viram callbacks do Fabric registrados num metodo `registrar()` (veja `EventosOrdem`). Eventos de dano/efeito do Forge nao tem equivalente identico no Fabric: use mixin (veja `LivingEntityMixin`) ou `ServerLivingEntityEvents`.
- Eventos que viraram mixin na 4d: `LivingChangeTargetEvent` -> `MobMixin` (`Mob.setTarget`); `PlayerXpEvent.XpChange` -> `PlayerMixin` (`Player.giveExperiencePoints`); `AnimalTameEvent` -> `TamableAnimalMixin` (`TamableAnimal.tame`); `LivingEntityUseItemEvent.Finish` -> `LivingEntityMixin` (`completeUsingItem`). Eventos que viraram callbacks do Fabric: `LivingAttackEvent` -> `ALLOW_DAMAGE`; `LivingDropsEvent` -> `AFTER_DEATH` (o drop extra nasce na mao); `PlayerInteractEvent.*` -> `UseEntityCallback`/`UseItemCallback`/`UseBlockCallback`; `PlayerTickEvent` -> `END_SERVER_TICK`. `LivingFallEvent` e `LivingHurtEvent` das pericias foram para dentro de `EventosDano.aoSofrerDano`.
- `EntityType.X` (WARDEN, ENDERMAN...) e `EntityType.is(tag)` nao existem no 26.2: comparar pelo id do registro (`BuiltInRegistries.ENTITY_TYPE.getKey(tipo).getPath()`).
- `AttributeModifier` agora usa `Identifier` (nao `UUID`) e a operacao `ADD_MULTIPLIED_BASE` (era `MULTIPLY_BASE`); `ADDITION` virou `ADD_VALUE`.
- Efeitos: `MOVEMENT_SPEED` virou `SPEED` e `JUMP` virou `JUMP_BOOST` (alem de `CONFUSION` -> `NAUSEA` e `MOVEMENT_SLOWDOWN` -> `SLOWNESS`).
- `ItemStack.isEdible()` nao existe mais: comida e `stack.has(DataComponents.FOOD)`.
- `Player.getCooldowns()` trabalha com `ItemStack` (nao `Item`), e o som `SoundEvents.ITEM_BREAK` e um `Holder` (usar `.value()`).
- NBT na 1.21.5+: os getters devolvem `Optional`. Use `getIntOr`, `getStringOr`, `getBooleanOr`, `getCompoundOrEmpty`, `getListOrEmpty`. `getAllKeys()` mudou de nome. Vale para a `TelaFicha`, que le o CompoundTag do `PacoteFicha`. Em entidades, salvar/ler e por `ValueOutput`/`ValueInput` (`putInt`, `getIntOr`).
- Entidades (5b): `hurt(DamageSource, float)` virou `hurtServer(ServerLevel, DamageSource, float)` (so roda no servidor); `defineSynchedData()` recebe um `SynchedEntityData.Builder` e usa `builder.define(...)`; `doHurtTarget` recebe `(ServerLevel, Entity)`; `level().isClientSide` virou o metodo `isClientSide()`; `EntityType.Builder.build(...)` recebe um `ResourceKey`.
- Itens (5c): `new Item.Properties()` precisa de `.setId(ResourceKey<Item>)` antes de criar o item (o helper `ItensOrdem.registrar` faz isso); todo item precisa de uma definicao de modelo em `assets/<mod>/items/<id>.json` (alem do modelo em `models/item/`); a cor do ovo de spawn vai no `tints` dessa definicao; aba criativa e `CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.X)` (Fabric API, pacote `...api.creativetab.v1`).
- Os receptores do Fabric ja rodam na thread certa: sem `enqueueWork` e sem `DistExecutor`.
- `ResourceLocation` virou `Identifier` na 26.x.
- `ChatFormatting.getColor()` nao existe mais na 26.2: use `Avisos.rgbDe(cor)`.
- `Player.displayClientMessage(texto, true)` virou `sendOverlayMessage(texto)` (e `sendSystemMessage(texto)` para o chat).
- `RegisterCommandsEvent` vira `CommandRegistrationCallback.EVENT` (`net.fabricmc.fabric.api.command.v2`).
- Sons: `DeferredRegister<SoundEvent>` + `RegistryObject` viram `Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))` e o campo e o proprio `SoundEvent` (sem `.get()`). Veja `SonsOrdem`.
- Registro de renderer: o `EntityRenderersEvent.RegisterRenderers` do Forge vira `EntityRendererRegistry.register(tipo, Renderer::new)` (Fabric API, cliente), chamado de `ClienteEntidades.registrar()`.
