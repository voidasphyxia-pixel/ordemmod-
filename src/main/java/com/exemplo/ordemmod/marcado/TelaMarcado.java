package com.exemplo.ordemmod.marcado;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.joml.Quaternionf;
import org.lwjgl.glfw.GLFW;

import com.exemplo.ordemmod.OrdemMod;
import com.exemplo.ordemmod.Rede;
import com.exemplo.ordemmod.TelaCriacao;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * A cena do Marcado: introdução narrativa de terror que vem ANTES da tela de criação de personagem.
 * Lê o roteiro (RoteiroMarcado), mostra uma fala ou pergunta por vez, dispara efeitos e, no fim,
 * manda as escolhas ao servidor (PacoteMarcado) e abre a TelaCriacao normal.
 *
 * ESTADO: esqueleto jogável, ainda sem teste no jogo. Veja o LEIA-ME-MARCADO.md (o que falta ajustar).
 */
public class TelaMarcado extends Screen {
    private static final Style FONTE_ENTIDADE = Style.EMPTY.withFont(new ResourceLocation(OrdemMod.MOD_ID, "entidade"));
    private static final ResourceLocation BRANCO = new ResourceLocation(OrdemMod.MOD_ID, "textures/gui/marcado/branco.png");
    private static final ResourceLocation HALO = new ResourceLocation(OrdemMod.MOD_ID, "textures/gui/marcado/halo.png");
    private static final float LETRAS_POR_SEGUNDO = 22f;
    private static final float PAUSA_PADRAO = 1.2f;
    private static final Random SORTEIO = new Random();

    private enum Fase { DIGITANDO, PAUSA, ESCOLHA, REACAO, SILENCIO, REVELACAO, FIM }

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

    public TelaMarcado() {
        super(Component.literal("Marcado"));
    }

