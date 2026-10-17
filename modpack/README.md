# Modpack (Fabric 26.2) para testar o Ordem Mod

Esta pasta guarda tudo do modpack **que não é `.jar` de outros autores**: configs, shaders, `options.txt` e um script que instala tudo
numa instância do Minecraft junto com o mod Ordem e o TACZ-Refabricated.

## Conteúdo
| Pasta/arquivo | O que é |
|---|---|
| `overrides/config/` | 278 arquivos de config dos mods (sem `.bak`, caches e cache da CrashAssistant) |
| `overrides/shaderpacks/` | Complementary Reimagined 5.9.3, BSL Classic 10.0, Photon 1.3b, Rethinking Voxels, Visual Vibrance 0.3.5a, Sildur's Enhanced Default, Vanilla Plus, I Like Vanilla, Astra Lex 93.0 (+ ajustes `.zip.txt`) |
| `overrides/options.txt` | Suas opções de vídeo/teclas/resourcepacks (veja "Teclas" abaixo) |
| `referencia/modlist-antigo-1.21.5.json` | Lista de mods que a CrashAssistant gravou — é de uma versão **antiga (1.21.5)**, serve só de pista |
| `instalar.ps1` / `instalar.bat` | Copia tudo para a instância e põe o mod Ordem + TACZ em `mods/` |

## Como testar
1. Crie um perfil **Fabric 26.2** (Loader 0.19.3+) na Modrinth App ou no Prism.
2. Na pasta do projeto: `gradlew.bat build` (JDK 25). O jar sai em `build/libs/`. O TACZ-Refabricated precisa estar em `libs/` (veja `PORTE-TACZ.md`).
3. Na pasta `modpack/`: `instalar.bat "C:\caminho\da\instancia\.minecraft"`
4. Coloque na pasta `mods/` da instância os **outros mods** (Fabric API, GeckoLib 5.5.3+, Forge Config API Port e os de qualidade de vida que você usa) — todos precisam existir para **26.2**.
5. Coloque os resourcepacks em `resourcepacks/` (ficam fora do repositório; o `options.txt` já os lista na ordem certa).

## Dependências obrigatórias do Ordem Mod (`fabric.mod.json`)
`fabricloader >= 0.19.3`, `minecraft >= 26.2`, `java >= 25`, `fabric-api`, `geckolib >= 5.5.3`, `tacz >= 1.1.8`
(+ `forgeconfigapiport-fabric`, que o build usa).

## Teclas (conflito resolvido no `options.txt`)
O Ordem usa **R** (usar habilidade), **G** (trocar habilidade) e **K** (menu). Essas teclas estavam em uso por outros mods no seu `options.txt`,
então foram **desvinculadas** (ficam como "Não definida"; dá para religar no menu de Teclas):
- Iris: recarregar shaders (R) e ligar/desligar shaders (K)
- Fancy Toasts: menu de config (K)
- Vanilla: ações rápidas (G); Punchy: inspecionar espada (G)

## O que NÃO está aqui (de propósito)
- `.jar` de outros autores (mods) e o TACZ.
- Resourcepacks (56 MB; `options.txt` já referencia).
- Seus saves e o `soundDevice` (foi limpo do `options.txt` para não prender o áudio ao seu dispositivo).

## Pendências
- Falta a lista **atual** de mods (nomes dos `.jar` da pasta `mods/`). Com ela dá para gerar um `.mrpack`/packwiz de verdade.
- Várias configs vieram de versões anteriores do jogo (1.21.x); o mod que mudou de nome ou foi removido simplesmente ignora o arquivo.
- Possíveis conflitos de mixin do Ordem (telas de inventário e `LivingEntity`) com mods como REI/JEI, Inventory Interactions e Better Combat só aparecem rodando o jogo: se travar, mande o `latest.log`.
