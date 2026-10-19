package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * Os slots de habilidades ativas no canto inferior esquerdo da tela: PENTAGONOS (ponta para cima) empilhados na
 * vertical, no estilo dos slots hexagonais de outro HUD, so que cada um mostra o simbolo da habilidade COBRINDO a forma
 * inteira. De cima para baixo:
 *  - PROXIMA (menor): a que a tecla TROCAR (G) escolhe;
 *  - SELECIONADA (no meio, grande): a que a tecla USAR (R) dispara agora;
 *  - ANTERIOR (menor, mais apagada): a que estava antes dela.
 * A fila e circular (depois da ultima volta a primeira), igual a Personagem.proximaHabilidade(). Com 1 habilidade so
 * aparece a do meio; com 2, so a do meio e a de cima.
 * Se o PE atual nao paga o custo, o icone escurece e a borda fica vermelha ("nao da para ativar agora").
 * O numero no canto de baixo e o custo em PE (como o "6" no slot do print de referencia).
 *
 * Como o pentagono e desenhado (suave, sem degraus): cada slot sao TEXTURAS com bordas antialiased, geradas por
 * tools/gerar_pentagonos.py em textures/gui/hud_habilidades/ — "<id>.png" (fundo escuro + icone ja recortado no
 * pentagono), "moldura.png" (anel branco, pintado com a cor da borda) e "brilho.png" (halo dourado do slot atual).
 * Cada PNG tem um .mcmeta com blur=true, entao o jogo filtra de forma suave ao reduzir para o tamanho da tela.
 * Se um icone mudar em poderes/, rode o script de novo.
 *
 * Todas as medidas estao nas constantes abaixo para voce ajustar (tamanho, margem, deslocamento diagonal).
 * Chamado de HudOrdem.desenhar; so existe no cliente.
 */
public final class HabilidadesHud {
    private HabilidadesHud() {
    }

    // ------------------------------------------------------------------ medidas (pixels da interface)
    /** Raio do pentagono grande (atual) e dos menores (fila). Largura = 1,90 * raio, altura = 1,81 * raio. */
    private static final float RAIO_ATUAL = 17F;
    private static final float RAIO_FILA = 12.5F;
    /** Distancia da borda esquerda e da borda de baixo da tela ate o bloco de slots. */
    private static final int MARGEM_ESQ = 8;
    private static final int MARGEM_BAIXO = 10;
    /** Vao vertical entre um pentagono e o seguinte. */
    private static final int VAO = 3;
    /** Quanto o slot selecionado (do meio) fica deslocado para a direita em relacao aos de cima e de baixo (arco espelhado). */
    private static final int DESLOCA_X = 9;
    /** Papel de cada slot na pilha (de cima para baixo: proxima, atual, anterior). */
    private static final int PAPEL_ATUAL = 0;
    private static final int PAPEL_PROXIMA = 1;
    private static final int PAPEL_ANTERIOR = 2;

    private static final int COR_OSSO = 0xFFEDE8DC;
    private static final int COR_DOURADO = 0xFFC2B48C;
    private static final int COR_SEM_PE = 0xFFB3261E;
    private static final int COR_BORDA_FILA = 0xFF6A6A78;
    private static final int COR_HALO = 0x99000000;
    private static final float ESCALA_CUSTO = 0.80F;
    /** Nome da habilidade selecionada, ao lado do pentagono do meio: escala da letra, largura maxima e vao ate a forma. */
    private static final float ESCALA_NOME = 0.72F;
    private static final int NOME_LARGURA_MAX = 84;
    private static final float NOME_VAO = 5F;

    // ------------------------------------------------------------------ animacoes (so o slot do meio)
    /** USO: "pop" de escala + clarao dourado que cresce e some. Duracao em ms e quanto o pentagono incha no inicio. */
    private static final float DUR_USO = 380F;
    private static final float POP_USO = 0.16F;
    /** SEM PE: tremida horizontal amortecida + flash vermelho. Duracao em ms, amplitude (px) e numero de balancos. */
    private static final float DUR_FALHA = 450F;
    private static final float TREMOR_PX = 3F;
    private static final float TREMOR_CICLOS = 3F;
    private static final int COR_FLASH_FALHA = 0xFFFF3B30;
    /** Volume dos dois sons (1 = o do arquivo). */
    private static final float VOLUME_SOM = 0.8F;

