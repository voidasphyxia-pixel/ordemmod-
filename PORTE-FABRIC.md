# Porte para Fabric 26.2

Branch `fabric-26.2`. O Forge 1.20.1 continua intacto na `main`.

## Como testar
1. Instale o **JDK 25**.
2. `./gradlew build` (o Gradle 9.6.1 baixa sozinho pelo wrapper).
3. `./gradlew runClient`: no log deve aparecer `Ordem Mod carregado no Fabric! Nucleo OK`.

Nada disso foi compilado ainda (o build foi montado sem acesso ao Gradle). Se der erro, cole aqui a saida do `gradlew build`.

## Etapas
- [x] 1. Esqueleto: Loom sem remapeamento, `fabric.mod.json`, entrypoint `OrdemMod`.
- [x] 2. Nucleo em Java puro (lista em `build.gradle`, variavel `nucleo`): so esses arquivos compilam por enquanto.
- [ ] 3. Rede (`Rede` + `Pacote*`) e salvamento (`SalvamentoOrdem`).
- [ ] 4. Eventos e pericias em jogo (`EventosOrdem`, `EventosPericias`, `EventosOrigens`, `QuedaAcrobacia`, `EsquivaReflexos`).
- [ ] 5. Entidades e sons (GeckoLib 5.x para 26.2): pasta `entidade`, `SonsOrdem`.
- [ ] 6. HUD, teclas e telas (`HudOrdem`, `TeclasOrdem`, `Tela*`).
- [ ] 7. Esquiva e rolamento sem Epic Fight (`EsquivaEpicFight`, `RolamentoEpicFight`) e armas sem TACZ.

Cada etapa feita: acrescente os arquivos portados na lista `nucleo` do `build.gradle`.
