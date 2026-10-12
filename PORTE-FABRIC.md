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

Para testar os comandos (etapa 4c), no mundo, abra o chat com `T` e digite (o personagem precisa estar num mundo com trapacas ligadas ou ser o dono):
- `/ordem status`, `/ordem nex` (repita ate subir de nivel), `/ordem habilidades`
- `/ordem aprender <id>` (o chat sugere os ids com Tab), `/ordem usar <id>`
- `/ordem san 5`, `/ordem trilha`, `/ordem resetar`

As etapas 3 e 4a foram compiladas e testadas no jogo. A 4c foi escrita sem compilar. Se der erro, cole aqui a saida do `gradlew build`.

## Etapas
- [x] 1. Esqueleto: Loom sem remapeamento, `fabric.mod.json`, entrypoint `OrdemMod`.
- [x] 2. Nucleo em Java puro (lista em `build.gradle`, variavel `nucleo`).
- [x] 3. Rede e salvamento (pacotes como payloads, `Rede.registrarTipos()`, Data Attachment). Testado.
- [~] 4. Eventos e pericias em jogo.
  - [x] 4a. Ciclo de vida do personagem em `EventosOrdem` (entrar, tick, sair, renascer, tecla de habilidade, status para a HUD), `Avisos`, `TesteNaTela`, `PerfilMarcado` (agora com attachment), `ReceptoresServidor` registrado. Testado (salvar e recarregar, status e aviso chegam ao cliente).
  - [ ] 4b. Dano e critico, resistencia a efeitos, dormir, estado mental (efeitos da sanidade baixa). Codigo atual na `main` (`EventosOrdem.java`: `aoSofrerDano`, `aoReceberEfeito`, `aoAcordar`, `aplicarEstadoMental`).
  - [~] 4c. Comandos `/ordem ...` portados para `EventosOrdem.registrarComandos` (`CommandRegistrationCallback`). Escrito, ainda nao compilado nem testado.
  - [ ] 4d. `EventosPericias`, `EventosOrigens`, `QuedaAcrobacia`, `EsquivaReflexos`, `ReceptorMarcado` (precisa do `RoteiroMarcado`).
- [ ] 5. Entidades e sons (GeckoLib 5.x para 26.2): pasta `entidade`, `SonsOrdem`.
- [ ] 6. HUD, teclas e telas (`HudOrdem`, `TeclasOrdem`, `Tela*`, `Cliente*`). Aqui trocar o `cliente/OrdemModClienteFabric` (provisorio) por `ReceptoresCliente.registrar()` e apagar o provisorio.
- [ ] 7. Esquiva e rolamento sem Epic Fight (`EsquivaEpicFight`, `RolamentoEpicFight`) e armas sem TACZ.

## O que muda ao portar o resto
- `Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador), pacote)` vira `Rede.paraJogador(jogador, pacote)`.
- `Rede.CANAL.sendToServer(pacote)` vira `ReceptoresCliente.enviar(pacote)`.
- `PacoteFicha.enviar(jogador)` virou `ReceptoresServidor.enviarFicha(jogador)`.
- `jogador.getPersistentData()` nao existe mais: usar attachments (`AnexosOrdem`). Ainda pode haver uso em `EventosPericias`/`EventosOrigens`.
- Eventos do Forge (`@SubscribeEvent`) viram callbacks do Fabric registrados num metodo `registrar()` (veja `EventosOrdem`). Eventos de dano/efeito do Forge nao tem equivalente identico no Fabric: a parte 4b vai precisar de mixin ou de eventos como `ServerLivingEntityEvents`.
- `AttributeModifier` agora usa `Identifier` (nao `UUID`) e a operacao `ADD_MULTIPLIED_BASE` (era `MULTIPLY_BASE`).
- NBT na 1.21.5+: os getters devolvem `Optional`. Use `getIntOr`, `getStringOr`, `getBooleanOr`, `getCompoundOrEmpty`, `getListOrEmpty`. `getAllKeys()` mudou de nome. Vale para a `TelaFicha`, que le o CompoundTag do `PacoteFicha`.
- Os receptores do Fabric ja rodam na thread certa: sem `enqueueWork` e sem `DistExecutor`.
- `ResourceLocation` virou `Identifier` na 26.x.
- `ChatFormatting.getColor()` nao existe mais na 26.2: use `Avisos.rgbDe(cor)`.
- `Player.displayClientMessage(texto, true)` virou `sendOverlayMessage(texto)` (e `sendSystemMessage(texto)` para o chat).
- `RegisterCommandsEvent` vira `CommandRegistrationCallback.EVENT` (`net.fabricmc.fabric.api.command.v2`).
