# Modpack (Fabric 26.2) para testar o Ordem Mod

Esta pasta guarda tudo do modpack **que não é `.jar` de outros autores**: configs, shaders, `options.txt` e um script que instala tudo
numa instância do Minecraft junto com o mod Ordem e o TACZ-Refabricated.

## Conteúdo
| Pasta/arquivo | O que é |
|---|---|
| `overrides/config/` | 278 arquivos de config dos mods (sem `.bak`, caches e cache da CrashAssistant) |
| `overrides/shaderpacks/` | Complementary Reimagined 5.9.3, BSL Classic 10.0, Photon 1.3b, Rethinking Voxels, Visual Vibrance 0.3.5a, Sildur's Enhanced Default, Vanilla Plus, I Like Vanilla, Astra Lex 93.0 (+ ajustes `.zip.txt`) |
| `overrides/options.txt` | Suas opções de vídeo/teclas/resourcepacks (veja "Teclas" abaixo) |
| `mods-lista.txt` | Lista dos 103 mods (todos 26.2) do seu modpack atual, só os nomes dos `.jar` |
| `instalar.ps1` / `instalar.bat` | Copia tudo para a instância e põe o mod Ordem + TACZ em `mods/` |

## Como testar (os mods e resourcepacks já estão em `overrides/`)
1. `git pull` na sua pasta do projeto.
2. Coloque o jar do **TACZ-Refabricated** em `libs/` e rode `gradlew.bat build` (JDK 25). O jar do mod sai em `build/libs/`.
3. Crie um perfil **Fabric 26.2** (Loader 0.19.3+) e abra a pasta `.minecraft` dele.
4. Em `modpack/`: `instalar.bat "C:\...\.minecraft"` (copia tudo + o jar do Ordem + o TACZ; o automodpack é removido).
5. Coloque na pasta `mods/` da instância o **GeckoLib 5.5.3+ (26.2)** — ele não está no pack.
6. Abra o jogo, crie um mundo e teste `/ordem status` e as teclas R, G e K. Se travar, mande o `logs/latest.log`.

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
- O TACZ e o GeckoLib (não estão no pack; veja acima) e o `automodpack`.
- Seus saves e o `soundDevice` (foi limpo do `options.txt` para não prender o áudio ao seu dispositivo).

## Pendências
- Gerar um `.mrpack`/packwiz de verdade (com links e hashes) precisa de acesso ao Modrinth, que está bloqueado onde eu trabalho; faça pelo Modrinth App (Exportar) depois que o teste passar.
- Possíveis conflitos de mixin do Ordem (telas de inventário e `LivingEntity`) com mods como Punchy, Not Enough Animations, Controlling, Bedrockify e BetterF3 só aparecem rodando o jogo: se travar, mande o `latest.log`.
