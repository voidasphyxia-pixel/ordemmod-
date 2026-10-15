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
- `[Ordem cliente] servidor pediu a tela de criacao (com Marcado: true) - a tela ainda nao foi portada (etapa 6b)`

(Desde a etapa 6a o cliente provisorio da etapa 4 foi apagado: o status, os avisos e as rolagens nao sao mais escritos no log, eles aparecem de verdade na HUD. A tecla de teste automatica tambem acabou: use R, G e K.)

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

### Para testar a 6a (NAO compilada ainda)
So a parte do CLIENTE que nao e tela: entrypoint, teclas, HUD, avisos e painel da rolagem. `gradlew build` primeiro; se der erro de compilacao, cole aqui (os pontos de maior risco estao em "Pontos de risco da 6a", abaixo).
1. `gradlew runClient`, crie um mundo novo em **sobrevivencia**, entre e espere uns segundos. No log, a linha `[Ordem cliente] servidor pediu a tela de criacao ... (etapa 6b)` e esperada (a tela ainda nao existe) e o jogo continua normal.
2. **HUD**: depois que o servidor mandar o status (1 a 2 s), os coracoes, a fome, a armadura, a barra de XP e a hotbar do jogo devem sumir e dar lugar as barras do mod: SAN (azul), PE (amarelo) + DEF (cinza), PV (vermelho) + FOME (marrom), NEX (branco, 20 celulas), XP (verde, fina) e a hotbar de vidro escuro com os itens. Cantos dourados em volta do bloco, vinheta escura no pe da tela e o simbolo ciano do medo no vao entre as colunas.
   - Troque de slot (rolagem do mouse / teclas 1 a 9): a casa selecionada ganha borda clara e cantos dourados. Segure algo na mao secundaria (F): a casa extra aparece ao lado.
   - Tome dano e perca fome: o pedaco perdido da barra fica um instante em branco e some devagar. Com `/ordem san 5` a barra de sanidade pisca (abaixo de 25%).
   - Modo criativo ou espectador: a HUD do mod NAO aparece e a do jogo volta (de proposito, como no Forge).
   - Debaixo d'agua: as bolhas de ar devem aparecer acima da pilha de barras, nao atras delas. Em um cavalo: a barra de pulo do jogo aparece no lugar da de XP, e os coracoes do cavalo ficam acima da pilha.
   - Pegue um item com nome: o nome (e a mensagem da barra de acao, ex.: `/title @s actionbar "oi"`) aparecem acima das barras.
3. **Avisos** embaixo da mira: `/ordem san 0` ("Voce esta enlouquecendo!"), `/ordem nex`, `/ordem aprender <id>`: o texto entra com fade, fica um instante e sai (ate 3 linhas).
4. **Painel da rolagem** (acima da barra de sanidade): `/ordem treinar acrobacia`, caia de mais de 3 blocos, ou leve um golpe de um mob (Reflexos): os numeros embaralham por ~0,75 s, param no resultado e somem; ao mesmo tempo o dano da queda/golpe e reduzido como na 4d.
5. **Teclas**: em Controles > Ordem Mod devem existir **Usar habilidade (R)**, **Trocar habilidade (G)** e **Abrir ficha do personagem (K)**. R e G funcionam (aviso/efeito da habilidade selecionada); K abre a ficha (tela da 6b, veja abaixo).
6. **Sair do mundo e entrar de novo**: a HUD some ao sair e so volta quando o servidor mandar o status de novo.
7. Se o texto da HUD sair torto, a fonte nao carregou, ou as letras estiverem altas/baixas demais na barra, ajuste `CENTRO_GLIFO` (e `CENTRO_GLIFO_TITULO` para os rotulos em Cinzel) no topo de `HudOrdem`.

