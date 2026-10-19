package com.exemplo.ordemmod;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
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
            float cy = y + r; // a ponta fica em y e o centro (circuncentro) r abaixo dela
            boolean paga = pe >= slot.custo();
            // a proxima fica um pouco apagada e a anterior mais ainda
            float brilho = (paga ? 1F : 0.38F) * (atual ? 1F : (papel[i] == PAPEL_PROXIMA ? 0.85F : 0.65F));
            int corBorda = !paga ? COR_SEM_PE : atual ? COR_OSSO : COR_BORDA_FILA;
            pentagono(g, slot.id(), cx, cy, r, corBorda, brilho, pulso, atual && paga);
            // o custo fica no canto de baixo-direita (perto do vertice inferior direito, em cx + 0,59 r)
            custo(g, mc.font, slot.custo(), cx + 0.59F * r + 4F, y + alturaDe(r) + 1F, paga);
            y += alturaDe(r) + VAO;
        }
    }

    /** Um pentagono completo: brilho por tras (so o atual), icone com fundo (ja recortado) e a moldura por cima. */
    private static void pentagono(GuiGraphicsExtractor g, String idIcone, float cx, float cy, float r, int corBorda,
            float brilho, float pulso, boolean brilha) {
        // 1) brilho suave em volta do atual que da para usar
        if (brilha) {
            int lado = Math.round(r * BRILHO_REL);
            int alfa = 0x55 + Math.round(0x44 * pulso);
            blit(g, BRILHO, Math.round(cx - lado / 2F), Math.round(cy - lado / 2F), lado, lado, BRILHO_LADO,
                    BRILHO_LADO, (alfa << 24) | (COR_DOURADO & 0xFFFFFF));
        }
        // 2) caixa do pentagono: largura x altura, com a ponta em cy - r
        int x = Math.round(cx - larguraDe(r) / 2F);
        int y = Math.round(cy - r);
        int w = Math.round(larguraDe(r));
        int h = Math.round(alturaDe(r));
        // 3) icone + fundo (o PNG ja vem recortado no pentagono interno); o escurecimento vai como tinta cinza
        int v = Mth.clamp(Math.round(brilho * 255F), 0, 255);
        blit(g, iconeDe(idIcone), x, y, w, h, TEX_W, TEX_H, 0xFF000000 | (v << 16) | (v << 8) | v);
        // 4) moldura por cima, com a cor da borda
        blit(g, MOLDURA, x, y, w, h, TEX_W, TEX_H, corBorda);
    }

    /** Textura inteira esticada em w x h (o filtro suave vem do .mcmeta de cada PNG). */
    private static void blit(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, int texW, int texH,
            int tinta) {
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0F, 0F, w, h, texW, texH, texW, texH, tinta);
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
