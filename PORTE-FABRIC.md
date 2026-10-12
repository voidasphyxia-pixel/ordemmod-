# Porte para Fabric 26.2

Branch `fabric-26.2`. O Forge 1.20.1 continua intacto na `main`.

## Como testar
1. Instale o **JDK 25**.
2. `gradlew.bat build` (ou `./gradlew build`).
3. `gradlew.bat runClient`: no log deve aparecer:
   - `Ordem Mod carregado no Fabric! Nucleo OK ...`
   - `Ordem Mod etapa 3: salvamento OK, pacote OK.`

A etapa 3 foi escrita sem compilar. Se der erro, cole aqui a saida do `gradlew build`.

## Etapas
- [x] 1. Esqueleto: Loom sem remapeamento, `fabric.mod.json`, entrypoint `OrdemMod`.
- [x] 2. Nucleo em Java puro (lista em `build.gradle`, variavel `nucleo`).
- [~] 3. Rede e salvamento.
  - Feito e na lista de compilacao: os 11 pacotes como payloads (`TYPE` + `CODEC`), `Rede.registrarTipos()`, `SalvamentoOrdem` com Data Attachment (`AnexosOrdem`), teste de fumaca no `OrdemMod`.
  - Escrito mas FORA da lista de compilacao (dependem de classes ainda em Forge): `ReceptoresServidor`, `ReceptoresCliente`, `marcado/ReceptorMarcado`.
- [ ] 4. Eventos e pericias em jogo (`EventosOrdem`, `EventosPericias`, `EventosOrigens`, `QuedaAcrobacia`, `EsquivaReflexos`). Aqui entram `Avisos`, `TesteNaTela`, `CriacaoPersonagem`, `PerfilMarcado`; depois chamar `ReceptoresServidor.registrar()` e `ReceptorMarcado.registrar()` no `OrdemMod`.
- [ ] 5. Entidades e sons (GeckoLib 5.x para 26.2): pasta `entidade`, `SonsOrdem`.
- [ ] 6. HUD, teclas e telas (`HudOrdem`, `TeclasOrdem`, `Tela*`, `Cliente*`). Aqui criar o entrypoint `client` no `fabric.mod.json` e chamar `ReceptoresCliente.registrar()`.
- [ ] 7. Esquiva e rolamento sem Epic Fight (`EsquivaEpicFight`, `RolamentoEpicFight`) e armas sem TACZ.

## O que muda ao portar o resto (anotado na etapa 3)
- `Rede.CANAL.send(PacketDistributor.PLAYER.with(() -> jogador), pacote)` vira `Rede.paraJogador(jogador, pacote)`.
- `Rede.CANAL.sendToServer(pacote)` vira `ReceptoresCliente.enviar(pacote)`.
- `PacoteFicha.enviar(jogador)` virou `ReceptoresServidor.enviarFicha(jogador)`.
- `jogador.getPersistentData()` nao existe mais: usar attachments (`AnexosOrdem`). Ainda tem uso em `PerfilMarcado` e provavelmente em `EventosOrdem`/`EventosPericias`.
- NBT na 1.21.5+: os getters devolvem `Optional`. Use `getIntOr`, `getStringOr`, `getBooleanOr`, `getCompoundOrEmpty`, `getListOrEmpty`. `getAllKeys()` mudou de nome. Vale para a `TelaFicha`, que le o CompoundTag do `PacoteFicha`.
- Os receptores do Fabric ja rodam na thread certa: sem `enqueueWork` e sem `DistExecutor`.
- `ResourceLocation` virou `Identifier` na 26.x.