### Para testar a 6b (NAO compilada ainda)
A tela do dado (`TelaTeste`) e a ficha (`TelaFicha`, tecla K). `gradlew build` primeiro; os pontos de maior risco estao em "Pontos de risco da 6b" abaixo. Obs.: `TelaTeste` e `IconesOP` ja tinham sido portados antes (por voce ou por outra sessao, estavam sem commit); o Claude so os incluiu no build e revisou o resto.
1. `gradlew runClient`, entre num mundo (o personagem ja criado serve) e aperte **K**: abre a ficha, com o fundo de nevoa/poeira, o titulo "FICHA DO PERSONAGEM" e a divisoria com o losango vermelho. K ou ESC fecham.
2. **Coluna esquerda**: a skin do jogador (olhando para o mouse), nome em Cinzel, Classe/Origem/Trilha, barra de NEX e os numeros (PV, SAN, PE, Defesa, Criticos, Resistencias, Velocidade, Dano). Se a skin nao aparecer ou ficar fora do painel, e o `DesenhoOP.entidade` (veja riscos).
3. **Aba ATRIBUTO**: o pentagono com aneis, o circulo ritual atras e os 5 atributos com o botao "+" (que ainda nao faz nada). Passe o mouse num atributo: a caixa de baixo explica. As linhas agora sao desenhadas por varredura (pode ficar um pouco serrilhado, ao contrario do Forge).
4. **Aba PERICIA**: perícias separadas por grau. **Clique esquerdo numa perícia rola o teste**: abre a tela do dado (animacao: dados rolando, bonus entrando, veredito); clique/Espaco pula, Esc ou K fecham; ao fechar volta para a ficha. Botao direito sobe de grau quando ha melhoria sobrando (seta verde).
5. **Aba PODERES**: icones dos poderes ligados por setas; clique escolhe, o botao do rodape aprende (1 ponto). **Aba TRILHA**: uma coluna por trilha; escolher pede um segundo clique ("CONFIRMAR?").
6. Rolagem do mouse rola as listas das abas Pericia/Poderes/Trilha. Com a ficha aberta, aprender um poder atualiza a tela sem trocar de aba.
7. Fora da ficha: `/ordem testar <pericia> [dt]` abre a tela do dado direto, por cima do jogo.

### Para testar a 6c (NAO compilada ainda)
A tela de criacao de personagem (`TelaCriacao`). `gradlew build` primeiro; riscos em "Pontos de risco da 6c" abaixo.
1. Para ver a tela com um personagem que ja existe: no mundo, `/ordem resetar` e saia e entre de novo (ou crie um mundo novo). O servidor manda `PacoteAbrirCriacao`; como a cena do Marcado ainda nao foi portada, o log mostra `[Ordem cliente] cena do Marcado ainda nao portada (etapa 6d): abrindo direto a criacao` e a tela de criacao abre sozinha.
2. **Etapa 1, Atributos**: o pentagono com circulo ritual e brilho atras, os 5 atributos com `-` e `+` e o numero no meio; a caixa "PONTOS RESTANTES" a direita. Clique em `+` num atributo que ja esta em 3: ele treme, toca o som de "nao" e aparece o aviso vermelho "NAO DA PARA SUBIR MAIS". O botao Avancar so liga quando os pontos acabam.
3. **Etapa 2, Origem** e **3, Classe** (tres cartas) e **4, Perícias**: passe o mouse para ler a descricao na caixa de baixo; clique escolhe; Voltar/Avancar/Confirmar no rodape. A skin do jogador aparece no painel da esquerda, olhando para o mouse.
4. **Confirmar**: a tela fecha e o personagem e criado no servidor (`[Ordem servidor] ...` no log). ESC nao fecha a tela (de proposito).
5. Se a tela abrir sem ter pedido (ou nao abrir), me avise: o `ClienteCriacao` agora ignora o parametro `comMarcado`.

