# Armadura de Sangue: do Blender para o mod

Estado (20/10/2026): **versao de teste**. `/ordem armadura` cria um Steve de cubos 3 blocos a sua frente, toca a formacao da
armadura (3,7 s) e ele fica parado na pose final. `/ordem armadura limpar` remove.

## Como foi feito
- A animacao original esta no Blender (`armadura_de_sangue.blend`, 90 quadros a 24 fps).
- `tools/gerar_armadura_sangue.py` refaz a mesma logica em Python e escreve o modelo e a animacao no formato Bedrock que o GeckoLib 5 le.
  Para mudar espessura das placas, comprimento dos espinhos, quantidade de gotas etc.: edite o topo do script e rode
  `python tools/gerar_armadura_sangue.py` (precisa de numpy e Pillow). `--preview` desenha o estado final em /tmp sem gravar nada.
- A semente (`SEED`) fixa o sorteio: mesma semente = mesma armadura.

## O que passou direto e o que foi recriado
| Elemento no Blender | No Minecraft |
|---|---|
| Corpo do Steve, inclinacao, braco levantando | ossos `body`, `head`, `left_arm`, `right_arm` com os mesmos keyframes |
| Placas (46) e subcamadas | 1 osso com 1 cubo cada; entram voando, giram, encaixam, contraem |
| Espinhos (14) | piramide de 3 cubos empilhados; crescem alongando |
| Filamentos (29 curvas) | cadeia de cubos 1 px; cada segmento cresce e depois e absorvido, em ordem |
| Gotas (70) | 1 cubo pequeno cada, com trajetoria por keyframe (puxadas, lancadas, gotejando) |
| Material sangue liquido -> coagulado -> carapaca | **nao existe**: cor fixa por peca (paleta na textura). Para recriar: trocar a textura por tempo, ou duas camadas de cubos |
| Malhas irregulares (vertices deformados) | **perdido**: cubo de Minecraft e reto |
| Iluminacao, brilho especular | e do jogo; nada a exportar |

## Proximo passo: a armadura ficar NO JOGADOR (nao numa entidade separada)
1. **Estado**: guardar no personagem (ja salvo por `SalvamentoOrdem`) `armaduraAtiva` + o tick de ativacao; sincronizar com os outros clientes (`PacoteStatus` ou um pacote novo). Copiar ao morrer.
2. **Desenho**: uma camada de renderizacao do jogador que desenha o mesmo modelo (`armadura_sangue.geo.json`, sem os ossos do Steve) preso aos ossos do jogador (braco, tronco). No GeckoLib 5 isso e uma `GeoRenderLayer`/renderer de armadura para o jogador. As assinaturas do 26.2 precisam ser conferidas no jogo.
3. **Tempo**: o cliente calcula `tempo = agora - tickAtivacao` e toca a animacao `formar` so ate o fim; depois trava no ultimo quadro (e o que `thenPlayAndHold` faz hoje).
4. **Os ossos do Steve** (corpo/braco levantando) saem do modelo da armadura: quem anima o corpo passa a ser o jogador de verdade (ou uma pose de ativacao).