    /** Momento (Util.getMillis) em que cada animacao comecou; -1 = nenhuma em andamento. */
    private static long inicioUso = -1L;
    private static long inicioFalha = -1L;
    /** Custo e PE do slot selecionado, guardados a cada quadro para a tecla saber se deu para usar. */
    private static int custoSelecionada = -1;
    private static int peUltimo = 0;

    /**
     * A tecla USAR (R) foi apertada: dispara a animacao de uso (se o PE paga o custo da selecionada) ou a de "sem PE".
     * E uma previsao do cliente (o servidor confere de novo); chamada de TeclasOrdem.
     */
    public static void aoApertarUsar() {
        if (custoSelecionada < 0) {
            return; // sem habilidades ativas na HUD
        }
        if (peUltimo >= custoSelecionada) {
            inicioUso = Util.getMillis();
            // o jogo ja sorteia 1 de 3 gravacoes (sounds.json); o pitch varia um pouco mais para nao soar igual a cada uso
            tocar(SonsOrdem.HABILIDADE_USAR, 0.95F + 0.1F * (float) Math.random(), VOLUME_SOM);
        } else {
            inicioFalha = Util.getMillis();
            tocar(SonsOrdem.HABILIDADE_SEM_PE, 0.97F + 0.06F * (float) Math.random(), VOLUME_SOM);
        }
    }

