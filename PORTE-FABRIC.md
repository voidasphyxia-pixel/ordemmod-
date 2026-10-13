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
A 5a so tem duas coisas: os sons (`SonsOrdem`) e a dependencia do GeckoLib no build. Nenhum codigo usa o GeckoLib ainda; o objetivo e ver se a dependencia resolve e se o jogo abre com ela.
1. `gradlew build`. Se o Gradle nao achar `com.geckolib:geckolib-fabric-26.2:5.5.5`, cole o erro aqui (pode ser a versao, o repositorio ou o nome do artefato).
2. `gradlew runClient`. No log deve aparecer `Ordem Mod etapa 5a: sons OK (dado_rolando = ordemmod:dado_rolando, 19 sons do Marcado).` e, procurando por `geckolib` no log, o GeckoLib carregando como mod. Se o jogo reclamar de mod duplicado ou de dependencia, cole o trecho aqui.
3. Dentro do mundo: `/playsound ordemmod:sucesso master @s`, `/playsound ordemmod:marcado.tum master @s` e `/playsound ordemmod:menu.tema_inicial master @s` (o tema e longo; pare saindo do mundo).

## Etapas
- [x] 1. Esqueleto: Loom sem remapeamento, `fabric.mod.json`, entrypoint `OrdemMod`.
- [x] 2. Nucleo em Java puro (lista em `build.gradle`, variavel `nucleo`).
- [x] 3. Rede e salvamento (pacotes como payloads, `Rede.registrarTipos()`, Data Attachment). Testado.
- [~] 4. Eventos e pericias em jogo.
  - [x] 4a. Ciclo de vida do personagem em `EventosOrdem` (entrar, tick, sair, renascer, tecla de habilidade, status para a HUD), `Avisos`, `TesteNaTela`, `PerfilMarcado` (agora com attachment), `ReceptoresServidor` registrado. Testado (salvar e recarregar, status e aviso chegam ao cliente).
  - [x] 4b. `EventosDano` + `mixin/LivingEntityMixin`: dano e critico, resistencia a efeitos, dormir, estado mental (efeitos da sanidade baixa). Testado (enlouquecendo, mente se acalma, resistiu a veneno). Obs.: no 26.2 nao existem `EntityType.WARDEN` etc. nem `EntityType.is(tag)`; mortos-vivos sao comparados pelo id do registro. O dano e alterado ANTES da armadura (no Forge era depois).
  - [x] 4c. Comandos `/ordem ...` em `EventosOrdem.registrarComandos` (`CommandRegistrationCallback`). Testado.
  - [~] 4d. Escrita, NAO compilada nem testada: `EventosPericias`, `EventosOrigens`, `ReceptorMarcado` (+ `RoteiroMarcado`, `QuedaAcrobacia`, `EsquivaReflexos`, que ja eram Java puro), `RolagemNaHud`, `EsquivaVisual` (provisorio) e os mixins `MobMixin`, `PlayerMixin`, `TamableAnimalMixin` (mais dois ganchos novos no `LivingEntityMixin`). Veja "O que muda" abaixo para o que virou o que.
- [~] 5. Entidades e sons (GeckoLib 5.x para 26.2).
  - [~] 5a. `SonsOrdem` (sem GeckoLib: cada campo e o proprio `SoundEvent`, registrado em `BuiltInRegistries.SOUND_EVENT`) e a dependencia `com.geckolib:geckolib-fabric-26.2:5.5.5` no build (repositorio Cloudsmith do GeckoLib, `geckolib_version` no `gradle.properties`). NAO compilada nem testada. Ainda sem `depends` do GeckoLib no `fabric.mod.json`: entra na 5b, quando o codigo passar a usar a biblioteca e o id do mod (`geckolib`, a confirmar no log) estiver confirmado.
  - [ ] 5b. Entidade: `EntidadesOrdem` (tipo da entidade, ovo de spawn, atributos, aba criativa), `ZumbiSangueEntity`, `ZumbiSangueAtaqueGoal` e o cliente (`ZumbiSangueRenderer`, `ZumbiSangueModel`, `ClienteEntidades`). Veja "GeckoLib 5" abaixo.
- [ ] 6. HUD, teclas e telas (`HudOrdem`, `TeclasOrdem`, `Tela*`, `Cliente*`). Aqui trocar o `cliente/OrdemModClienteFabric` (provisorio) por `ReceptoresCliente.registrar()` e apagar o provisorio.
- [ ] 7. Esquiva e rolamento sem Epic Fight (`EsquivaEpicFight`, `RolamentoEpicFight`, hoje substituidos pelo provisorio `EsquivaVisual`) e armas sem TACZ.