### Para testar a 6d (NAO compilada ainda)
A cena do Marcado (`TelaMarcado`), que vem ANTES da criacao quando o personagem ainda nao viu a cena. `gradlew build` primeiro; riscos em "Pontos de risco da 6d" abaixo.
1. Para rever a cena: no mundo, `/ordem resetar` (apaga personagem e perfil do Marcado), saia e entre de novo, ou crie um mundo novo. Deve abrir a tela preta com o drone quase inaudivel; depois as falas da entidade aparecem letra a letra (com o blip), as perguntas sao respondidas com W/S/Enter ou o mouse, e ha efeitos (glitch, pulso vermelho, simbolos, sussurros, batimento, silencio).
2. **Personagem**: perto do fim ele e revelado por um halo azulado sobre o fundo preto, girando devagar. No Forge era a skin do jogador em branco; agora e uma **silhueta chapada** (cabeca, tronco, bracos, pernas) que afina e engorda ao girar. Se quiser a skin real (parada), troque `SILHUETA_PROCEDURAL` para `false` no topo de `TelaMarcado`.
3. **Sombra da criatura** (efeito `shadow`): o feixe de luz varre a tela e a sequencia `sombra` aparece em camera lenta. **Revelacao**: correntes e depois o titulo "VOCE E UM / MARCADO" (sequencias `correntes` e `titulo`).
4. **Saida secreta**: ESC 5 vezes seguidas (no maximo 2,5 s entre eles): fade para o preto, o som para, aparece a frase e abre a criacao.
5. No fim da cena, o log do servidor deve mostrar as respostas gravadas (`PacoteMarcado`) e a `TelaCriacao` abre; ao terminar a criacao, `/ordem resetar` repete tudo.
6. Os sons: se algum nao tocar, confira `sounds.json` e os nomes em `SonsOrdem.NOMES_MARCADO` (etapa 5a).

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
- [~] 6. HUD, teclas e telas (`HudOrdem`, `TeclasOrdem`, `Tela*`, `Cliente*`).
  - [~] 6a. Cliente que nao e tela: entrypoint `OrdemModClienteFabric` agora liga `ReceptoresCliente.registrar()`, `TeclasOrdem.registrar()`, `HudOrdem.registrar()` e `ClienteEntidades.registrar()` (o provisorio da etapa 4 foi apagado); `FonteOP`, `TeclasOrdem` (`KeyMappingHelper`), `AvisoOrdem`, `RolagemHud` e `HudOrdem` (HUD com `HudElementRegistry`, geometria com `fill`/`fillGradient`). `ClienteCriacao`, `ClienteFicha` e `ClienteTeste` entram como PROVISORIOS (so escrevem no log). NAO compilada nem testada.
  - [~] 6b. `TelaTeste` (o dado), `TelaFicha` (menu K), `DesenhoOP` (poligono, linha, halo e skin do jogador; novo), `IconesOP`, `DescricoesOP` e `marcado/VfxMarcado`; `ClienteFicha` e `ClienteTeste` agora abrem as telas de verdade. NAO compilada nem testada. Falta o `InventarioOrdem` (inventario em vidro escuro): usava `ContainerScreenEvent.Render.Background` do Forge, que nao existe no Fabric; vira um mixin em `AbstractContainerScreen` (6b2).
  - [~] 6c. `TelaCriacao` (criacao de personagem, 4 etapas) portada com `DesenhoOP`; `ClienteCriacao` abre ela direto (sem a cena do Marcado, que e a 6d). NAO compilada nem testada.
  - [~] 6d. Cena do Marcado: `TelaMarcado` e `AudioMarcado` portados; `ClienteCriacao.abrir(true)` volta a abrir a `TelaMarcado` (que no fim manda `PacoteMarcado` e abre a `TelaCriacao`). A silhueta branca do personagem virou uma silhueta chapada desenhada na mao (`DesenhoOP.silhueta`; `SILHUETA_PROCEDURAL = false` usa a skin real). `EventosMarcado` e obsoleto (o attachment ja e `copyOnDeath`) e fica fora do build. NAO compilada nem testada.
  - [~] 6e. Tela inicial: `TelaInicial`, `TemaInicial` e `EventosTitulo` portados (o `EventosTitulo` usa `ScreenEvents.AFTER_INIT` do Fabric API e troca o `TitleScreen` no tick seguinte). O botao "Mods" saiu (o Fabric nao tem tela de mods; o rodape mostra a quantidade de mods). NAO compilada nem testada.
  - [~] 6b2. `InventarioOrdem` (inventario e baus em vidro escuro) portado via mixins so de cliente (`mixin/cliente/*`, `ordemmod.client.mixins.json`). Nomes dos metodos do jogo NAO conferidos (config tolerante a falha). NAO compilada nem testada.
  - [ ] 6e2. Visual das telas do jogo (`titulo/EstiloMenu`, `titulo/EstiloTexturas`): fundo de nevoa, botoes de vidro, borda dos campos de texto. FICAM FORA do build: usavam eventos do Forge (ScreenEvent.Render) e trocavam `widgets.png`/`slider.png`, que no 26.x nao sao mais usados (os botoes viraram sprites do atlas gui). O caminho provavel: sprites estaticos em `assets/minecraft/textures/gui/sprites/widget/` + `ScreenEvents` do Fabric API para o fundo. Ate la, Um jogador/Opcoes etc. abrem com o visual original do jogo.
  - Depois da 6e: apagar `cliente/ZumbiSangueModel.java` e conferir os avisos de "Pendencias pequenas".