    @Override
    protected void init() {
        if (idx < 0) {
            drone = new AudioMarcado.Laco("ambiente_drone", 0.45f);
            drone.iniciar();
            AudioMarcado.tocar("tum", 1f, 1f);
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
        VfxMarcado.liberarTodos();
    }

    /** Volta o drone e o batimento depois de um "silence" (a Morte tira o som só por um momento). */
    private void restaurarSom() {
        if (mudo) {
            mudo = false;
            if (drone != null) {
                drone.volumeAlvo(0.45f);
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
        if (idx >= roteiro.passos.size()) {
            terminar();
            return;
        }
        passo = roteiro.passos.get(idx);
        if (!"silencio".equals(passo.tipo)) {
            restaurarSom(); // o silêncio de uma resposta vale até o fim da reação, não para sempre
        }
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
            case "revelacao" -> {
                fase = Fase.REVELACAO;
                linhas = new ArrayList<>();
                revelacaoTocou = false;
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
        AudioMarcado.tocar("ui_select", 0.6f, 1f);
        disparar(escolhida.efeito);
        if (escolhida.reacao != null) {
            AudioMarcado.tocar("reacao_entidade", 0.8f, 1f);
            mostrar(escolhida.reacao, Fase.REACAO, PAUSA_PADRAO);
        } else {
            proximoPasso();
        }
    }

    private void terminar() {
        if (enviado) {
            return;
        }
        enviado = true;
        Rede.CANAL.sendToServer(new PacoteMarcado(escolhas));
        Minecraft.getInstance().setScreen(new TelaCriacao());
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
        switch (fase) {
            case DIGITANDO, REACAO -> {
                int mostradas = Math.min(totalLetras, (int) (t * LETRAS_POR_SEGUNDO));
                while (letrasTocadas < mostradas) {
                    if (letrasTocadas % 3 == 0) {
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
                    if (passo != null && "pergunta".equals(passo.tipo) && escolhida == null) {
                        fase = Fase.ESCOLHA;
                        t = 0;
                    } else {
                        proximoPasso();
                    }
                }
            }
            case SILENCIO -> {
                if (t >= silencioAte) {
                    restaurarSom();
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
            default -> { }
        }
    }

    // ================================================================== desenho

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        long agora = System.nanoTime();
        float dt = Math.min(0.1f, (agora - ultimo) / 1_000_000_000f);
        ultimo = agora;
        if (!enviado) {
            avancarTempo(dt);
        }

        g.fill(0, 0, width, height, 0xFF000000);
        int cx = width / 2;

        // fundo: névoa e poeira em laço, bem discretas
        VfxMarcado.NEVOA.desenhar(g, relogio, 0.35f, 0, 0, width, height);
        VfxMarcado.POEIRA.desenhar(g, relogio, 0.7f, 0, 0, width, height);

        // sombra humanoide passando atrás do personagem
        float sombra = efeito("shadow", VfxMarcado.SOMBRA.duracao());
        if (sombra >= 0) {
            int s = Math.min(width, height);
            VfxMarcado.SOMBRA.desenhar(g, efeitos.get("shadow"), 0.85f, cx - s / 2, height / 2 - s / 2 - 10, s, s);
        }

        // luz no chão e o personagem girando devagar
        if (fase != Fase.REVELACAO && fase != Fase.FIM && fase != Fase.SILENCIO) {
            desenharPersonagem(g, cx);
        }

        desenharEfeitos(g, cx);

        // vinheta
        g.fillGradient(0, 0, width, height / 3, 0xDD000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xDD000000);

        if (fase == Fase.REVELACAO) {
            desenharRevelacao(g);
        }
        desenharTexto(g, mx, my);
    }

    private void desenharPersonagem(GuiGraphics g, int cx) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        int pe = height / 2 + height / 8;
        int escala = height / 6;
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
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).rotateY(ang);
        Quaternionf camera = new Quaternionf().rotateX(-0.12f);
        desenharSilhueta(g, cx + dx, pe, escala, pose, camera, mc.player);
    }

    /** Brilho suave (textura radial branca), somado à imagem de trás. */
    private void desenharHalo(GuiGraphics g, int x, int y, int raio, float alfa, float r, float gc, float b) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.setColor(r, gc, b, alfa);
        g.blit(HALO, x - raio, y - raio, raio * 2, raio * 2, 0f, 0f, 128, 128, 128, 128);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * Igual ao InventoryScreen.renderEntityInInventory, mas todo o modelo (corpo, roupa, itens) é desenhado
     * com uma textura branca única, então vira uma silhueta clara sem rosto nem skin.
     */
    private void desenharSilhueta(GuiGraphics g, int x, int y, int escala, Quaternionf pose, Quaternionf camera,
            LivingEntity entidade) {
        g.pose().pushPose();
        g.pose().translate((double) x, (double) y, 50.0D);
        g.pose().mulPoseMatrix(new Matrix4f().scaling((float) escala, (float) escala, (float) -escala));
        g.pose().mulPose(pose);
        Lighting.setupForEntityInInventory();
        EntityRenderDispatcher disp = Minecraft.getInstance().getEntityRenderDispatcher();
        camera.conjugate();
        disp.overrideCameraOrientation(camera);
        disp.setRenderShadow(false);
        MultiBufferSource fonte = tipo -> g.bufferSource().getBuffer(RenderType.entityCutoutNoCull(BRANCO));
        RenderSystem.runAsFancy(() -> disp.render(entidade, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, g.pose(), fonte, 15728880));
        g.flush();
        disp.setRenderShadow(true);
        g.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private void desenharEfeitos(GuiGraphics g, int cx) {
        float p = efeito("pulse_red", 1.25f);
        if (p >= 0) {
            int a = (int) (70 * Math.sin(p * Math.PI));
            g.fill(0, 0, width, height, (a << 24) | 0x00660000);
            int s = Math.min(width, height);
            VfxMarcado.TINTA.desenhar(g, efeitos.get("pulse_red"), 0.6f, cx - s / 2, height / 2 - s / 2, s, s);
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
            float s = efeito("symbol_" + el, 0.35f);
            if (s >= 0) {
                ResourceLocation tex = new ResourceLocation(OrdemMod.MOD_ID, "textures/gui/marcado/simbolo_" + el + ".png");
                float a = (float) Math.sin(s * Math.PI);
                g.setColor(1f, 1f, 1f, a);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                g.blit(tex, cx - 48, height / 2 - 48, 96, 96, 0f, 0f, 128, 128, 128, 128);
                g.setColor(1f, 1f, 1f, 1f);
            }
        }
    }

    private void desenharRevelacao(GuiGraphics g) {
        float corr = VfxMarcado.CORRENTES.duracao();
        int w = width;
        int h = w * 9 / 16; // os quadros são 16:9
        VfxMarcado.CORRENTES.desenhar(g, t, 1f, 0, (height - h) / 2, w, h);
        if (t > corr - 0.3f) {
            VfxMarcado.TITULO.desenhar(g, t - (corr - 0.3f), 1f, 0, (height - h) / 2, w, h);
        }
    }

    private void desenharTexto(GuiGraphics g, int mx, int my) {
        areasOpcao.clear();
        int y = height * 2 / 3;
        float shake = efeito("distort", 1.6f);
        int dx = shake >= 0 ? (int) (Math.sin(relogio * 40) * 2 * (1 - shake)) : 0;
        int restante = (fase == Fase.DIGITANDO || fase == Fase.REACAO) ? (int) (t * LETRAS_POR_SEGUNDO) : totalLetras;
        for (String linha : linhas) {
            String parte = linha.substring(0, Math.max(0, Math.min(linha.length(), restante)));
            restante -= linha.length();
            Component c = Component.literal(parte).withStyle(FONTE_ENTIDADE);
            g.drawString(font, c, width / 2 - font.width(Component.literal(linha).withStyle(FONTE_ENTIDADE)) / 2 + dx, y,
                    0xFFD8D0D0, false);
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
                g.drawString(font, c, x, y, ativa ? 0xFFFFFFFF : 0xFF8A8484, false);
                y += 15;
            }
        }
    }

    // ================================================================== entrada

    @Override
    public boolean keyPressed(int tecla, int scan, int mods) {
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
        return super.keyPressed(tecla, scan, mods); // ESC não faz nada (shouldCloseOnEsc = false)
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
    public boolean mouseClicked(double mx, double my, int botao) {
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
