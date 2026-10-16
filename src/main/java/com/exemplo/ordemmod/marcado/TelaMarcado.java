package com.exemplo.ordemmod.marcado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.lwjgl.glfw.GLFW;

import com.exemplo.ordemmod.DesenhoOP;
import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.ReceptoresCliente;
import com.exemplo.ordemmod.TelaCriacao;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;

/**
 * A cena do Marcado: introdução narrativa de terror que vem ANTES da tela de criação de personagem.
 * Lê o roteiro (RoteiroMarcado), mostra uma fala ou pergunta por vez, dispara efeitos e, no fim,
 * manda as escolhas ao servidor (PacoteMarcado) e abre a TelaCriacao normal.
 *
 * ESTADO: esqueleto jogável, ainda sem teste no jogo. Veja o LEIA-ME-MARCADO.md (o que falta ajustar).
 *
 * Fabric 26.2 (6d): render() virou extractRenderState(GuiGraphicsExtractor, ...); a entrada recebe KeyEvent/MouseButtonEvent;
 * a pilha de matrizes e a Matrix3x2fStack; os halos deixaram de ser aditivos (sobre o fundo preto da cena ficam iguais);
 * drawCenteredString virou textoCentro (g.text com a largura medida); as escolhas vao por ReceptoresCliente.enviar.
 * A SILHUETA do personagem: no Forge era o proprio jogador desenhado com uma textura branca (RenderType trocado). Isso nao
 * existe no 26.2, entao por padrao e uma silhueta chapada desenhada na mao (DesenhoOP.silhueta). Com SILHUETA_PROCEDURAL = false
 * volta a skin real do jogador, parada (DesenhoOP.entidade).
 */