- [~] 7. Esquiva e rolada sem Epic Fight: `EsquivaVisual` agora da um impulso de verdade (passo para longe do atacante; rolada para a frente) com fumaca e som; sem animacao de corpo (nao ha biblioteca de animacao do jogador no Fabric aqui). `EsquivaEpicFight` e `RolamentoEpicFight` apagados deste branch. Armas: TACZ Fabric (7a, ver `PORTE-TACZ.md`). NAO compilada nem testada.

## Pendencias pequenas
- FEITO (15/10): `font/*-OFL.txt` e `sounds/menu/CREDITOS-MUSICA.txt` renomeados para minusculas (`*-ofl.txt`, `creditos-musica.txt`); o aviso "Invalid path in mod resource-pack" some.
- 4d: `TamableAnimalMixin` so cobre lobo, gato e papagaio; cavalos e afins ainda nao ganham o bonus de Adestramento.
- 4d: a queda (Acrobacia) agora testa quando o dano da queda e >= 1 (queda acima de 3 blocos); no Forge o gatilho era distancia >= 4.
- FEITO: todo o codigo de tela/HUD/Marcado ja usa `SonsOrdem.X` sem `.get()` (conferido por grep em 15/10).
- FEITO (15/10): `cliente/ZumbiSangueModel.java` (obsoleto, GeckoLib 4) apagado; `ZumbiSangueRenderer` o substitui.
- 5b: os arquivos antigos em `assets/ordemmod/geo/` e `animations/` (caminho do GeckoLib 4) so servem para a `main` (Forge); neste branch eles saem de la pelo `git mv`.
- 5b: o arquivo `zumbi_sangue.animation.json` e do formato do GeckoLib 4; o GeckoLib 5 le o mesmo formato Bedrock, mas se alguma animacao nao tocar, o log do GeckoLib avisa o nome. Os nomes usados no codigo sao `animation.zumbi.` + deitado, levantar, idle, andar, correr, ataque_garra, ataque_arrasto, ataque, morte, dano.
- 5c: o ovo nao tem textura propria: usa as texturas padrao do jogo (`item/spawn_egg` e `item/spawn_egg_overlay`) pintadas pelas cores do `tints`, igual ao ovo de qualquer mob de vanilla.

- 6a: `HudOrdem` perdeu o corte inclinado das barras (a constante `INCLINACAO` ja era 0F) e o halo do simbolo do medo deixou de ser aditivo. Se quiser a inclinacao de volta, e preciso um `RenderPipeline` proprio (o `fill` so faz retangulos).
- 6a: a barra de informacao do jogo (`INFO_BAR`) tambem mostra a barra do localizador (waypoints) do 26.x; ela some junto com a de XP enquanto a HUD do mod esta ativa.

## Pontos de risco da 7 (escrita sem compilar)
- `Entity.hurtMarked` (campo publico que manda o movimento ao cliente) e `ServerPlayer.onGround()`; se o build reclamar de `hurtMarked`, troque por `jogador.connection.send(new ClientboundSetEntityMotionPacket(jogador))`.
- `Level.playSound(null, x, y, z, SoundEvent, SoundSource, float, float)` e `SoundEvents.PLAYER_ATTACK_SWEEP`.
- Sensacao: `FORCA_PASSO` (0,55) e `FORCA_ROLADA` (0,40) em `EsquivaVisual`; ajuste vendo no jogo. Se o passo parecer "arrastar", aumente o `SALTINHO`.
- Teste: deixe um zumbi te bater com Reflexos treinado; ao esquivar (painel "Esquiva" com sucesso) voce deve ser empurrado para longe dele. Caia de uns 6 blocos com Acrobacia treinada: ao reduzir a queda voce desliza um pouco para a frente.

