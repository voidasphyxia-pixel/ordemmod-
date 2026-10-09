# Regras de trabalho para o Claude neste projeto (ordemmod, Forge 1.20.1)

1. Toda alteracao no mod deve ser registrada em `ALTERACOES-CLAUDE.txt` (raiz do projeto):
   data, o que mudou, arquivos tocados e como testar. Mais recente primeiro.
2. Manter o `LEIA-ME-MARCADO.md` atualizado com o estado da cena do Marcado e o que falta.
3. Antes de os tokens acabarem, enviar ao usuario o arquivo de registro atualizado
   (e fazer commit/push das mudancas) para que outro chat possa continuar de onde parou.
4. Respostas ao usuario em portugues, passo a passo, diretas e logicas.
5. O Claude nao consegue compilar aqui (Forge/Mojang bloqueados): pedir ao usuario o
   resultado de `gradlew build` / `runClient` e conferir APIs do 1.20.1 com cuidado.
