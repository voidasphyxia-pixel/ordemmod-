package com.exemplo.ordemmod;

import java.util.List;
import java.util.Locale;
import java.util.Random;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;

/**
 * A tela do teste de perícia (estilo Baldur's Gate 3). Abre por cima da ficha (ou do jogo) quando o servidor manda
 * o resultado de um teste, e toca a animação:
 *
 *  1. os dados rolam e quicam, com os números piscando até assentar;
 *  2. o dado que valeu fica em destaque (os outros apagam);
 *  3. os bônus entram um por um, e o total vai somando junto com um som a cada bônus;
 *  4. o veredito aparece (SUCESSO, FALHA, CRÍTICO ou FALHA CRÍTICA) com o som correspondente.
 *
 * Clique ou Espaço/Enter: pula direto para o veredito; depois do veredito, fecha. Esc ou K fecham na hora.
 * Fecha sozinha alguns segundos depois do veredito e devolve a tela que estava aberta antes.
 * Só existe no cliente. Os tempos e as cores estão nas constantes logo abaixo, é só mexer.
 */
public class TelaTeste extends Screen {
    // ---------------------------------------------------------------- tempos (milissegundos)
    private static final long T_ROLAGEM = 1100;        // os dados rolando (o som dado_rolando dura ~0,8 s)
    private static final long T_PAUSA = 550;           // dado escolhido em destaque, antes dos bônus
    private static final long T_POR_BONUS = 650;       // cada bônus
    private static final long T_CONTAGEM = 450;        // quanto o total demora para subir com cada bônus
    private static final long T_ANTES_VEREDITO = 300;  // respiro depois do último bônus
    private static final long T_PERMANENCIA = 2600;    // quanto o veredito fica na tela antes de fechar sozinho
    private static final long T_FADE_ENTRADA = 200;
    private static final long T_FADE_SAIDA = 300;

    // ---------------------------------------------------------------- cores (RGB; a transparência é aplicada no desenho)
    private static final int COR_OSSO = 0xE8E2D4;
    private static final int COR_APAGADO = 0x8A8A94;
    private static final int COR_DOURADO = 0xE6C229;
    private static final int COR_SANGUE = 0xB0172B;
    private static final int COR_SANGUE_CLARO = 0xE0263E;
    private static final int COR_OK = 0x6FBF73;
    private static final int COR_FALHA_CRITICA = 0xFF5555;
    private static final int COR_FACE = 0x1A0A0E;
    private static final int COR_FACE_TRIANGULO = 0x2A0F15;

    /** Distância do "y" de desenho até o meio das letras na fonte do mod (mesmo valor da ficha). */
    private static final float CENTRO_VISUAL = 2.2F;

    private final Screen anterior;
    private final PacoteTeste teste;
    private final List<Integer> dados;
    private final List<ParteBonus> partes;
    private final int indiceEscolhido;
    private final boolean falhaCritica;
    private final long semente;

    private long inicio = -1;
    private float larguraVirtual = 400F; // largura da área de desenho, já descontada a escala
    private int bonusTocados = 0;
    private boolean vereditoTocado = false;

    public TelaTeste(Screen anterior, PacoteTeste teste) {
        super(Component.literal("Teste de perícia"));
        this.anterior = anterior;
        this.teste = teste;
        this.dados = teste.dados();
        this.partes = teste.partes();
        int indice = dados.indexOf(teste.escolhido());
        this.indiceEscolhido = Math.max(0, indice);
        this.falhaCritica = teste.escolhido() == 1 && !teste.sucesso();
        this.semente = teste.total() * 131L + teste.dt() * 17L + teste.pericia();
    }

    public Screen getAnterior() {
        return anterior;
    }

    // ================================================================== tempos derivados

    private long tBonus0() {
        return T_ROLAGEM + T_PAUSA;
    }

    private long tVeredito() {
        return tBonus0() + (long) partes.size() * T_POR_BONUS + T_ANTES_VEREDITO;
    }

    private long tFim() {
        return tVeredito() + T_PERMANENCIA;
    }

