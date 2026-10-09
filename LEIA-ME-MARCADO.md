# Cena do Marcado — estado do trabalho e como continuar

Última atualização: 08/10/2026. Projeto: mod `ordemmod` (Forge 1.20.1, package `com.exemplo.ordemmod`).

## Onde parou

**Pronto (escrito, mas NUNCA compilado nem testado no jogo — ainda não rodei `gradlew build`):**

| Parte | Arquivo(s) | Situação |
|---|---|---|
| Roteiro em dados | `marcado/RoteiroMarcado.java`, `assets/ordemmod/marcado/marcado.json` | motor pronto; **roteiro REAL da ficha colocado em 09/10/2026** (10 perguntas, 83 passos; textos em `lang/pt_br.json` e `en_us.json`) |
| Persistência (NBT) | `marcado/PerfilMarcado.java`, `marcado/EventosMarcado.java` | pronto: respostas, tags e `concluido`; copiado na morte |
| Rede | `marcado/PacoteMarcado.java`, `Rede.java` (id 10, versão "7"), `PacoteAbrirCriacao.java` (agora leva um boolean) | pronto; o servidor refaz a soma das tags e não confia no cliente |
| Cena (cliente) | `marcado/TelaMarcado.java`, `AudioMarcado.java`, `VfxMarcado.java` | esqueleto jogável: fala letra a letra, perguntas com W/S/Enter/mouse, efeitos, VFX, sons |
| Ligação ao mod | `ClienteCriacao.java`, `EventosOrdem.java`, `SonsOrdem.java`, `sounds.json`, `lang/*.json` | feito |
| Assets | `sounds/marcado/` (19 OGG mono), `textures/gui/marcado/` (5 símbolos + 6 VFX em quadros), `font/entidade.ttf` e `titulo.ttf` + `entidade.json`, `titulo.json` | feito |

**Fluxo:** entrou no mundo sem personagem criado → servidor manda `PacoteAbrirCriacao(comMarcado)` → se a cena ainda não foi vista abre `TelaMarcado`; no fim ela manda `PacoteMarcado` (índice escolhido em cada pergunta) e abre a `TelaCriacao` que o mod já tinha. `/ordem resetar` agora também apaga o perfil do Marcado, então a cena repete.

**HUD de criação (`TelaCriacao`) restilizado em 09/10/2026** na estética do Marcado: névoa/poeira, vinheta, Cinzel (títulos) e Cormorant (textos), painéis translúcidos com cantos dourados e o círculo ritual (`circulo_ritual.png`) atrás do pentágono de atributos. Cores dos símbolos: Energia = roxo, Medo = ciano. Ainda não compilado/testado.

**Saída secreta (09/10/2026):** ESC 5 vezes seguidas (máx. 2,5 s entre eles) durante a cena → fade out, o som para, aparece "Como... você sabe disso?" e abre a `TelaCriacao`. Marca a cena como concluída (perguntas não respondidas = opção 0). Ainda não compilado/testado.

## O que falta (por ordem)

1. **Compilar e corrigir erros.** Escrevi sem poder compilar. Rode `gradlew build` e me mande os erros. Pontos de maior risco:
   `InventoryScreen.renderEntityInInventory` (assinatura com 2 `Quaternionf`), `GuiGraphics.blit/setColor`, `AbstractTickableSoundInstance`, `StringSplitter.splitLines`.
2. ~~Colocar o roteiro real.~~ FEITO (09/10/2026). Revisar os textos no jogo; as reações das opções da pergunta de Percepção (p02) foram escritas pelo Claude (a ficha só dá um exemplo) e podem ser trocadas em `lang/*.json`.
3. **Origem e "O Caminho"** como as 2 últimas perguntas, com as origens sugeridas pelas tags (`PerfilMarcado.tags`). Hoje as tags só são salvas; a `TelaCriacao` ainda não as usa.
4. **Ajustes visuais** só possíveis vendo no jogo: tamanho/posição do personagem, intensidade da névoa/poeira, vinheta, tempo da revelação (correntes → título), volume de cada som, se a Cormorant **Bold** ficou pesada (o arquivo `entidade.ttf` pode ser trocado pela Regular/Medium).
5. **Segurança no singleplayer:** o jogo não pausa durante a cena (o servidor precisa receber o pacote), então o jogador fica vulnerável. Dar invulnerabilidade temporária ou pausar só no cliente.
6. **Anomalia ao entrar no mundo** (figura distante; sons `anomalia_passos` e `anomalia_arranhado` já estão no pacote, sem código ainda).
7. **Fase de aparência** (pele/cabelo/roupa): fora do escopo até aqui.
8. Peso: os VFX somam ~41 MB de PNG (a névoa tem 17 MB). Se pesar, reduzir quadros ou resolução.

## IDs de efeito aceitos no `marcado.json` (campo `efeito`, vários separados por vírgula)

`pulse_red` (Sangue), `glitch` (Energia), `distort` (Conhecimento), `shadow` (Medo), `silence` (Morte: some o som), `heartbeat`, `whisper`, e os símbolos `symbol_conhecimento`, `symbol_sangue`, `symbol_morte`, `symbol_energia`, `symbol_medo` (aparecem ~0,35 s e somem).

Efeitos de controle da cena (adicionados em 09/10/2026): `ambiente` (drone sobe de quase mudo para o volume normal), `particulas_on` / `particulas_off` (névoa e poeira nascem devagar / somem), `silence` (some o som; **só volta com** `som_volta`), `som_volta`, `personagem_revela` (silhueta surge do preto em ~6 s), `aproximar` (zoom lento no personagem), `voz_seria` (texto da entidade muda de cor).

Variação por resposta (09/10/2026): `reacao` aceita várias chaves separadas por `|` (a cena sorteia uma); qualquer passo aceita `"se": "p01=0"` ou `"p01=0|2"` (vírgula = todas valem) e só roda se a resposta bater (0 = 1ª opção). Símbolos: `symbol_<elemento>` (flash forte ao responder, <1 s) e `ghost_<elemento>` (sombra fraca quando a pergunta surge, ~2 s); elementos: conhecimento, sangue, morte, energia, medo. Nunca ficam na tela.

Tipos de passo: `pausa` (espera sem texto e sem mexer no som; `duracao`), `fala` (campos `texto`, `duracao` = pausa depois), `pergunta` (`id`, `texto`, `opcoes[]` com `texto`, `reacao`, `efeito`, `tags`), `silencio` (`duracao`), `revelacao`, `fim`. Todo texto é uma chave de `lang/pt_br.json`.

## Para continuar em outro chat, anexe

1. Este zip completo (`ordemmod_marcado.zip`) e este arquivo.
2. A **ficha de roteiro** do Marcado (texto completo, a versão que veio cortada no item 8 não serve).
3. Os erros do `gradlew build` / `runClient`, se houver, e uma descrição do que apareceu na tela.
4. Prints ou um vídeo da cena rodando, para ajustar o visual (eu não consigo ver o resultado, só o código).
5. Os `.blend` dos VFX só se for preciso refazer algum efeito (as pastas `OrdemVFX\...` no seu PC), junto com o MCP do Blender ligado.

Cole no começo do novo chat: "Continue a cena do Marcado do ordemmod a partir do LEIA-ME-MARCADO.md".

## Sobre este zip
Para ficar leve, ele **não inclui** `libs/` (os .jar de TACZ, Epic Fight e GeckoLib) nem `run/`. Copie esses jars da sua pasta original para `libs/` antes de rodar o Gradle.