    /** Toca um som de interface (nao vem de um ponto do mundo, entao ouve-se igual em qualquer lugar). */
    private static void tocar(SoundEvent som, float pitch, float volume) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(som, pitch, volume));
    }

    /** Progresso 0..1 de uma animacao que comecou em {@code inicio}; 1 = terminou (ou nunca comecou). */
    private static float progresso(long inicio, float duracao) {
        if (inicio < 0L) {
            return 1F;
        }
        return Mth.clamp((Util.getMillis() - inicio) / duracao, 0F, 1F);
    }

    /** Mistura duas cores ARGB (t = 0 da {@code a}, t = 1 da {@code b}). */
    private static int misturar(int a, int b, float t) {
        int r = Math.round(Mth.lerp(t, (a >> 16) & 0xFF, (b >> 16) & 0xFF));
        int gr = Math.round(Mth.lerp(t, (a >> 8) & 0xFF, (b >> 8) & 0xFF));
        int bl = Math.round(Mth.lerp(t, a & 0xFF, b & 0xFF));
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    // ------------------------------------------------------------------ texturas (tamanhos = os PNGs gerados)
    private static final int TEX_W = 192;
    private static final int TEX_H = 183;
    private static final int BRILHO_LADO = 256;
    /** O PNG do brilho e um quadrado de lado 3,2 * raio, centrado no circuncentro (o pentagono dele tem ~1,15 raio). */
    private static final float BRILHO_REL = 3.2F;

    private static final Identifier MOLDURA = textura("moldura");
    private static final Identifier BRILHO = textura("brilho");
    private static final Identifier GENERICO = textura("generico");
    private static final Map<String, Identifier> CACHE = new HashMap<>();

    private static Identifier textura(String nome) {
        return Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "textures/gui/hud_habilidades/" + nome + ".png");
    }

    /** O icone ja recortado do poder; se o PNG nao foi gerado, usa o generico. */
    private static Identifier iconeDe(String id) {
        Identifier achada = CACHE.get(id);
        if (achada == null) {
            Identifier candidata = textura(id);
            boolean existe = Minecraft.getInstance().getResourceManager().getResource(candidata).isPresent();
            achada = existe ? candidata : GENERICO;
            CACHE.put(id, achada);
        }
        return achada;
    }

    // ------------------------------------------------------------------ geometria

    private static final float COS18 = (float) Math.cos(Math.toRadians(18));
    private static final float COS36 = (float) Math.cos(Math.toRadians(36));

    private static float larguraDe(float r) {
        return 2F * r * COS18;
    }

    private static float alturaDe(float r) {
        return r * (1F + COS36);
    }

    // ------------------------------------------------------------------ desenho

    /** Desenha o bloco de pentagonos. {@code pe} e o PE atual, para saber quais a pessoa consegue pagar agora. */
    public static void desenhar(GuiGraphicsExtractor g, Minecraft mc, int altura, List<PacoteStatus.Slot> fila,
            int selecionada, int pe) {
        int n = fila.size();
        if (n == 0) {
            return; // sem habilidades ativas, nao aparece nada
        }
        int sel = Mth.clamp(selecionada, 0, n - 1);
        custoSelecionada = fila.get(sel).custo();
        peUltimo = pe;
        float tUso = progresso(inicioUso, DUR_USO);
        float tFalha = progresso(inicioFalha, DUR_FALHA);
        float pulso = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 400.0);

        // de cima para baixo: PROXIMA, SELECIONADA (no meio, grande), ANTERIOR. Com 1 habilidade so aparece a do meio;
        // com 2, a "proxima" e a "anterior" sao a mesma, entao so a de cima aparece.
        int[] indice = new int[3];
        int[] papel = new int[3];
        int quantos = 0;
        if (n >= 2) {
            indice[quantos] = (sel + 1) % n;
            papel[quantos++] = PAPEL_PROXIMA;
        }
        indice[quantos] = sel;
        papel[quantos++] = PAPEL_ATUAL;
        if (n >= 3) {
            indice[quantos] = (sel - 1 + n) % n;
            papel[quantos++] = PAPEL_ANTERIOR;
        }

        // calculo a altura total primeiro, para o bloco ficar rente a margem de baixo
        float[] raio = new float[quantos];
        float total = 0F;
        for (int i = 0; i < quantos; i++) {
            raio[i] = papel[i] == PAPEL_ATUAL ? RAIO_ATUAL : RAIO_FILA;
            total += alturaDe(raio[i]) + (i > 0 ? VAO : 0);
        }
        float y = altura - MARGEM_BAIXO - total;
        float larguraAtual = larguraDe(RAIO_ATUAL);

        for (int i = 0; i < quantos; i++) {
            PacoteStatus.Slot slot = fila.get(indice[i]);
            float r = raio[i];
            boolean atual = papel[i] == PAPEL_ATUAL;
            // arco espelhado: a selecionada (meio) avanca DESLOCA_X para a direita; a de cima e a de baixo ficam mais a esquerda
            float cx = MARGEM_ESQ + larguraAtual / 2F + (atual ? DESLOCA_X : 0F);
            if (atual && tFalha < 1F) {
                // tremida: seno amortecido (o slot inteiro, com custo e nome, balanca de um lado para o outro)
                cx += TREMOR_PX * (1F - tFalha) * (float) Math.sin(tFalha * TREMOR_CICLOS * 2.0 * Math.PI);
            }
            float cy = y + r; // a ponta fica em y e o centro (circuncentro) r abaixo dela
            boolean paga = pe >= slot.custo();
            // a proxima fica um pouco apagada e a anterior mais ainda
            float brilho = (paga ? 1F : 0.38F) * (atual ? 1F : (papel[i] == PAPEL_PROXIMA ? 0.85F : 0.65F));
            int corBorda = !paga ? COR_SEM_PE : atual ? COR_OSSO : COR_BORDA_FILA;
            // animacoes so no slot do meio: 1 = no auge do efeito, 0 = sem efeito
            float fUso = atual ? 1F - tUso : 0F;
            float fFalha = atual ? 1F - tFalha : 0F;
            pentagono(g, slot.id(), cx, cy, r, corBorda, brilho, pulso, atual && paga, fUso, fFalha);
            // o custo fica no canto de baixo-direita (perto do vertice inferior direito, em cx + 0,59 r)
            custo(g, mc.font, slot.custo(), cx + 0.59F * r + 4F, y + alturaDe(r) + 1F, paga);
            if (atual) {
                nome(g, mc.font, slot.nome(), cx + larguraDe(r) / 2F + NOME_VAO, cy, paga);
            }
            y += alturaDe(r) + VAO;
        }
    }

    /** Um pentagono completo: brilho por tras (so o atual), icone com fundo (ja recortado) e a moldura por cima. */
    private static void pentagono(GuiGraphicsExtractor g, String idIcone, float cx, float cy, float r, int corBorda,
            float brilho, float pulso, boolean brilha, float fUso, float fFalha) {
        // 1) brilho suave em volta do atual que da para usar
        if (brilha) {
            int lado = Math.round(r * BRILHO_REL);
            int alfa = 0x55 + Math.round(0x44 * pulso);
            blit(g, BRILHO, Math.round(cx - lado / 2F), Math.round(cy - lado / 2F), lado, lado, BRILHO_LADO,
                    BRILHO_LADO, (alfa << 24) | (COR_DOURADO & 0xFFFFFF));
        }
        // 1b) USO: clarao dourado que cresce e some (atras do icone)
        if (fUso > 0F) {
            float crescer = 1F + 0.45F * (1F - fUso);
            int lado = Math.round(r * BRILHO_REL * crescer);
            int alfa = Math.round(0xDD * fUso * fUso);
            blit(g, BRILHO, Math.round(cx - lado / 2F), Math.round(cy - lado / 2F), lado, lado, BRILHO_LADO,
                    BRILHO_LADO, (alfa << 24) | 0xFFE9A8);
        }
        // 1c) SEM PE: clarao vermelho (fixo, so esmaece)
        if (fFalha > 0F) {
            int lado = Math.round(r * BRILHO_REL * 0.95F);
            int alfa = Math.round(0xCC * fFalha);
            blit(g, BRILHO, Math.round(cx - lado / 2F), Math.round(cy - lado / 2F), lado, lado, BRILHO_LADO,
                    BRILHO_LADO, (alfa << 24) | (COR_FLASH_FALHA & 0xFFFFFF));
        }
        // 2) caixa do pentagono: largura x altura, com a ponta em cy - r. No USO ela "incha" em volta do centro da caixa.
        float escala = 1F + POP_USO * fUso * fUso;
        int w = Math.round(larguraDe(r) * escala);
        int h = Math.round(alturaDe(r) * escala);
        int x = Math.round(cx - w / 2F);
        int y = Math.round(cy - r + alturaDe(r) / 2F - h / 2F);
        // 3) icone + fundo (o PNG ja vem recortado no pentagono interno); o escurecimento vai como tinta. Na falha o
        //    canal vermelho sobe e os outros ficam como estavam, entao o icone pisca em vermelho.
        int v = Mth.clamp(Math.round(brilho * 255F), 0, 255);
        int vr = Mth.clamp(Math.round(Math.max(brilho, 0.85F * fFalha) * 255F), 0, 255);
        blit(g, iconeDe(idIcone), x, y, w, h, TEX_W, TEX_H, 0xFF000000 | (vr << 16) | (v << 8) | v);
        // 4) moldura por cima, com a cor da borda (branca no auge do uso, vermelho vivo no auge da falha)
        int cor = corBorda;
        if (fUso > 0F) {
            cor = misturar(corBorda, 0xFFFFFFFF, fUso);
        }
        if (fFalha > 0F) {
            cor = misturar(corBorda, COR_FLASH_FALHA, fFalha);
        }
        blit(g, MOLDURA, x, y, w, h, TEX_W, TEX_H, cor);
    }

    /** Textura inteira esticada em w x h (o filtro suave vem do .mcmeta de cada PNG). */
    private static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, int texW, int texH,
            int tinta) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0F, 0F, w, h, texW, texH, texW, texH, tinta);
    }

    /** O nome da habilidade selecionada, a direita do pentagono do meio e centrado na vertical dele (cortado se for longo). */
    private static void nome(GuiGraphicsExtractor g, Font fonte, String nome, float x, float yCentro, boolean paga) {
        String texto = fonte.substrByWidth(FonteOP.rotulo(nome), Math.round(NOME_LARGURA_MAX / ESCALA_NOME)).getString();
        float y = yCentro - 4F * ESCALA_NOME; // a fonte tem ~8 px de altura
        int cor = paga ? COR_OSSO : 0xFFC9A09A;
        float[][] halo = { { -0.9F, 0F }, { 0.9F, 0F }, { 0F, -0.9F }, { 0F, 0.9F } };
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(ESCALA_NOME, ESCALA_NOME);
        for (float[] o : halo) {
            FonteOP.desenharRotulo(g, fonte, texto, o[0], o[1], COR_HALO, false);
        }
        FonteOP.desenharRotulo(g, fonte, texto, 0F, 0F, cor, false);
        g.pose().popMatrix();
    }

    /** O custo em PE no canto de baixo-direita do slot, com halo escuro para ler sobre qualquer icone. */
    private static void custo(GuiGraphicsExtractor g, Font fonte, int custo, float xDir, float yBase, boolean paga) {
        String texto = String.valueOf(custo);
        float w = FonteOP.largura(fonte, texto) * ESCALA_CUSTO;
        float x = xDir - w;
        float y = yBase - 8F * ESCALA_CUSTO;
        int cor = paga ? 0xFFFFE066 : 0xFFFF7A70;
        float[][] halo = { { -0.9F, 0F }, { 0.9F, 0F }, { 0F, -0.9F }, { 0F, 0.9F } };
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(ESCALA_CUSTO, ESCALA_CUSTO);
        for (float[] o : halo) {
            FonteOP.desenhar(g, fonte, texto, o[0], o[1], COR_HALO, false);
        }
        FonteOP.desenhar(g, fonte, texto, 0F, 0F, cor, false);
        g.pose().popMatrix();
    }
}