    // ================================================================== tela

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        // nada para criar: tudo é desenhado na mão em render()
    }

    @Override
    public void resize(Minecraft mc, int largura, int altura) {
        if (anterior != null) {
            anterior.resize(mc, largura, altura);
        }
        super.resize(mc, largura, altura);
    }

    @Override
    public void tick() {
        super.tick();
        if (inicio >= 0 && Util.getMillis() - inicio >= tFim()) {
            onClose(); // fecha sozinha um tempo depois do veredito
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(anterior); // volta para a ficha (ou para o jogo, se anterior for null)
        }
    }

    // ================================================================== entrada

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao == 0 || botao == 1) {
            continuar();
            return true;
        }
        return super.mouseClicked(mx, my, botao);
    }

    @Override
    public boolean keyPressed(int tecla, int codigo, int modificadores) {
        if (tecla == 256 || TeclasOrdem.MENU.matches(tecla, codigo)) { // Esc ou K
            onClose();
            return true;
        }
        if (tecla == 32 || tecla == 257 || tecla == 335) { // Espaço, Enter, Enter do teclado numérico
            continuar();
            return true;
        }
        return super.keyPressed(tecla, codigo, modificadores);
    }

    /** Durante a animação, pula para o veredito; depois do veredito, fecha. */
    private void continuar() {
        if (inicio < 0) {
            return;
        }
        long agora = Util.getMillis();
        if (agora - inicio >= tVeredito()) {
            onClose();
        } else {
            inicio = agora - tVeredito();
            bonusTocados = partes.size(); // os sons dos bônus pulados não tocam
        }
    }

    // ================================================================== sons

    private void tocar(SoundEvent som, float tom) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(som, tom, 1.0F));
        }
    }

    private void tocarVeredito() {
        if (teste.critico()) {
            tocar(SonsOrdem.CRITICO.get(), 1.0F);
        } else if (teste.sucesso()) {
            tocar(SonsOrdem.SUCESSO.get(), 1.0F);
        } else if (falhaCritica) {
            tocar(SonsOrdem.FALHA_CRITICA.get(), 1.0F);
        } else {
            tocar(SonsOrdem.FALHA.get(), 1.0F);
        }
    }

    // ================================================================== desenho

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        long agora = Util.getMillis();
        if (inicio < 0) {
            inicio = agora;
            tocar(SonsOrdem.DADO_ROLANDO.get(), 1.0F);
        }
        long t = agora - inicio;

        // sons que dependem do tempo
        while (bonusTocados < partes.size() && t >= tBonus0() + (long) bonusTocados * T_POR_BONUS) {
            tocar(SonsOrdem.BONUS_SOMA.get(), 1.0F + 0.1F * bonusTocados); // cada bônus um pouquinho mais agudo
            bonusTocados++;
        }
        if (!vereditoTocado && t >= tVeredito()) {
            vereditoTocado = true;
            tocarVeredito();
        }

        float entrada = Mth.clamp(t / (float) T_FADE_ENTRADA, 0F, 1F);
        float saida = Mth.clamp((tFim() - t) / (float) T_FADE_SAIDA, 0F, 1F); // some no fim, até o tick fechar
        float opacidade = entrada * saida;

        // o que estava aberto antes (a ficha), sem reação ao mouse, escurecido por trás
        if (anterior != null) {
            anterior.render(g, -1, -1, parcial);
        }
        g.fill(0, 0, width, height, argb(0x000000, 0.74F * opacidade));

        // tudo é desenhado numa área "virtual" centrada e encolhida se a janela for baixa
        float escala = Mth.clamp(height / 250F, 0.6F, 1.25F);
        larguraVirtual = width / escala;
        float cx = width / 2F / escala;
        float cy = height / 2F / escala;
        g.pose().pushPose();
        g.pose().scale(escala, escala, 1F);

        desenharTitulo(g, cx, cy, opacidade);
        desenharDados(g, cx, cy, t, opacidade);
        if (t >= T_ROLAGEM) {
            desenharTotalEBonus(g, cx, cy, t, opacidade);
        }
        if (t >= tVeredito()) {
            desenharVeredito(g, cx, cy, t, opacidade);
        }

        g.pose().popPose();
    }

    private void desenharTitulo(GuiGraphics g, float cx, float cy, float op) {
        Pericia[] todas = Pericia.values();
        String nome = teste.pericia() >= 0 && teste.pericia() < todas.length
                ? todas[teste.pericia()].getNome().toUpperCase(Locale.ROOT) + "  ·  "
                        + todas[teste.pericia()].getAtributo().getSigla()
                : "TESTE";
        textoCentro(g, nome, cx, cy - 92, 1.5F, COR_OSSO, op);
        String sub = "DT " + teste.dt() + (teste.motivo().isBlank() ? "" : "  ·  " + teste.motivo());
        textoCentro(g, sub, cx, cy - 76, 1.0F, COR_APAGADO, op);
    }

    // ---------------------------------------------------------------- dados

    private void desenharDados(GuiGraphics g, float cx, float cy, long t, float op) {
        int n = dados.size();
        if (n == 0) {
            return;
        }
        float raio = Math.min(26F, larguraVirtual * 0.8F / (n * 2.5F));
        float passo = raio * 2.5F;
        float yCentro = cy - 36;

        float p = Mth.clamp(t / (float) T_ROLAGEM, 0F, 1F);
        float suave = 1F - (1F - p) * (1F - p) * (1F - p);
        float depois = Mth.clamp((t - T_ROLAGEM) / 300F, 0F, 1F); // destaque do dado escolhido

        for (int i = 0; i < n; i++) {
            float x = cx + (i - (n - 1) / 2F) * passo;
            boolean escolhido = i == indiceEscolhido;

            // rolagem: gira, quica e achata um pouco, tudo diminuindo até assentar
            float giro = (1F - suave) * (720F + 150F * i) * (i % 2 == 0 ? 1F : -1F);
            float quique = -(float) Math.abs(Math.sin(p * Math.PI * 3F)) * (1F - p) * (1F - p) * 26F;
            float achatar = 1F + 0.14F * (float) Math.sin(p * Math.PI * 6F) * (1F - p);

            int valor;
            if (p < 1F) {
                long passoDoNumero = (long) (t / (45F + 170F * p));
                valor = 1 + new Random(semente * 31L + i * 977L + passoDoNumero).nextInt(20);
            } else {
                valor = dados.get(i);
            }

            // depois de assentar: o dado que valeu cresce e brilha; os outros apagam
            float tamanho = raio * (escolhido ? 1F + 0.28F * depois : 1F);
            float visibilidade = op * (escolhido || n == 1 ? 1F : 1F - 0.62F * depois);

            desenharDado(g, x, yCentro + quique, tamanho, giro, achatar, valor, visibilidade,
                    escolhido ? depois : 0F, valor == 20, valor == 1);
        }
    }

    private void desenharDado(GuiGraphics g, float x, float y, float raio, float giro, float achatar, int valor,
            float visibilidade, float destaque, boolean vinte, boolean um) {
        int corBorda = COR_OSSO;
        if (destaque > 0F) {
            corBorda = vinte ? COR_DOURADO : um ? COR_SANGUE_CLARO : COR_DOURADO;
            // brilho atrás do dado escolhido
            poligono(g, x, y, raio * 1.22F, giro, achatar, 6, argb(corBorda, 0.22F * destaque * visibilidade));
        }
        poligono(g, x, y, raio, giro, achatar, 6, argb(corBorda, visibilidade));
        poligono(g, x, y, raio - 2F, giro, achatar, 6, argb(COR_FACE, visibilidade));
        // triângulo do d20, na cor de sangue
        poligono(g, x, y + raio * 0.05F, raio * 0.66F, giro, achatar, 3, argb(COR_SANGUE, visibilidade));
        poligono(g, x, y + raio * 0.05F, raio * 0.66F - 2F, giro, achatar, 3, argb(COR_FACE_TRIANGULO, visibilidade));
        int corNumero = destaque > 0F && vinte ? COR_DOURADO : destaque > 0F && um ? COR_SANGUE_CLARO : COR_OSSO;
        textoCentro(g, Integer.toString(valor), x, y + raio * 0.12F, raio / 15F, corNumero, visibilidade);
    }

    // ---------------------------------------------------------------- total, bônus e veredito

    private void desenharTotalEBonus(GuiGraphics g, float cx, float cy, long t, float op) {
        float entrada = Mth.clamp((t - T_ROLAGEM) / 250F, 0F, 1F);

        // total que vai somando
        int mostrado;
        if (t >= tVeredito()) {
            mostrado = teste.total();
        } else {
            float v = teste.escolhido();
            for (int i = 0; i < partes.size(); i++) {
                long ini = tBonus0() + (long) i * T_POR_BONUS;
                float q = Mth.clamp((t - ini) / (float) T_CONTAGEM, 0F, 1F);
                v += partes.get(i).valor() * (1F - (1F - q) * (1F - q));
            }
            mostrado = Math.round(v);
        }
        int corTotal = COR_OSSO;
        if (t >= tVeredito()) {
            corTotal = corDoVeredito();
        }
        // pulsinho quando um bônus entra
        float pulso = 1F;
        for (int i = 0; i < partes.size(); i++) {
            long ini = tBonus0() + (long) i * T_POR_BONUS;
            float q = (t - ini) / 220F;
            if (q >= 0F && q < 1F) {
                pulso = Math.max(pulso, 1F + 0.25F * (1F - q));
            }
        }
        textoCentro(g, Integer.toString(mostrado), cx, cy + 22, 3F * pulso, corTotal, op * entrada);

        // linhas de bônus: "+5  Treinado"
        for (int i = 0; i < partes.size(); i++) {
            long ini = tBonus0() + (long) i * T_POR_BONUS;
            float q = Mth.clamp((t - ini) / 220F, 0F, 1F);
            if (q <= 0F) {
                continue;
            }
            ParteBonus parte = partes.get(i);
            String sinal = parte.valor() >= 0 ? "+" : "";
            float y = cy + 48 + i * 12 + (1F - q) * 6F;
            textoCentro(g, sinal + parte.valor() + "  " + parte.rotulo(), cx, y, 1.0F, COR_DOURADO, op * q);
        }
    }

    private void desenharVeredito(GuiGraphics g, float cx, float cy, long t, float op) {
        float q = Mth.clamp((t - tVeredito()) / 250F, 0F, 1F);
        float pop = 1F + 0.8F * (1F - q) * (1F - q);
        String texto = teste.critico() ? "CRÍTICO!" : teste.sucesso() ? "SUCESSO" : falhaCritica ? "FALHA CRÍTICA" : "FALHA";
        float y = cy + 56 + Math.max(1, partes.size()) * 12 + 14;
        textoCentro(g, texto, cx, y, 2.2F * pop, corDoVeredito(), op * q);

        String detalhe = teste.critico() && teste.escolhido() == 20
                ? "20 natural"
                : teste.total() + " contra DT " + teste.dt();
        textoCentro(g, detalhe, cx, y + 16, 1.0F, COR_APAGADO, op * q);
        textoCentro(g, "clique para continuar", cx, y + 30, 0.8F, COR_APAGADO, op * q * 0.7F);
    }

    private int corDoVeredito() {
        if (teste.critico()) {
            return COR_DOURADO;
        }
        if (teste.sucesso()) {
            return COR_OK;
        }
        return falhaCritica ? COR_FALHA_CRITICA : COR_SANGUE_CLARO;
    }

    // ================================================================== desenho "na mão"

    /** RGB + transparência (0 a 1) viram a cor com alfa que o jogo espera. */
    private static int argb(int rgb, float alfa) {
        int a = Mth.clamp(Math.round(alfa * 255F), 0, 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Texto centralizado em (x, y), com escala, na fonte do mod. Abaixo de uma transparência mínima nem desenha. */
    private void textoCentro(GuiGraphics g, String s, float x, float y, float escala, int rgb, float alfa) {
        if (alfa < 0.04F || s.isEmpty()) {
            return; // o jogo trata alfa muito baixo como opaco, então é melhor nem desenhar
        }
        int w = FonteOP.largura(font, s);
        g.pose().pushPose();
        g.pose().translate(x, y, 0F);
        g.pose().scale(escala, escala, 1F);
        FonteOP.desenhar(g, font, s, -w / 2F, -CENTRO_VISUAL, argb(rgb, alfa), false);
        g.pose().popPose();
    }

    /**
     * Polígono regular preenchido (linha por linha), com a ponta para cima.
     * {@code giro}: graus; {@code achatar}: escala horizontal (dá a ideia de o dado virando).
     */
    private static void poligono(GuiGraphics g, float cx, float cy, float raio, float giro, float achatar, int lados,
            int cor) {
        if (((cor >>> 24) & 0xFF) == 0 || raio <= 0F) {
            return;
        }
        float[] xs = new float[lados];
        float[] ys = new float[lados];
        double base = Math.toRadians(-90.0 + giro);
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        for (int k = 0; k < lados; k++) {
            double a = base + k * 2.0 * Math.PI / lados;
            xs[k] = cx + (float) Math.cos(a) * raio * achatar;
            ys[k] = cy + (float) Math.sin(a) * raio;
            minY = Math.min(minY, ys[k]);
            maxY = Math.max(maxY, ys[k]);
        }
        for (int y = (int) Math.floor(minY); y < (int) Math.ceil(maxY); y++) {
            float yc = y + 0.5F;
            float esquerda = Float.MAX_VALUE;
            float direita = -Float.MAX_VALUE;
            for (int k = 0; k < lados; k++) {
                int k2 = (k + 1) % lados;
                float y1 = ys[k];
                float y2 = ys[k2];
                if (yc >= Math.min(y1, y2) && yc < Math.max(y1, y2)) {
                    float x = xs[k] + (yc - y1) * (xs[k2] - xs[k]) / (y2 - y1);
                    esquerda = Math.min(esquerda, x);
                    direita = Math.max(direita, x);
                }
            }
            if (esquerda < direita) {
                g.fill(Math.round(esquerda), y, Math.round(direita), y + 1, cor);
            }
        }
    }
}
