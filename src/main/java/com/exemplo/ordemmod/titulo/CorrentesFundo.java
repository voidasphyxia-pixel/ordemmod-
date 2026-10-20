package com.exemplo.ordemmod.titulo;

import com.exemplo.ordemmod.OrdemMod;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/**
 * As CORRENTES da tela inicial: varias faixas de corrente atravessando a tela, umas na frente (nitidas e grandes), outras ao
 * fundo (menores e desfocadas), todas sendo PUXADAS sem parar (andam em trancos: puxa, afrouxa, puxa) e GIRANDO em torno do
 * proprio eixo, com um balanco leve e paralaxe com o mouse.
 *
 * Como funciona: tools/gerar_correntes.py renderiza (3D, ray casting) uma corrente de elos retangulares em 24 quadros de giro
 * (0 a 90 graus; a 90 graus a corrente e identica a de 0 deslocada 1 passo, o que permite girar sem fim) em 3 folhas:
 * correntes_nitida.png, correntes_media.png (desfoque leve) e correntes_fundo.png (desfoque forte). Cada folha e uma grade de
 * celulas; cada celula e um ladrilho de 2 elos que se repete (com PAD pixels de sobra de cada lado para o filtro suave nao
 * deixar emenda). Aqui a faixa e montada repetindo o ladrilho ao longo do comprimento, rolando pela distancia "puxada";
 * o quadro de giro vem do angulo atual e dois quadros vizinhos se misturam (crossfade) para o giro ficar liso.
 *
 * Camadas (a TelaInicial chama uma por vez, na ordem certa):
 *  - FUNDO : desfoque forte, atras do simbolo; - MEIO: desfoque leve, na frente do simbolo; - FRENTE: nitidas, depois da
 *  vinheta (por isso nao escurecem), nas bordas da tela para nao atrapalhar o logo e os botoes.
 *
 * As constantes de atlas abaixo TEM de bater com o que tools/gerar_correntes.py imprime ("CONSTANTES para CorrentesFundo.java").
 * Cada corrente tem seus numeros na tabela CORRENTES (posicao, angulo, tamanho, velocidade, trancos, giro, balanco, cor).
 * Todas as medidas de tela sao relativas a altura/largura da tela, entao funciona em qualquer escala de interface.
 */
public final class CorrentesFundo {
    private CorrentesFundo() {
    }

    public enum Camada {
        FUNDO, MEIO, FRENTE
    }

    // ------------------------------------------------------------------ atlas (devem bater com gerar_correntes.py)
    private static final int QUADROS = 24;        // quadros de giro de 0 a 90 graus
    private static final int COLUNAS = 4;         // grade da folha: 4 colunas x 6 linhas
    private static final float FH_NITIDO = 120F;  // altura util do quadro na folha nitida (HO + 8 px)

    private static final class Folha {
        final Identifier textura;
        final float esc;      // escala da folha em relacao a nitida (1 ou 0,5)
        final int celW;       // largura da celula, com a sobra (PAD) dos dois lados
        final int celH;       // altura da celula
        final int folhaW;
        final int folhaH;
        final int ladrilho;   // largura util do ladrilho (2 elos)
        final int pad;        // sobra de cada lado
        final float passo;    // distancia entre elos consecutivos (metade do ladrilho)

