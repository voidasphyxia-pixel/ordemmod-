# TACZ no porte para Fabric 26.2 (etapa 7a)

Jar usado: `TACZ-Refabricated-26_2-1_1_8_fabric_26_2_R3-hotfix2.jar` (id do mod `tacz`, versao `1.1.8+fabric.26.2.R3-hotfix2`).
No Forge o TACZ era dependencia obrigatoria do mod (`mods.toml`, `modId="tacz"`, `[1.1.8,)`). Aqui a regra e a mesma
(`fabric.mod.json`: `"tacz": ">=1.1.8"`).

## O que mudou nesta etapa (sem codigo Java novo)
- `libs/TACZ-Refabricated-26_2-1_1_8_fabric_26_2_R3-hotfix2.jar`: **voce precisa copiar este jar para a pasta `libs/`** (o Claude nao
  consegue enviar arquivo binario ao GitHub). O nome do arquivo esta em `gradle.properties` (`tacz_jar`). Se o arquivo nao existir, o
  `gradlew` mostra um AVISO e o jogo reclama que o mod `tacz` esta faltando.
- `build.gradle`: `implementation files("libs/${tacz_jar}")` e `implementation "fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.2.1"`
  (o TACZ 26.2 exige `forgeconfigapiport >= 26.2.1`), com o maven do autor (`raw.githubusercontent.com/Fuzss/modresources`) so para esse grupo.
- `fabric.mod.json`: `depends` ganhou `tacz`.
- `libs/` ainda tem os jars antigos do Forge 1.20.1 (Epic Fight, GeckoLib 4, TACZ Forge). Eles continuam la porque a `main` usa; neste branch
  nenhum deles e referenciado.

## Requisitos que o jar declara (conferidos no `fabric.mod.json` dele)
Fabric Loader >= 0.19.3 (o projeto usa 0.19.3), Fabric API, Minecraft 26.2, Java >= 25, forgeconfigapiport >= 26.2.1. O jar traz dentro bcel,
commons-math3, luaj e mae (o Fabric Loader carrega). Ele tambem declara que "provides" `lrtactical` e `taczmeshloader`.

## Como as armas entram no sistema de pericias (conferido no bytecode do jar, NAO no jogo)
- A bala (`EntityKineticBullet`) cria o dano com `ModDamageTypes.Sources` (tipos `tacz:bullet`, `bullet_ignore_armor`, e as versoes `void`), com a
  **bala como entidade direta** e o atirador como causador, e aplica com `Entity.hurt(DamageSource, float)`.
- No `EventosDano.aoSofrerDano`, `fonte.getDirectEntity() != atacante` vira `TipoDano.A_DISTANCIA`. Entao tiro de TACZ usa o multiplicador de
  Pontaria, os bonus de habilidades a distancia e o critico, sem mudar codigo.
- O gancho do mod esta em `actuallyHurt` (`LivingEntityMixin`). O TACZ tambem tem mixin em `actuallyHurt` (evento proprio dele, depois da
  armadura); os dois podem coexistir, mas e o primeiro ponto a olhar se o dano ficar estranho.
- Importante: o dano do mod e aplicado ANTES da armadura (diferenca ja conhecida em relacao ao Forge). O tipo `bullet_ignore_armor` do TACZ
  ignora a armadura do alvo de qualquer forma.

## Como testar (NAO compilado nem testado)
1. Copie o jar do TACZ para `libs/`. `gradlew build`. Se o Gradle nao achar `fuzs.forgeconfigapiport:forgeconfigapiport-fabric:26.2.1`
   (nao consegui confirmar o nome exato do artefato/versao no maven do autor), baixe o jar da versao 26.2.1 (Fabric) no Modrinth, ponha em
   `libs/` e troque a linha por `implementation files('libs/<nome-do-jar>.jar')`. Cole aqui o erro se der.
2. `gradlew runClient`. No log o `tacz` e o `forgeconfigapiport` devem aparecer na lista de mods; se o Fabric Loader recusar alguma dependencia,
   cole o trecho.
3. Mundo criativo: pegue uma arma na aba criativa do TACZ e atire num zumbi (e em um boneco de treino, se houver).
   - `/ordem pericias` para ver os ids; `/ordem treinar <id da pontaria>` e compare o dano antes e depois.
   - Mate um mob com tiro: deve aparecer `CRITICO!` de vez em quando e o dano deve subir com o grau de Pontaria.
4. Se for atingido por um jogador com arma de fogo (multijogador), o jogador atingido deve passar pelo calculo de defesa normal.

## O que NAO foi feito (de proposito)
- Nenhuma chamada a API do TACZ no codigo do mod. Se mais tarde for preciso (ex.: dano por parte do corpo, tiro na cabeca, municao como item de
  inventario do RPG), os eventos ficam em `com.tacz.guns.api.event.common` (`EntityHurtByGunEvent`, `GunShootEvent`, `GunFireEvent`, ...).
- HUD: o TACZ desenha a propria mira/municao. Isso so importa na etapa 6 (HudOrdem); ver como convivem quando a HUD for portada.
- Epic Fight continua fora (etapa 7, `EsquivaEpicFight` / `RolamentoEpicFight`).

## Trecho para colar no topo do ALTERACOES-CLAUDE.txt (regra 1 do CLAUDE.md)
```
--- 14/10/2026 (porte Fabric: TACZ) ------------------------------------------
Dependencia do TACZ Fabric 26.2 no build e no fabric.mod.json
  Arquivos: build.gradle, gradle.properties, src/main/resources/fabric.mod.json, PORTE-TACZ.md (novo)
  - Branch fabric-26.2. Nenhum codigo Java mudou. libs/TACZ-Refabricated-26_2-1_1_8_fabric_26_2_R3-hotfix2.jar precisa ser copiado pelo usuario.
  - O tiro do TACZ passa pelo gancho de dano existente (A_DISTANCIA): conferido so no bytecode do jar.
  - Como testar: PORTE-TACZ.md. Nao compilado/testado no jogo pelo Claude.
```