public class TelaMarcado extends Screen {
    private static final Style FONTE_ENTIDADE = estilo("entidade");
    private static final Identifier HALO = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "textures/gui/marcado/halo.png");
    /** O circulo ritual grande da tela inicial (o mesmo arquivo), 1024x1024. */
    private static final Identifier SIMBOLO_TITULO = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID,
            "textures/gui/titulo/simbolo_maior.png");
    /** Onde fica o centro do circulo dentro da textura (fracao da altura) e metricas da Cinzel Black (iguais as da TelaInicial). */
    private static final float SIMBOLO_CENTRO = 0.539f;
    private static final float CAP_LOGO = 28f;
    private static final float CAP_SUB = 8.75f;
    private static final float BASE_LOGO = 6.6f;
    private static final float BASE_SUB = 4.9f;
    /** true: silhueta chapada desenhada na mao; false: a skin real do jogador (sem o efeito de silhueta branca). */
    private static final boolean SILHUETA_PROCEDURAL = true;
    private static final int COR_SILHUETA = 0xFFE4E8F2;
    private static final float LETRAS_POR_SEGUNDO = 22f;
    private static final float PAUSA_PADRAO = 1.2f;
    private static final Random SORTEIO = new Random();

    private static Style estilo(String fonte) {
        return Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, fonte)));
    }

    private enum Fase { DIGITANDO, PAUSA, ESCOLHA, REACAO, SILENCIO, ESPERA, REVELACAO, FIM, SAIDA_FADE }

    // saída secreta: ESC 5 vezes seguidas pula a cena (fade out, som para, uma frase, e vai para a criação)
    private static final int ESC_PARA_PULAR = 5;
    private static final float ESC_JANELA = 2.5f;      // segundos máximos entre um ESC e o próximo
    private static final float SAIDA_FADE_SEG = 1.8f;  // duração do fade out
    private int escContagem;
    private float escUltimo = -100f;
    private boolean saindo;

    private final RoteiroMarcado roteiro = RoteiroMarcado.carregar();
    private final int[] escolhas = new int[roteiro.totalPerguntas()];
    private int idx = -1;
    private int perguntaAtual = -1;
    private Fase fase = Fase.PAUSA;
    private float t;                       // segundos dentro da fase atual
    private float relogio;                 // segundos desde que a tela abriu (animações de fundo)
    private long ultimo = System.nanoTime();

    private List<String> linhas = new ArrayList<>();
    private int totalLetras;
    private int sel;
    private final List<int[]> areasOpcao = new ArrayList<>(); // x1,y1,x2,y2 de cada opção (para o mouse)
    private RoteiroMarcado.Passo passo;
    private RoteiroMarcado.Opcao escolhida;
    private float pausaFinal = PAUSA_PADRAO;
    private int letrasTocadas;

    private AudioMarcado.Laco drone;
    private AudioMarcado.Laco batimento;
    private final Map<String, Float> efeitos = new HashMap<>(); // id -> segundos já passados
    private float silencioAte = -1f;
    private boolean mudo;
    private boolean revelacaoTocou;
    private boolean enviado;
    private final Map<String, Integer> respostasPorId = new HashMap<>(); // id da pergunta -> opção escolhida

    // estado visual/sonoro controlado pelos efeitos do roteiro
    private float volumeBase = 0.06f;      // volume do drone (quase inaudível na abertura)
    private static final float ESCALA_SOMBRA = 0.4f;                                  // câmera lenta da criatura
    private static final float DURACAO_SOMBRA = VfxMarcado.SOMBRA.duracao() / ESCALA_SOMBRA; // ~2,5 s
    private float particulas;              // 0..1: névoa e poeira (nascem devagar depois do TUM)
    private boolean particulasOn;
    private float aparicao;                // 0..1: o personagem só é revelado perto do fim
    private boolean revelando;
    private float zoom = 1f;               // "a câmera se aproxima" do personagem
    private boolean aproximando;
    private int corTexto = 0xFFD8D0D0;

    public TelaMarcado() {
        super(Component.literal("Marcado"));
    }

    @Override
    protected void init() {
        if (idx < 0) {
            drone = new AudioMarcado.Laco("ambiente_drone", volumeBase);
            drone.iniciar();
            proximoPasso();
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // a cena não pode ser fechada no meio
    }

    @Override
    public boolean isPauseScreen() {
        return false; // o servidor precisa continuar rodando para receber as escolhas
    }

    @Override
    public void removed() {
        if (drone != null) {
            drone.fim();
        }
        if (batimento != null) {
            batimento.fim();
        }
        VfxMarcado.liberarTodos(VfxMarcado.NEVOA, VfxMarcado.POEIRA); // a tela de criação ainda usa os dois
    }

    /** Volta o drone e o batimento depois de um "silence" (a Morte tira o som só por um momento). */
    private void restaurarSom() {
        if (mudo) {
            mudo = false;
            if (drone != null) {
                drone.volumeAlvo(volumeBase);
            }
            if (batimento != null) {
                batimento.volumeAlvo(0.5f);
            }
        }
    }

    // ================================================================== fluxo do roteiro

    private void proximoPasso() {
        idx++;
        t = 0;
        escolhida = null;
        while (idx < roteiro.passos.size() && !condicaoOk(roteiro.passos.get(idx))) {
            idx++; // passo que só vale para outra resposta
        }
        if (idx >= roteiro.passos.size()) {
            terminar();
            return;
        }
        passo = roteiro.passos.get(idx);
        disparar(passo.efeito);
        if (passo.som != null) {
            AudioMarcado.tocar(passo.som, 1f, 1f);
        }
        switch (passo.tipo) {
            case "fala" -> mostrar(passo.texto, Fase.DIGITANDO, passo.duracao > 0 ? passo.duracao : PAUSA_PADRAO);
            case "pergunta" -> {
                perguntaAtual++;
                sel = 0;
                mostrar(passo.texto, Fase.DIGITANDO, 0.4f);
            }
            case "silencio" -> {
                fase = Fase.SILENCIO;
                linhas = new ArrayList<>();
                disparar("silence");
                silencioAte = passo.duracao > 0 ? passo.duracao : 3f;
            }
            case "pausa" -> { // espera sem texto e SEM mudar o som (o "silence" é outro efeito)
                fase = Fase.ESPERA;
                linhas = new ArrayList<>();
                silencioAte = passo.duracao > 0 ? passo.duracao : 1.5f;
            }
            case "revelacao" -> {
                fase = Fase.REVELACAO;
                linhas = new ArrayList<>();
                revelacaoTocou = false;
                aparicao = 0f; // as correntes e o título aparecem sobre o preto
                revelando = false;
            }
            default -> { // "fim"
                fase = Fase.FIM;
                linhas = new ArrayList<>();
                AudioMarcado.tocar("transicao_mundo", 1f, 1f);
                if (drone != null) {
                    drone.fim();
                }
                if (batimento != null) {
                    batimento.fim();
                }
            }
        }
    }

    /** Campo "se" do passo: "p01=0" ou "p01=0|2" (várias, separadas por vírgula, todas precisam valer). */
    private boolean condicaoOk(RoteiroMarcado.Passo p) {
        if (p.se == null) {
            return true;
        }
        for (String c : p.se.split(",")) {
            String[] kv = c.trim().split("=");
            if (kv.length != 2) {
                continue;
            }
            Integer r = respostasPorId.get(kv[0].trim());
            if (r == null) {
                return false;
            }
            boolean ok = false;
            for (String v : kv[1].split("\\|")) {
                if (v.trim().equals(String.valueOf(r))) {
                    ok = true;
                }
            }
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    private void mostrar(String chave, Fase novaFase, float pausa) {
        fase = novaFase;
        t = 0;
        pausaFinal = pausa;
        letrasTocadas = 0;
        linhas = new ArrayList<>();
        int largura = Math.min(width - 60, 360);
        Component c = Component.translatable(chave).withStyle(FONTE_ENTIDADE);
        for (FormattedText linha : font.getSplitter().splitLines(c, largura, FONTE_ENTIDADE)) {
            linhas.add(linha.getString());
        }
        totalLetras = 0;
        for (String l : linhas) {
            totalLetras += l.length();
        }
    }

    private void escolher(int i) {
        if (fase != Fase.ESCOLHA || i < 0 || i >= passo.opcoes.size()) {
            return;
        }
        escolhida = passo.opcoes.get(i);
        escolhas[perguntaAtual] = i;
        if (passo.id != null) {
            respostasPorId.put(passo.id, i);
        }
        AudioMarcado.tocar("ui_select", 0.6f, 1f);
        disparar(escolhida.efeito);
        if (escolhida.reacao != null) {
            AudioMarcado.tocar("reacao_entidade", 0.8f, 1f);
            String[] variantes = escolhida.reacao.split("\\|");
            mostrar(variantes[SORTEIO.nextInt(variantes.length)].trim(), Fase.REACAO, PAUSA_PADRAO);
        } else {
            proximoPasso();
        }
    }

    /** Conta os ESC (um por vez que a tecla é solta). Cinco seguidos, com no máximo 2,5 s entre eles, pulam a cena. */
    private void contarEsc() {
        if (saindo || enviado || fase == Fase.FIM) {
            return;
        }
        escContagem = relogio - escUltimo <= ESC_JANELA ? escContagem + 1 : 1;
        escUltimo = relogio;
        if (escContagem >= ESC_PARA_PULAR) {
            iniciarSaidaSecreta();
        }
    }

    /** Fade out da tela e dos sons; depois aparece a frase e só então abre a tela de criação. */
    private void iniciarSaidaSecreta() {
        saindo = true;
        fase = Fase.SAIDA_FADE;
        t = 0;
        linhas = new ArrayList<>();
        efeitos.clear();
        revelando = false;
        if (drone != null) {
            drone.fim();      // o volume desce devagar durante o fade
        }
        if (batimento != null) {
            batimento.fim();
        }
    }

    private void terminar() {
        if (enviado) {
            return;
        }
        enviado = true;
        ReceptoresCliente.enviar(new PacoteMarcado(escolhas));
        Minecraft.getInstance().gui.setScreen(new TelaCriacao());
    }

    // ================================================================== efeitos (ids do roteiro)

    /** Aceita vários ids separados por vírgula: "glitch,symbol_energia". */
    private void disparar(String ids) {
        if (ids == null) {
            return;
        }
        for (String id : ids.split(",")) {
            id = id.trim();
            efeitos.put(id, 0f);
            switch (id) {
                case "pulse_red" -> AudioMarcado.tocar("sangue", 0.9f, 1f);
                case "glitch" -> AudioMarcado.tocar("glitch", 0.7f, 1f);
                case "distort" -> AudioMarcado.tocar("distorcao", 0.8f, 1f);
                case "shadow" -> AudioMarcado.tocar("sombra", 0.8f, 1f);
                case "whisper" -> AudioMarcado.tocar("sussuros_" + (1 + SORTEIO.nextInt(4)), 0.5f, 1f);
                case "heartbeat" -> {
                    if (batimento == null) {
                        batimento = new AudioMarcado.Laco("batimento_loop", 0.5f);
                        batimento.iniciar();
                    }
                }
                case "silence" -> {
                    mudo = true;
                    if (drone != null) {
                        drone.volumeAlvo(0f);
                    }
                    if (batimento != null) {
                        batimento.volumeAlvo(0f);
                    }
                }
                case "som_volta" -> restaurarSom();
                case "ambiente" -> {
                    volumeBase = 0.45f;
                    if (!mudo && drone != null) {
                        drone.volumeAlvo(volumeBase);
                    }
                }
                case "particulas_on" -> particulasOn = true;
                case "particulas_off" -> {
                    particulasOn = false;
                    particulas = 0f;
                }
                case "personagem_revela" -> revelando = true;
                case "aproximar" -> aproximando = true;
                case "voz_seria" -> corTexto = 0xFFC49A9A;
                default -> { }
            }
        }
    }

    private float efeito(String id, float duracao) {
        Float v = efeitos.get(id);
        return v != null && v < duracao ? v / duracao : -1f; // 0..1 enquanto ativo, -1 inativo
    }

    // ================================================================== tempo

    private void avancarTempo(float dt) {
        t += dt;
        relogio += dt;
        efeitos.replaceAll((k, v) -> v + dt);
        if (particulasOn) {
            particulas = Math.min(1f, particulas + dt / 4f);
        }
        if (revelando) {
            aparicao = Math.min(1f, aparicao + dt / 6f);
        }
        if (aproximando) {
            zoom = Math.min(1.5f, zoom + dt * 0.05f);
        }
        switch (fase) {
            case DIGITANDO, REACAO -> {
                int mostradas = Math.min(totalLetras, (int) (t * LETRAS_POR_SEGUNDO));
                while (letrasTocadas < mostradas) {
                    if (letrasTocadas % 3 == 0 && !saindo) { // na saída secreta o som já parou: texto em silêncio
                        AudioMarcado.tocar("blip_texto", 0.25f, 0.9f + SORTEIO.nextFloat() * 0.2f);
                    }
                    letrasTocadas++;
                }
                if (mostradas >= totalLetras) {
                    fase = Fase.PAUSA;
                    t = 0;
                }
            }
            case PAUSA -> {
                if (t >= pausaFinal) {
                    if (saindo) {
                        terminar(); // a frase da saída secreta acabou: vai para a criação de personagem
                    } else if (passo != null && "pergunta".equals(passo.tipo) && escolhida == null) {
                        fase = Fase.ESCOLHA;
                        t = 0;
                    } else {
                        proximoPasso();
                    }
                }
            }
            case SILENCIO, ESPERA -> {
                if (t >= silencioAte) {
                    proximoPasso();
                }
            }
            case REVELACAO -> {
                if (!revelacaoTocou && t >= 0.75f) {
                    revelacaoTocou = true;
                    AudioMarcado.tocar("revelacao_marcado", 1f, 1f);
                }
                if (t >= VfxMarcado.CORRENTES.duracao() + VfxMarcado.TITULO.duracao() + 0.5f) {
                    proximoPasso();
                }
            }
            case FIM -> {
                if (t >= (passo.duracao > 0 ? passo.duracao : 2.5f)) {
                    terminar();
                }
            }
            case SAIDA_FADE -> {
                if (t >= SAIDA_FADE_SEG) {
                    // tela toda preta: corta qualquer som que ainda esteja tocando (drone, batimento, efeitos)
                    Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MASTER);
                    corTexto = 0xFFC49A9A;
                    mostrar("marcado.segredo", Fase.DIGITANDO, 2.6f);
                }
            }
            default -> { }
        }
    }

    // ================================================================== desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float parcial) {
        long agora = System.nanoTime();
        float dt = Math.min(0.1f, (agora - ultimo) / 1_000_000_000f);
        ultimo = agora;
        if (!enviado) {
            avancarTempo(dt);
        }

        g.fill(0, 0, width, height, 0xFF000000);
        int cx = width / 2;

        // fundo: névoa e poeira em laço, bem discretas
        if (particulas > 0.01f) {
            VfxMarcado.NEVOA.desenhar(g, relogio, 0.35f * particulas, 0, 0, width, height);
            VfxMarcado.POEIRA.desenhar(g, relogio, 0.7f * particulas, 0, 0, width, height);
        }

        // sombra humanoide passando atrás do personagem
        float sombra = efeito("shadow", DURACAO_SOMBRA);
        if (sombra >= 0) {
            desenharCriatura(g, cx, sombra);
        }

        // luz no chão e o personagem girando devagar
        if (aparicao > 0f && fase != Fase.REVELACAO && fase != Fase.FIM && fase != Fase.SILENCIO) {
            desenharPersonagem(g, cx);
        }

        desenharEfeitos(g, cx);

        // vinheta
        g.fillGradient(0, 0, width, height / 3, 0xDD000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xDD000000);

        if (saindo) { // saída secreta: fade out até o preto total e fica preto durante a frase
            float k = fase == Fase.SAIDA_FADE ? Math.min(1f, t / SAIDA_FADE_SEG) : 1f;
            g.fill(0, 0, width, height, ((int) (k * 255f)) << 24);
        }

        if (fase == Fase.REVELACAO) {
            desenharRevelacao(g);
        }
        desenharTexto(g, mx, my);
        if (fase == Fase.FIM) {
            desenharTitulo(g);
        }
    }

    /**
     * "VOCÊ É UM / MARCADO / Você é livre das correntes da realidade." no fade final, sobre o circulo ritual da tela
     * inicial (simbolo_maior, 1024 px). Fontes: Cinzel Black (titulo_logo para MARCADO, titulo_hud_black para as outras
     * duas linhas) e o espaco entre as linhas segue a altura das letras (as capitais da Cinzel Black sao bem maiores
     * que a linha de texto do jogo, por isso antes as linhas se sobrepunham).
     */
    private void desenharTitulo(GuiGraphicsExtractor g) {
        float fade = Math.min(1f, t / 1.5f);
        int a = (int) (255f * fade);
        if (a < 8) {
            return; // alfa muito baixo vira opaco ou some no texto do Minecraft
        }
        int alfa = a << 24;
        int cx = width / 2;
        int cy = height / 2;

        // simbolo de fundo (o mesmo da tela inicial), respirando devagar
        float respira = 0.8f + 0.2f * (float) Math.sin(t * 1.2f);
        int tamSimbolo = Math.round(Math.min(0.84f * height, 0.70f * width));
        int sx = cx - tamSimbolo / 2;
        int sy = Math.round(cy - SIMBOLO_CENTRO * tamSimbolo);
        desenharHalo(g, cx, cy, (int) (tamSimbolo * 0.62f), 0.20f * respira * fade, 0.70f, 0.80f, 1f);
        g.blit(RenderPipelines.GUI_TEXTURED, SIMBOLO_TITULO, sx, sy, 0f, 0f, tamSimbolo, tamSimbolo, 1024, 1024, 1024, 1024,
                DesenhoOP.argb(0.40f * respira * fade, 0.85f, 0.90f, 1f));

        Style logo = estilo("titulo_logo");
        Style sub = estilo("titulo_hud_black");
        Component c1 = Component.translatable("marcado.titulo.1").withStyle(sub);
        Component c2 = Component.translatable("marcado.titulo.2").withStyle(logo);
        Component c3 = Component.translatable("marcado.titulo.3").withStyle(sub);

        // escala: MARCADO ocupa ~55% da largura (no maximo 2,2x); as outras linhas acompanham
        float sL = Math.max(0.6f, Math.min(2.2f, Math.min(width * 0.55f / Math.max(1, font.width(c2)), height / 300f)));
        float s1 = sL * 0.95f;
        float s3 = sL * 0.80f;
        float cap1 = CAP_SUB * s1;
        float capL = CAP_LOGO * sL;
        float cap3 = CAP_SUB * s3;
        float gap1 = 0.30f * capL;
        float gap2 = 0.42f * capL;
        float total = cap1 + gap1 + capL + gap2 + cap3;
        float topo = cy - total / 2f;
        float base1 = topo + cap1;
        float base2 = base1 + gap1 + capL;
        float base3 = base2 + gap2 + cap3;
        textoCentro(g, c1, cx, base1 - BASE_SUB * s1, s1, 0xD8D0D0 | alfa);
        textoCentro(g, c2, cx, base2 - BASE_LOGO * sL, sL, 0xF4EEEE | alfa);
        textoCentro(g, c3, cx, base3 - BASE_SUB * s3, s3, 0xC8BEBE | alfa);
    }

    /** Texto centralizado em (x, y) com escala (o g.text so aceita posicoes inteiras; a escala vem da matriz). */
    private void textoCentro(GuiGraphicsExtractor g, Component c, float x, float y, float escala, int cor) {
        int w = font.width(c);
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(escala, escala);
        g.text(font, c, -w / 2, 0, cor, false);
        g.pose().popMatrix();
    }

    private void desenharPersonagem(GuiGraphicsExtractor g, int cx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int pe = height / 2 + height / 8 + (int) ((zoom - 1f) * height * 0.30f);
        int escala = (int) (height / 6 * zoom);
        int corpoY = pe - (int) (escala * 0.9f); // meio do corpo

        // cor e intensidade do brilho (respira devagar; acompanha batimento, sangue, glitch e silêncio)
        float r = 0.80f, gc = 0.86f, b = 1f;
        float resp = 0.5f + 0.5f * (float) Math.sin(relogio * 1.8f);
        float alfa = 0.30f + 0.20f * resp;
        if (batimento != null && !mudo) {
            alfa += 0.25f * (float) Math.pow(Math.max(0.0, Math.sin(relogio * Math.PI * 2 * 1.1)), 6);
        }
        float sangue = efeito("pulse_red", 1.25f);
        if (sangue >= 0) {
            float k = (float) Math.sin(sangue * Math.PI);
            r += (1f - r) * k;
            gc *= 1f - 0.75f * k;
            b *= 1f - 0.75f * k;
            alfa += 0.2f * k;
        }
        int dx = 0;
        if (efeito("glitch", 0.6f) >= 0 && SORTEIO.nextInt(3) == 0) {
            dx = SORTEIO.nextInt(9) - 4;
            alfa *= 0.5f + SORTEIO.nextFloat();
        }
        if (mudo) {
            alfa *= 0.3f;
        }
        alfa = Math.min(1f, alfa);

        // halo grande atrás e um núcleo menor no peito
        desenharHalo(g, cx + dx, corpoY, (int) (escala * 2.1f), alfa * 0.8f, r, gc, b);
        desenharHalo(g, cx + dx, corpoY - escala / 4, (int) (escala * 0.9f), alfa, r, gc, b);

        float ang = relogio * 0.35f;
        if (SILHUETA_PROCEDURAL) {
            DesenhoOP.silhueta(g, cx + dx, pe, escala * 1.8f, ang, COR_SILHUETA);
        } else {
            // skin real, parada, olhando de frente (o "mouse" fica no meio do corpo)
            DesenhoOP.entidade(g, cx + dx - escala, pe - Math.round(escala * 1.9f), cx + dx + escala,
                    pe + Math.round(escala * 0.1f), escala, cx + dx, corpoY, mc.player);
        }
        if (aparicao < 1f) { // surge devagar saindo do preto
            g.fill(0, 0, width, height, ((int) ((1f - aparicao) * 255f)) << 24);
        }
    }

    /**
     * A criatura é uma silhueta PRETA, então sobre o fundo preto ela some. Por isso: um brilho frio atrás dela
     * e um feixe de luz que varre a tela da esquerda para a direita, recortando a silhueta. A animação roda em
     * câmera lenta ({@link #ESCALA_SOMBRA}).
     */
    private void desenharCriatura(GuiGraphicsExtractor g, int cx, float p) {
        int s = Math.min(width, height);
        int topo = height / 2 - s / 2 - 10;
        float env = (float) Math.sin(p * Math.PI);
        // brilho fixo atrás do corpo
        desenharHalo(g, cx, topo + s / 2, (int) (s * 0.8f), 0.30f * env, 0.70f, 0.82f, 1f);
        // feixe de luz que passa (elipse alta e estreita)
        float x = -width * 0.15f + p * width * 1.3f;
        DesenhoOP.haloEliptico(g, HALO, (int) x, height / 2, (int) (s * 0.35f), (int) (height * 0.75f), 0.55f * env, 0.85f,
                0.92f, 1f);
        VfxMarcado.SOMBRA.desenhar(g, efeitos.get("shadow") * ESCALA_SOMBRA, 0.97f, cx - s / 2, topo, s, s);
    }

    /** Brilho suave (textura radial branca) tingido; por cima do fundo preto da cena fica igual ao aditivo do Forge. */
    private void desenharHalo(GuiGraphicsExtractor g, int x, int y, int raio, float alfa, float r, float gc, float b) {
        if (alfa < 0.01f) {
            return;
        }
        DesenhoOP.halo(g, HALO, x, y, raio, alfa, r, gc, b);
    }

    private void desenharEfeitos(GuiGraphicsExtractor g, int cx) {
        float p = efeito("pulse_red", 1.25f);
        if (p >= 0) {
            int a = (int) (40 * Math.sin(p * Math.PI));
            g.fill(0, 0, width, height, (a << 24) | 0x00660000);
            int s = Math.min(width, height);
            VfxMarcado.TINTA.desenhar(g, efeitos.get("pulse_red"), 0.45f, cx - s / 2, height / 2 - s / 2, s, s);
        }
        float gl = efeito("glitch", 0.6f);
        if (gl >= 0) {
            for (int i = 0; i < 7; i++) {
                int y = SORTEIO.nextInt(height);
                int h = 2 + SORTEIO.nextInt(10);
                int x = SORTEIO.nextInt(Math.max(1, width / 2));
                g.fill(x, y, x + width / 3 + SORTEIO.nextInt(width / 3), y + h, 0x30AAFFFF);
            }
        }
        for (String el : new String[] { "conhecimento", "sangue", "morte", "energia", "medo" }) {
            float f = efeito("symbol_" + el, 0.8f);   // flash ao responder (some em menos de 1 s)
            if (f >= 0) {
                desenharSimbolo(g, cx, el, 128, 0.95f * (float) Math.sin(f * Math.PI));
            }
            float gh = efeito("ghost_" + el, 2.2f);   // sombra fraca quando a pergunta surge
            if (gh >= 0) {
                desenharSimbolo(g, cx, el, 190, 0.20f * (float) Math.sin(gh * Math.PI));
            }
        }
    }

    /** Símbolo do elemento, tingido e com brilho; só aparece por instantes (nunca fica na tela). */
    private void desenharSimbolo(GuiGraphicsExtractor g, int cx, String el, int tam, float alfa) {
        if (alfa < 0.01f) {
            return;
        }
        float r = 1f, gc = 1f, b = 1f;
        switch (el) {
            case "conhecimento" -> { r = 0.95f; gc = 0.85f; b = 0.55f; }
            case "sangue" -> { r = 0.90f; gc = 0.15f; b = 0.15f; }
            case "morte" -> { r = 0.72f; gc = 0.72f; b = 0.85f; }
            case "energia" -> { r = 0.65f; gc = 0.45f; b = 0.85f; } // roxo
            case "medo" -> { r = 0.50f; gc = 0.90f; b = 1.00f; } // ciano
            default -> { }
        }
        int cy = height / 2 - 10;
        desenharHalo(g, cx, cy, (int) (tam * 1.1f), alfa * 0.6f, r, gc, b);
        Identifier tex = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "textures/gui/marcado/simbolo_" + el + ".png");
        g.blit(RenderPipelines.GUI_TEXTURED, tex, cx - tam / 2, cy - tam / 2, 0f, 0f, tam, tam, 128, 128, 128, 128,
                DesenhoOP.argb(alfa, r, gc, b));
    }

    private void desenharRevelacao(GuiGraphicsExtractor g) {
        float corr = VfxMarcado.CORRENTES.duracao();
        int w = width;
        int h = w * 9 / 16; // os quadros são 16:9
        VfxMarcado.CORRENTES.desenhar(g, t, 1f, 0, (height - h) / 2, w, h);
        if (t > corr - 0.3f) {
            VfxMarcado.TITULO.desenhar(g, t - (corr - 0.3f), 1f, 0, (height - h) / 2, w, h);
        }
    }

    private void desenharTexto(GuiGraphicsExtractor g, int mx, int my) {
        areasOpcao.clear();
        int y = saindo ? height / 2 - linhas.size() * 13 / 2 : height * 2 / 3; // frase da saída secreta fica no centro
        if (fase == Fase.ESCOLHA && passo != null) { // com muitas opções, sobe o bloco para caber na tela
            int total = linhas.size() * 13 + 10 + passo.opcoes.size() * 15;
            if (y + total > height - 10) {
                y = Math.max(10, height - 10 - total);
            }
        }
        float shake = efeito("distort", 1.6f);
        int dx = shake >= 0 ? (int) (Math.sin(relogio * 40) * 2 * (1 - shake)) : 0;
        int restante = (fase == Fase.DIGITANDO || fase == Fase.REACAO) ? (int) (t * LETRAS_POR_SEGUNDO) : totalLetras;
        for (String linha : linhas) {
            String parte = linha.substring(0, Math.max(0, Math.min(linha.length(), restante)));
            restante -= linha.length();
            Component c = Component.literal(parte).withStyle(FONTE_ENTIDADE);
            g.text(font, c, width / 2 - font.width(Component.literal(linha).withStyle(FONTE_ENTIDADE)) / 2 + dx, y,
                    corTexto, false);
            y += 13;
        }
        if (fase == Fase.ESCOLHA && passo != null) {
            y += 10;
            for (int i = 0; i < passo.opcoes.size(); i++) {
                boolean ativa = i == sel;
                String texto = (ativa ? "› " : "  ") + Component.translatable(passo.opcoes.get(i).texto).getString();
                Component c = Component.literal(texto).withStyle(FONTE_ENTIDADE);
                int w = font.width(c);
                int x = width / 2 - w / 2;
                areasOpcao.add(new int[] { x, y - 2, x + w, y + 11 });
                g.text(font, c, x, y, ativa ? 0xFFFFFFFF : 0xFF8A8484, false);
                y += 15;
            }
        }
    }

    // ================================================================== entrada

    @Override
    public boolean keyPressed(KeyEvent evento) {
        int tecla = evento.key();
        if (fase == Fase.ESCOLHA) {
            int n = passo.opcoes.size();
            if (tecla == GLFW.GLFW_KEY_W || tecla == GLFW.GLFW_KEY_UP) {
                sel = (sel + n - 1) % n;
                AudioMarcado.tocar("ui_hover", 0.5f, 1f);
                return true;
            }
            if (tecla == GLFW.GLFW_KEY_S || tecla == GLFW.GLFW_KEY_DOWN) {
                sel = (sel + 1) % n;
                AudioMarcado.tocar("ui_hover", 0.5f, 1f);
                return true;
            }
            if (tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_KP_ENTER || tecla == GLFW.GLFW_KEY_SPACE) {
                escolher(sel);
                return true;
            }
        } else if (tecla == GLFW.GLFW_KEY_ENTER || tecla == GLFW.GLFW_KEY_SPACE) {
            pular();
            return true;
        }
        return super.keyPressed(evento); // ESC não faz nada (shouldCloseOnEsc = false)
    }

    /** O ESC é contado ao soltar a tecla, assim segurar a tecla (repetição automática) vale um só. */
    @Override
    public boolean keyReleased(KeyEvent evento) {
        if (evento.key() == GLFW.GLFW_KEY_ESCAPE) {
            contarEsc();
            return true;
        }
        return super.keyReleased(evento);
    }

    /** Enter durante a digitação mostra o texto todo; durante a pausa, passa adiante. */
    private void pular() {
        if (fase == Fase.DIGITANDO || fase == Fase.REACAO) {
            t = totalLetras / LETRAS_POR_SEGUNDO + 0.01f;
        } else if (fase == Fase.PAUSA) {
            t = pausaFinal;
        }
    }

    @Override
    public void mouseMoved(double mx, double my) {
        if (fase != Fase.ESCOLHA) {
            return;
        }
        for (int i = 0; i < areasOpcao.size(); i++) {
            int[] a = areasOpcao.get(i);
            if (mx >= a[0] && mx <= a[2] && my >= a[1] && my <= a[3] && sel != i) {
                sel = i;
                AudioMarcado.tocar("ui_hover", 0.5f, 1f);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent evento, boolean duploClique) {
        double mx = evento.x();
        double my = evento.y();
        if (fase == Fase.ESCOLHA) {
            for (int i = 0; i < areasOpcao.size(); i++) {
                int[] a = areasOpcao.get(i);
                if (mx >= a[0] && mx <= a[2] && my >= a[1] && my <= a[3]) {
                    escolher(i);
                    return true;
                }
            }
        } else {
            pular();
        }
        return true;
    }
}