## Pontos de risco da 6b2 (inventario, escrita sem compilar)
- Os NOMES dos metodos do jogo sao palpites pela regra render -> extract: `extractBg(GuiGraphicsExtractor, float, int, int)` (em `InventoryScreen` e `ContainerScreen`) e `extractLabels(GuiGraphicsExtractor, int, int)` (em `InventoryScreen` e `AbstractContainerScreen`). Os campos `leftPos`, `topPos`, `imageWidth`, `imageHeight` tambem. `ContainerScreen` (baus) pode ter outro nome no 26.2.
- A config `ordemmod.client.mixins.json` tem `defaultRequire: 0` e `required: false`: se um nome estiver errado, o mixin nao acha o alvo e o inventario fica com o visual do jogo (o log mostra um aviso do Mixin). Um erro de ASSINATURA (parametros diferentes) pode derrubar o jogo ao abrir o inventario: nesse caso o log diz qual metodo; para desligar tudo, tire a linha `ordemmod.client.mixins.json` do `fabric.mod.json`.
- Para conferir os nomes reais: `gradlew genSources` e procurar `extractBg` em `AbstractContainerScreen`/`InventoryScreen` nas fontes geradas.
- Se os textos sairem deslocados: o jogo pode nao mover o desenho para o canto do painel dentro de `extractLabels` (a `InventarioOrdem.frente` assume que move).
- O boneco usa `DesenhoOP.entidade` (nome do metodo do jogo nao conferido, ja listado na 6b).

## Pontos de risco da 6e (escrita sem compilar)
- `ScreenEvents.AFTER_INIT.register((cliente, tela, largura, altura) -> ...)` (pacote `net.fabricmc.fabric.api.client.screen.v1`) e `cliente.gui.screen()/setScreen`.
- `Minecraft.getMusicManager().stopPlaying()` em `TemaInicial.tick` (calar a musica de menu do jogo; o nome do metodo nao foi conferido). Se falhar, apague so essa linha: o tema do mod toca por cima da musica do jogo.
- `SharedConstants.getCurrentVersion().name()` no rodape da `TelaInicial` (pode ser `getName()`).
- `OptionsScreen(Screen, Options)`, `SafetyScreen(Screen)`, `Options.skipMultiplayerWarning`, `Minecraft.allowsMultiplayer()` nos botoes.
- `g.text(font, Component, ...)` (texto letra a letra), blit do simbolo 1024x1024 com tinta.
- Visual: halos nao-aditivos (ficam um pouco mais fracos). Sem EstiloMenu, os submenus (Um jogador, Opcoes) ficam com o visual do jogo.
- Se a tela inicial nao aparecer: veja se o log mostra erro em `EventosTitulo`; para testar sem ela, `SUBSTITUIR_MENU = false`.

## Pontos de risco da 6d (escrita sem compilar)
- `AbstractTickableSoundInstance(SoundEvent, SoundSource, RandomSource)` e os campos `looping`, `delay`, `volume`, `relative`, `attenuation` em `AudioMarcado.Laco` (nao mudaram de nome que eu saiba, mas nao foi conferido no 26.2).
- `Minecraft.getSoundManager().stop(null, SoundSource.MASTER)` (a assinatura com `Identifier` nulo) na saida secreta.
- `font.getSplitter().splitLines(Component, int, Style)` em `mostrar()` e `Screen.mouseMoved(double, double)`/`keyReleased(KeyEvent)`.
- `g.text(font, Component, int, int, int, boolean)` com `Component` (a documentacao so mostra `String`/`Component` de forma geral).
- Visual: os halos deixaram de ser aditivos; sobre o fundo preto da cena ficam iguais, mas sobre a sombra/correntes podem ficar um pouco mais fracos. A silhueta e uma aproximacao (veja acima).

## Pontos de risco da 6c (escrita sem compilar)
- `Screen.shouldCloseOnEsc()` (usado para impedir fechar a criacao com ESC) e `onClose()` apos confirmar: no 26.2 o `onClose` padrao deve fechar a tela via `Gui`; se a tela nao fechar, trocar por `minecraft.gui.setScreen(null)`.
- `SoundEvents.VILLAGER_NO` com `SimpleSoundInstance.forUI(..., 1.0F)` (um `SoundEvent`, nao `Holder`); se reclamar, use `.value()`/o overload `Holder`, como no `UI_BUTTON_CLICK`.
- `g.text(font, FormattedCharSequence, int, int, int, boolean)` e o `DesenhoOP.entidade` (mesmos riscos da 6b).
- `ClienteCriacao` ignora `comMarcado` ate a 6d (o servidor segue mandando `PacoteAbrirCriacao(!concluido)`).