## Pendencias pequenas
- Os arquivos `font/c-OFL.txt`, `font/Tektur-OFL.txt`, `font/z-OFL.txt` e `sounds/menu/CREDITOS-MUSICA.txt` tem maiuscula e geram "Invalid path in mod resource-pack" no log (inofensivo). Renomear para minusculas ou mover para fora de `assets/`.
- 4d: `TamableAnimalMixin` so cobre lobo, gato e papagaio; cavalos e afins ainda nao ganham o bonus de Adestramento.
- 4d: a queda (Acrobacia) agora testa quando o dano da queda e >= 1 (queda acima de 3 blocos); no Forge o gatilho era distancia >= 4.
- 5a: o codigo ainda nao portado que usa `SonsOrdem.X.get()` (telas, `RolagemHud`, cena do Marcado) precisa trocar para `SonsOrdem.X` quando for portado na etapa 6.

## GeckoLib 5 (notas para a 5b)
Lidas na wiki oficial (wiki.geckolib.com, secao GeckoLib5) e no Cloudsmith/mvnrepository. NADA disto foi compilado aqui; confirmar na 5b.
- Artefato: `com.geckolib:geckolib-fabric-26.2`; versoes 5.5.1 a 5.5.5 (a 5.5.5 e de 06/09/2026). O grupo do Maven mudou de `software.bernie.geckolib` para `com.geckolib`; os nomes dos pacotes das classes (`GeoEntity`, `AnimationController`, `GeoEntityRenderer`...) tambem podem ter mudado, entao os `import software.bernie.geckolib...` do codigo antigo nao servem de referencia.
- Arquivos de assets mudaram de lugar: o modelo vai para `assets/ordemmod/geckolib/models/entity/zumbi_sangue.geo.json` e as animacoes para `assets/ordemmod/geckolib/animations/entity/zumbi_sangue.animation.json` (hoje estao em `geo/entity/` e `animations/entity/`). A textura continua em `textures/entity/zumbi_sangue.png`. Isto exige mover 2 arquivos.
- Renderer: o GeckoLib 5 trabalha com RenderState (a entidade nao existe na hora de renderizar). Um renderer simples e `context -> new GeoEntityRenderer<>(context, EntidadesOrdem.ZUMBI_SANGUE)`, que monta o modelo sozinho a partir do id do tipo (`zumbi_sangue`). Renderer proprio vira `GeoEntityRenderer<ZumbiSangueEntity, R extends EntityRenderState & GeoRenderState>`.
- `setCustomAnimations` (cabeca seguindo o olhar no `ZumbiSangueModel`) nao existe mais; o gancho novo e `adjustModelBonesForRender(RenderPassInfo<R>, BoneSnapshots)` com `snapshots.ifPresent("head", osso -> osso.setRotX(...))`. O `getDeathMaxRotation` do renderer tambem muda por causa do RenderState.
- Controladores: o predicado recebe um `AnimationTest` e usa `test.setAndContinue(RawAnimation)`; os exemplos da wiki criam `new AnimationController<>(test -> ...)` sem passar a entidade. Falta confirmar na 5b: velocidade da animacao (o `setAnimationSpeedHandler` do ataque 1.5x), a transicao de 4 ticks e o `triggerableAnim`/`triggerAnim` do tranco de dano.
- Do lado do Minecraft (nao e GeckoLib): a `ZumbiSangueEntity` usa APIs que mudaram desde a 1.20.1 (`hurt`, `defineSynchedData`, salvar/ler NBT, `EntityType.Builder`, atributos e spawn egg no Fabric). Isso sera conferido na compilacao.

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
- NBT na 1.21.5+: os getters devolvem `Optional`. Use `getIntOr`, `getStringOr`, `getBooleanOr`, `getCompoundOrEmpty`, `getListOrEmpty`. `getAllKeys()` mudou de nome. Vale para a `TelaFicha`, que le o CompoundTag do `PacoteFicha`.
- Os receptores do Fabric ja rodam na thread certa: sem `enqueueWork` e sem `DistExecutor`.
- `ResourceLocation` virou `Identifier` na 26.x.
- `ChatFormatting.getColor()` nao existe mais na 26.2: use `Avisos.rgbDe(cor)`.
- `Player.displayClientMessage(texto, true)` virou `sendOverlayMessage(texto)` (e `sendSystemMessage(texto)` para o chat).
- `RegisterCommandsEvent` vira `CommandRegistrationCallback.EVENT` (`net.fabricmc.fabric.api.command.v2`).
- Sons: `DeferredRegister<SoundEvent>` + `RegistryObject` viram `Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id))` e o campo e o proprio `SoundEvent` (sem `.get()`). Veja `SonsOrdem`.