        Folha(String nome, float esc, int celW, int celH, int folhaW, int folhaH, int ladrilho, int pad) {
            this.textura = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "textures/gui/titulo/" + nome + ".png");
            this.esc = esc;
            this.celW = celW;
            this.celH = celH;
            this.folhaW = folhaW;
            this.folhaH = folhaH;
            this.ladrilho = ladrilho;
            this.pad = pad;
            this.passo = ladrilho / 2F;
        }
    }

    private static final Folha NITIDA = new Folha("correntes_nitida", 1F, 352, 120, 1408, 720, 344, 4);
    private static final Folha MEDIA = new Folha("correntes_media", 0.5F, 176, 60, 704, 360, 172, 2);
    private static final Folha FUNDO_FOLHA = new Folha("correntes_fundo", 0.5F, 176, 60, 704, 360, 172, 2);

    // ------------------------------------------------------------------ as correntes
    /**
     * Uma corrente. Posicao e tamanho relativos a tela. {@code vel} e a velocidade em alturas-de-tela por segundo (negativa =
     * anda ao contrario); a velocidade real e vel * (base + amp * sen^4(...)): {@code base} e o ritmo "frouxo" e {@code amp} o
     * tranco, que se repete a cada {@code periodo} segundos. {@code giroBase} (graus/s) gira os elos sozinho e
     * {@code giroPuxa} (graus por altura-de-tela puxada) gira mais quando e puxada. {@code balanco} (graus) e {@code bob}
     * (fracao da altura) fazem a faixa oscilar de leve. {@code tom} escurece (1 = cor cheia); {@code paralaxe} e quanto
     * segue o mouse (px por unidade do deslocamento da tela inicial).
     */
    private record Corrente(Camada camada, float xRel, float yRel, float angulo, float alturaRel, float vel, float base,
            float amp, float periodo, float fase, float giroBase, float giroPuxa, float psi0, float balanco, float bob,
            float tom, float paralaxe) {
    }

    private static final Corrente[] CORRENTES = {
            // FUNDO: pequenas, escuras, bem desfocadas; uma delas cruza o centro, atras do simbolo
            new Corrente(Camada.FUNDO, 0.50F, 0.30F, -14F, 0.13F, 0.045F, 0.30F, 1.6F, 3.4F, 0.0F, 9F, 60F, 0F, 3.0F, 0.012F, 0.50F, 0.4F),
            new Corrente(Camada.FUNDO, 0.50F, 0.74F, 8F, 0.11F, -0.035F, 0.30F, 1.6F, 4.1F, 1.3F, -7F, 50F, 30F, 2.5F, 0.010F, 0.46F, 0.4F),
            new Corrente(Camada.FUNDO, 0.50F, 0.50F, -33F, 0.10F, 0.030F, 0.30F, 1.4F, 4.8F, 2.1F, 6F, 40F, 60F, 3.5F, 0.015F, 0.42F, 0.3F),
            // MEIO: desfoque leve, em cima e embaixo, cruzando a tela inteira
            new Corrente(Camada.MEIO, 0.50F, 0.17F, 5F, 0.17F, -0.075F, 0.25F, 1.7F, 2.9F, 0.7F, -12F, 80F, 15F, 2.0F, 0.008F, 0.72F, 0.9F),
            new Corrente(Camada.MEIO, 0.50F, 0.86F, -7F, 0.19F, 0.065F, 0.25F, 1.7F, 3.3F, 2.6F, 10F, 70F, 45F, 2.0F, 0.008F, 0.72F, 0.9F),
            // FRENTE: grandes e nitidas, quase verticais, coladas nas laterais (para nao cobrir o logo nem os botoes)
            new Corrente(Camada.FRENTE, 0.02F, 0.50F, 84F, 0.22F, 0.11F, 0.22F, 1.8F, 2.6F, 0.4F, 14F, 90F, 0F, 1.2F, 0.006F, 1.0F, 1.7F),
            new Corrente(Camada.FRENTE, 0.98F, 0.50F, -83F, 0.25F, -0.10F, 0.22F, 1.8F, 3.0F, 1.9F, -11F, 80F, 70F, 1.2F, 0.006F, 1.0F, 1.7F)
    };

    // ------------------------------------------------------------------ desenho

    /**
     * Desenha as correntes de uma camada.
     * @param t      tempo da tela, em segundos
     * @param fade   0..1 (entrada suave: escurece a cor, ja que as correntes sao opacas)
     * @param parX   deslocamento de paralaxe do mouse (o mesmo parX da TelaInicial)
     */
    public static void desenhar(GuiGraphicsExtractor g, Camada camada, float t, float fade, int largura, int altura,
            float parX, float parY) {
        if (fade <= 0.01F) {
            return;
        }
        Folha folha = camada == Camada.FRENTE ? NITIDA : camada == Camada.MEIO ? MEDIA : FUNDO_FOLHA;
        for (Corrente c : CORRENTES) {
            if (c.camada() == camada) {
                faixa(g, folha, c, t, fade, largura, altura, parX, parY);
            }
        }
    }

    private static void faixa(GuiGraphicsExtractor g, Folha f, Corrente c, float t, float fade, int w, int h, float parX,
            float parY) {
        // escala: pixels da interface por pixel da folha (a folha media/fundo tem metade da resolucao)
        float k = (c.alturaRel() * h / FH_NITIDO) / f.esc;

        // distancia puxada, em alturas-de-tela, pela integral exata de vel * (base + amp * sen^4(x)), x = omega*t + fase:
        // integral de sen^4 = 3x/8 - sen(2x)/4 + sen(4x)/32
        float omega = (float) Math.PI / c.periodo();
        float x = omega * t + c.fase();
        float s = c.vel() * (c.base() * t
                + c.amp() * (0.375F * x - (float) Math.sin(2 * x) / 4F + (float) Math.sin(4 * x) / 32F) / omega);
        float puxadoAtlas = s * h / k;

        // giro: angulo em graus (cresce com o tempo e com a puxada); 90 graus = mesma corrente deslocada 1 passo
        float psi = c.psi0() + c.giroBase() * t + c.giroPuxa() * s;
        float q = psi / 90F;
        int n = (int) Math.floor(q);
        float posQuadro = (q - n) * QUADROS;
        int f0 = Math.min(QUADROS - 1, (int) Math.floor(posQuadro));
        float mistura = posQuadro - f0;
        int f1 = (f0 + 1) % QUADROS;
        int nB = n + (f0 + 1 >= QUADROS ? 1 : 0); // passando do ultimo quadro, o proximo e o 0 do ciclo seguinte (+1 passo)

        // posicao e angulo da faixa (com balanco e paralaxe)
        float ang = c.angulo() + c.balanco() * (float) Math.sin(0.31F * t + c.fase() * 1.7F);
        float cx = c.xRel() * w + parX * c.paralaxe();
        float cy = c.yRel() * h + parY * c.paralaxe() + c.bob() * h * (float) Math.sin(0.5F * t + c.fase() * 0.6F);

        int canal = Math.max(0, Math.min(255, Math.round(255F * c.tom() * fade)));
        int cor = 0xFF000000 | (canal << 16) | (canal << 8) | canal;

        // comprimento a cobrir: a diagonal da tela (em pixels da folha), com folga
        float metade = (float) Math.hypot(w, h) * 0.5F / k + f.ladrilho;
        int m0 = (int) Math.floor(-metade / f.ladrilho) - 1;
        int m1 = (int) Math.ceil(metade / f.ladrilho) + 1;

        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().rotate((float) Math.toRadians(ang));
        g.pose().scale(k, k);
        // quadro A por baixo (opaco) e o B por cima com a "mistura" como alfa: o giro passa liso de um quadro ao outro
        camadaDeLadrilhos(g, f, f0, puxadoAtlas - n * f.passo, m0, m1, cor);
        if (mistura > 0.02F) {
            int alfa = Math.round(255F * Math.min(1F, mistura));
            camadaDeLadrilhos(g, f, f1, puxadoAtlas - nB * f.passo, m0, m1, (alfa << 24) | (cor & 0xFFFFFF));
        }
        g.pose().popMatrix();
    }

    /** Uma fileira de ladrilhos do quadro {@code quadro}, deslocada para a direita por {@code deslocamento} (px da folha). */
    private static void camadaDeLadrilhos(GuiGraphicsExtractor g, Folha f, int quadro, float deslocamento, int m0, int m1,
            int cor) {
        float off = deslocamento % f.ladrilho;
        if (off < 0F) {
            off += f.ladrilho;
        }
        int celX = (quadro % COLUNAS) * f.celW;
        int celY = (quadro / COLUNAS) * f.celH;
        g.pose().pushMatrix();
        g.pose().translate(off, 0F);
        for (int m = m0; m <= m1; m++) {
            // u + pad: a sobra lateral fica de fora da area desenhada, mas o filtro suave ainda a le nas bordas
            g.blit(RenderPipelines.GUI_TEXTURED, f.textura, m * f.ladrilho, -f.celH / 2, (float) (celX + f.pad),
                    (float) celY, f.ladrilho, f.celH, f.ladrilho, f.celH, f.folhaW, f.folhaH, cor);
        }
        g.pose().popMatrix();
    }
}