## Pontos de risco da 6b (escrita sem compilar)
- `InventoryScreen.extractEntityInInventoryFollowsMouse(g, x0, y0, x1, y1, tamanho, 0.0625F, mouseX, mouseY, entidade)` em `DesenhoOP.entidade`: a assinatura (caixa + escala + yOffset + mouse) vem do 1.21.11 (`renderEntityInInventoryFollowsMouse` em mappings.dev); o NOME no 26.2 e palpite pela convencao `render*` -> `extract*`. E o unico ponto que chama isso. Se nao compilar, procure no jogo o metodo `static` de `InventoryScreen` que recebe `(GuiGraphicsExtractor, int, int, int, int, int, float, float, float, LivingEntity)`.
- `CompoundTag`: `getIntArray`/`getByteArray` devolvendo `Optional` (usados com `.filter(...).orElseGet(...)`), `getListOrEmpty`, `getDoubleOr` e `ListTag.getCompoundOrEmpty(int)`.
- `KeyMapping.matches(KeyEvent)` (usado em `TelaFicha` e `TelaTeste`) e `Screen.mouseClicked(MouseButtonEvent, boolean)`, `mouseScrolled(double, double, double, double)`, `keyPressed(KeyEvent)`.
- `SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F)` (o som do jogo e um `Holder`; a `TelaTeste` usa a variante com `SoundEvent`).
- `TextureManager.release(Identifier)` em `VfxMarcado.liberarTodos` e o `blit` de 13 argumentos (com regiao e cor) em `VfxMarcado.desenhar`.
- Desempenho: o pentagono e as linhas viram dezenas de `fill` de 1 pixel de altura por quadro; se a ficha ficar pesada, reduzir os aneis em `desenharPentagono` ou trocar por uma textura.
- Visual: o halo e o circulo ritual deixaram de ser aditivos (mistura normal), entao ficam mais fracos; as linhas finas do pentagono podem parecer mais serrilhadas.

## Pontos de risco da 6a (escrita sem compilar)
Fontes: `docs.fabricmc.net` 26.1.2 (rendering/gui-graphics, hud, key-mappings, custom-screens), o codigo do Fabric API (`HudElement`, `HudElementRegistry`, `VanillaHudElements`, `KeyMappingHelper`, `ScreenMouseEvents`, `ScreenKeyboardEvents`) e o primer do NeoForge 26.1 -> 26.2. As assinaturas abaixo NAO foram conferidas no jogo:
- `GuiGraphicsExtractor.text(Font, FormattedCharSequence, int, int, int, boolean)` (so as versoes `String`/`Component` aparecem na documentacao), `item(LivingEntity, ItemStack, int, int, int)` e `itemDecorations(Font, ItemStack, int, int)` (nomes por analogia com o 1.21.x: `renderItem`/`renderItemDecorations`).
- `blit(RenderPipelines.GUI_TEXTURED, textura, x, y, u, v, largura, altura, regionW, regionH, texW, texH, int cor)`: a variante com o ultimo `int` de cor (tinta ARGB) e a que mais pode nao existir; se faltar, tirar a cor (a transparencia do halo/simbolo some) ou usar `blitSprite`.
- `g.pose()` como `Matrix3x2fStack` (`pushMatrix`, `popMatrix`, `translate(float, float)`, `scale(float, float)`), `g.fill` e `g.fillGradient` com `int`.
- `FontDescription.Resource(Identifier)` e `Style.withFont(FontDescription)` (a fonte do mod; se der erro, o jogo mostra os textos na fonte padrao).
- `KeyMapping.Category.register(Identifier)`, o construtor `KeyMapping(String, InputConstants.Type, int, KeyMapping.Category)` e `KeyMappingHelper.registerKeyMapping`: vem da pagina de key mappings do Fabric (26.1.2).
- `Minecraft.gui.screen()` (no 26.2 a tela aberta saiu de `Minecraft` e foi para `Gui`; o primer so diz isso de passagem).
- `Player.getInventory().getSelectedSlot()` e `getItem(int)` (o campo `selected` ficou privado no 1.21.5).
- Ar/montaria: o deslocamento (`ALTURA_PILHA - 49`) supoe que o ar do jogo fica 49 px acima do fim da tela; se as bolhas ficarem coladas na pilha ou longe, ajuste `ALTURA_AR_NO_JOGO`.
- `RenderPipelines` esta em `net.minecraft.client.renderer` (se o import falhar, procurar em `...client.renderer.RenderPipelines` ou `...client.gui.render`).

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
- GUI (6a): `GuiGraphics` virou `GuiGraphicsExtractor` e as telas desenham em `extractRenderState(GuiGraphicsExtractor, int mouseX, int mouseY, float delta)` (nao mais `render`); sempre chame o `super`. `drawString` virou `text(font, texto, x, y, corARGB, sombra)` com x/y INTEIROS e a cor precisa de alfa diferente de 0 (OR com `0xFF000000`). A pilha de matrizes e `Matrix3x2fStack`: `pushPose/popPose` viram `pushMatrix/popMatrix`, `translate(x, y, z)` vira `translate(x, y)` e `scale(x, y, z)` vira `scale(x, y)`. `flush()` e `setColor(...)` nao existem mais (cor/tinta vai como argumento do `blit`). `blit` pede o pipeline: `g.blit(RenderPipelines.GUI_TEXTURED, textura, x, y, u, v, largura, altura, texLargura, texAltura)`. Nao ha mais `RenderSystem`/`Tesselator` para desenhar "na mao": use `fill`, `fillGradient`, `outline` e `blit`.
- Telas (6a, para as proximas etapas): eventos de entrada recebem objetos (`net.minecraft.client.input`): `mouseClicked(MouseButtonEvent, boolean doubleClick)`, `mouseReleased(MouseButtonEvent)`, `mouseDragged(MouseButtonEvent, double dx, double dy)`, `mouseScrolled(double x, double y, double scrollX, double scrollY)`, `keyPressed(KeyEvent)`, `charTyped(CharacterEvent)`. A tela aberta e `minecraft.gui.screen()` e se abre com `minecraft.gui.setScreen(...)`.
- HUD (6a): `RegisterGuiOverlaysEvent`/`RenderGuiOverlayEvent` viram `HudElementRegistry` (Fabric API): `attachElementBefore/After(id, novoId, elemento)`, `replaceElement(id, anterior -> novo)` e `removeElement(id)`; os ids do jogo estao em `VanillaHudElements` (`HEALTH_BAR`, `ARMOR_BAR`, `FOOD_BAR`, `HOTBAR`, `INFO_BAR`, `EXPERIENCE_LEVEL`, `HELD_ITEM_TOOLTIP`, `OVERLAY_MESSAGE`, `AIR_BAR`, `MOUNT_HEALTH`, `CROSSHAIR`...). Um `HudElement` e `(GuiGraphicsExtractor g, DeltaTracker delta) -> ...`.
- Teclas (6a): `RegisterKeyMappingsEvent` vira `KeyMappingHelper.registerKeyMapping(new KeyMapping(chave, InputConstants.Type.KEYSYM, tecla, categoria))` com a categoria criada por `KeyMapping.Category.register(Identifier)` (traducao `key.category.<mod>.<nome>`); `TickEvent.ClientTickEvent` vira `ClientTickEvents.END_CLIENT_TICK`.
- Formas livres (6b): sem `Tesselator`/`BufferBuilder`, poligonos e linhas grossas se desenham por varredura com `fill` (veja `DesenhoOP.poligono`/`linha`); `GuiGraphics.setColor` vira a tinta ARGB do `blit`; brilho aditivo nao existe sem um `RenderPipeline` proprio.
- `net.minecraft.Util` virou `net.minecraft.util.Util` (`Util.getMillis()`); ainda vale corrigir o import em `TelaCriacao`, `TelaFicha`, `TelaTeste`, `titulo/EstiloMenu` e `titulo/TelaInicial` quando forem portadas (6a: o `gradlew build` acusou nas tres classes da HUD).
- Saiu do mundo no cliente (6a): `ClientPlayerNetworkEvent.LoggingOut` vira `ClientPlayConnectionEvents.DISCONNECT`.
- Registro de renderer: o `EntityRenderersEvent.RegisterRenderers` do Forge vira `EntityRendererRegistry.register(tipo, Renderer::new)` (Fabric API, cliente), chamado de `ClienteEntidades.registrar()`.
