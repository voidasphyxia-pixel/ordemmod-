package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * A HUD do mod, no mesmo visual dos menus e da cena do Marcado (TelaCriacao, TelaFicha): formas desenhadas "na mão",
 * painéis retos de vidro escuro translúcido com borda fina, cantos de ritual dourados, degradês, brilho neon,
 * rótulos em Cinzel, vinheta escura no pé da tela e o símbolo do medo (ciano) entre as barras.
 * Barras empilhadas em cima da hotbar (de baixo para cima):
 * XP (verde, fina) / NEX (branco, 20 células) / vida (vermelho) + fome (marrom) /
 * PE (amarelo) + armadura (cinza) / sanidade (azul). No meio das duas colunas (PE/DEF e PV/FOME) fica o símbolo do medo.
 * Os corações, a fome, a armadura, a barra de XP e a hotbar do jogo não são mais desenhados (a hotbar é refeita aqui). Só existe no cliente.
 *
 * Como desenha: primeiro TODA a geometria num lote só ({@link Pincel}); depois todos os textos.
 * Cada barra é um paralelogramo descrito por {@link Forma}, com coordenadas (u, v) de 0 a 1.
 */
public final class HudOrdem {
    private HudOrdem() {
    }

    // ------------------------------------------------------------------ medidas (em pixels da interface)
    private static final int LARGURA = 182;      // a mesma da hotbar
    /** Vão entre as duas colunas de barras (PE/DEF e PV/FOME): é onde fica o símbolo do medo. */
    private static final int VAO_CENTRAL = 20;
    private static final int METADE = (LARGURA - VAO_CENTRAL) / 2; // duas barras lado a lado: 81 + 20 + 81 = 182
    private static final int ALTURA = 7;
    private static final int PASSO = ALTURA + 1; // altura da barra + 1 de espaço
    private static final int BASE = 43;          // a barra de NEX fica 43 acima do fim da tela
    /** Espaço que a pilha ocupa acima da base da tela; o que o jogo desenha em cima (ar) sobe para cá. */
    private static final int ALTURA_PILHA = BASE + 3 * PASSO + 11;
    /** O nome do item e a mensagem da barra de ação sobem para não ficar atrás das barras. */
    private static final int DESLOCAMENTO_TEXTOS = 22;

    private static final float INCLINACAO = 0F;      // barras retas, como os painéis dos menus (3F devolve o corte inclinado)
    private static final int ALTURA_XP = 7;
    private static final float INCLINACAO_XP = 0F;
    private static final int Y_XP = 31;              // a barra de XP fica 31 acima do fim da tela
    /** Escala dos números dentro das barras (Tektur). */
    private static final float ESCALA_TEXTO = 0.72F;
    /**
     * Rótulos (SAN, PE, DEF, PV, FOME) em Cinzel BLACK (font/titulo_hud_black.json), a família dos menus no peso mais
     * grosso. A Cinzel Regular ficou ilegível em barras de 7 px (traço fino demais). Com false volta para Tektur,
     * igual aos números (ESCALA_TEXTO). Se a Black ficar pequena/grande, ajuste ESCALA_ROTULO.
     */
    private static final boolean ROTULO_CINZEL = true;
    private static final float ESCALA_ROTULO = 0.66F;
    /**
     * Onde fica o centro visual das letras em relação ao topo da linha de texto (não escalada). Na fonte Tektur com o
     * deslocamento do hud.json as letras ficam um pouco ACIMA da linha, por isso o valor é negativo. Se o texto
     * estiver alto demais na barra, deixe mais negativo; se estiver baixo demais, aproxime de zero.
     */
    private static final float CENTRO_GLIFO = -0.6F;
    /** Mesmo ajuste para a Cinzel: nos menus o meio das letras fica ~0,5 px ABAIXO do y, por isso o valor é positivo. */
    private static final float CENTRO_GLIFO_TITULO = 0.5F;

    // ------------------------------------------------------------------ cores (ARGB), as mesmas dos menus e da cena do Marcado
    private static final int COR_OSSO = 0xFFEDE8DC;
    private static final int COR_DOURADO = 0xFFC2B48C;
    private static final int COR_HALO = 0x99000000;
    /** Vidro escuro translúcido dos painéis (o mesmo tom do COR_PAINEL da TelaCriacao, topo e base). */
    private static final int COR_VIDRO_TOPO = 0xC80E0E16;
    private static final int COR_VIDRO_BASE = 0xDC050508;

    // símbolo do medo: ciano, o mesmo tom da cena do Marcado (0.50, 0.90, 1.00)
    private static final ResourceLocation SIMBOLO_MEDO = new ResourceLocation(OrdemMod.MOD_ID,
            "textures/gui/hud_simbolo_medo.png");
    private static final ResourceLocation HALO_TEX = new ResourceLocation(OrdemMod.MOD_ID,
            "textures/gui/marcado/halo.png");
    private static final int SIMBOLO_L = 11;         // tamanho desenhado (a textura tem 22x34, o dobro, para suavizar)
    private static final int SIMBOLO_A = 17;         // 2 barras (7+1+7) + 1 px de vão em cima e embaixo
    private static final float MEDO_R = 0.50F;
    private static final float MEDO_G = 0.90F;
    private static final float MEDO_B = 1.00F;

    private static final int COR_VIDA = 0xFFC8202E;
    private static final int COR_FOME = 0xFF8B5A2B;
    private static final int COR_SANIDADE = 0xFF2F6FDB;
    private static final int COR_ESFORCO = 0xFFE6C229;
    private static final int COR_NEX = 0xFFF2F2F2;
    private static final int COR_ARMADURA = 0xFF9A9A9A;
    private static final int COR_XP = 0xFF6FBF73;

    private static final int ARMADURA_MAXIMA = 20;
    private static final int FOME_MAXIMA = 20;
    private static final int PARTES_NEX = 20;
    /** Abaixo disso (25%) a borda da barra pisca e o brilho pulsa, avisando que está acabando. */
    private static final float LIMITE_ALERTA = 0.25F;

    // índices do "rastro" de cada barra (o pedaço que acabou de ser perdido e vai sumindo devagar)
    private static final int B_SANIDADE = 0;
    private static final int B_ESFORCO = 1;
    private static final int B_ARMADURA = 2;
    private static final int B_VIDA = 3;
    private static final int B_FOME = 4;
    private static final float[] RASTRO = new float[5];
    private static long ultimoQuadro;
    private static float msDoQuadro;

    /** Último estado mandado pelo servidor (null = ainda não chegou, a HUD não aparece). */
    private static PacoteStatus status;
    private static boolean textosDeslocados;

    public static void receber(PacoteStatus novo) {
        status = novo;
    }

    /** A HUD só aparece para quem vê os elementos de sobrevivência (não no criativo nem no espectador). */
    static boolean ativo() {
        Minecraft mc = Minecraft.getInstance();
        return status != null && mc.player != null && mc.gameMode != null
                && mc.gameMode.canHurtPlayer() && mc.getCameraEntity() instanceof Player;
    }

    /** Y do topo da barra de sanidade (a de cima da pilha): o painel da rolagem fica logo acima dele. */
    static int yTopoDasBarras(int altura) {
        return altura - BASE - 3 * PASSO;
    }

    // ================================================================== desenho

    /** Um texto para escrever depois da geometria. alinhamento: 0 esquerda, 1 centro, 2 direita. titulo = Cinzel. */
    private record Texto(String conteudo, float x, float y, float escala, int cor, int alinhamento, boolean titulo) {
    }

    private static final List<Texto> TEXTOS = new ArrayList<>();
    private static final float[][] HALO = {
            {-0.9F, 0F}, {0.9F, 0F}, {0F, -0.9F}, {0F, 0.9F},
            {-0.65F, -0.65F}, {0.65F, -0.65F}, {-0.65F, 0.65F}, {0.65F, 0.65F}};

    public static void desenhar(ForgeGui gui, GuiGraphics g, float parcial, int largura, int altura) {
        if (!ativo()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player jogador = mc.player;
        PacoteStatus s = status;

        long agora = Util.getMillis();
        msDoQuadro = ultimoQuadro == 0L ? 0F : Math.min(100L, agora - ultimoQuadro);
        ultimoQuadro = agora;

        int x = largura / 2 - LARGURA / 2;
        int xDireita = x + METADE + VAO_CENTRAL;
        int yNex = altura - BASE;
        int yVida = yNex - PASSO;
        int yEsforco = yVida - PASSO;
        int ySanidade = yEsforco - PASSO;

        TEXTOS.clear();
        g.flush(); // termina o que já foi pedido antes de desenhar "na mão"
        Pincel p = new Pincel(g);

        // vinheta no pé da tela (a mesma ideia dos menus: o fundo escurece perto da borda), por trás de tudo
        vinheta(p, largura, altura);

        // sanidade (largura total, no topo)
        barra(p, B_SANIDADE, x, ySanidade, LARGURA, s.sanidade(), s.sanidadeMax(), COR_SANIDADE, "SAN", true);
        // pontos de esforço + armadura
        barra(p, B_ESFORCO, x, yEsforco, METADE, s.pe(), s.peMax(), COR_ESFORCO, "PE", false);
        barra(p, B_ARMADURA, xDireita, yEsforco, METADE, jogador.getArmorValue(), ARMADURA_MAXIMA,
                COR_ARMADURA, "DEF", false);
        // vida + fome
        barra(p, B_VIDA, x, yVida, METADE, (int) Math.ceil(jogador.getHealth()),
                (int) Math.ceil(jogador.getMaxHealth()), COR_VIDA, "PV", true);
        barra(p, B_FOME, xDireita, yVida, METADE, jogador.getFoodData().getFoodLevel(), FOME_MAXIMA,
                COR_FOME, "FOME", true);
        // NEX: 20 células, uma a cada 5%
        desenharNex(p, x, yNex, s);
        // XP (a do jogo não é mais desenhada); no cavalo o jogo mostra a barra de pulo no lugar
        if (mc.player.jumpableVehicle() == null) {
            desenharXp(p, x, altura - Y_XP, largura / 2F, jogador);
        }

        // hotbar no mesmo estilo (o jogo não desenha mais a dele)
        desenharHotbar(p, largura, altura, jogador);

        // cantos de ritual dourados em volta de todo o bloco de barras (de SAN até NEX), como nos painéis dos menus
        cantos(p, x, ySanidade, LARGURA, yNex + ALTURA - ySanidade, 0xCCC2B48C, 4F);

        p.fim();
        // símbolo do medo no vão entre as colunas (PE/DEF e PV/FOME)
        desenharSimboloMedo(g, x + METADE + VAO_CENTRAL / 2F, yEsforco + (2 * ALTURA + 1) / 2F);
        for (Texto t : TEXTOS) {
            escrever(g, mc.font, t);
        }
        g.flush();
        desenharItensHotbar(g, mc, largura, altura, jogador);
    }

    /** Escurece de leve o pé da tela (transparente em cima, ~40% de preto embaixo), como a vinheta dos menus. */
    private static void vinheta(Pincel p, int largura, int altura) {
        float topo = altura - 100F;
        p.quad(0F, topo, 0x00000000, largura, topo, 0x00000000, largura, altura, 0x66000000, 0F, altura, 0x66000000);
    }

    /**
     * O símbolo do medo (ciano) no centro do vão entre as colunas: um brilho suave por trás e o desenho por cima, os
     * dois respirando devagar, igual ao halo da silhueta nos menus. A textura é desenhada com filtro suave, senão o
     * desenho (fino) fica serrilhado nesse tamanho.
     */
    private static void desenharSimboloMedo(GuiGraphics g, float cx, float cy) {
        Minecraft mc = Minecraft.getInstance();
        float respira = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 900.0);
        g.flush();
        RenderSystem.enableBlend();
        // brilho: textura radial branca somada ao fundo
        int raio = 15;
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.setColor(MEDO_R, MEDO_G, MEDO_B, 0.28F + 0.17F * respira);
        g.blit(HALO_TEX, Math.round(cx) - raio, Math.round(cy) - raio, raio * 2, raio * 2, 0F, 0F, 128, 128, 128, 128);
        // o desenho
        RenderSystem.defaultBlendFunc();
        mc.getTextureManager().getTexture(SIMBOLO_MEDO).setFilter(true, false);
        g.setColor(MEDO_R, MEDO_G, MEDO_B, 0.80F + 0.20F * respira);
        g.blit(SIMBOLO_MEDO, Math.round(cx - SIMBOLO_L / 2F), Math.round(cy - SIMBOLO_A / 2F), SIMBOLO_L, SIMBOLO_A,
                0F, 0F, 22, 34, 22, 34);
        g.setColor(1F, 1F, 1F, 1F);
        g.flush();
    }

    // ---------------------------------------------------------------- hotbar

    private static final float SLOT = 18F;           // lado de cada casa (o item tem 16, sobra 1 de cada lado)
    private static final float PASSO_SLOT = 20F;     // casa + 2 de vão
    private static final float Y_SLOT_OFFSET = 20F;  // topo das casas, acima do fim da tela

    /** Posição (x) da mão secundária: do lado oposto à mão principal, como no jogo. */
    private static float xMaoSecundaria(int largura) {
        boolean canhoto = Minecraft.getInstance().options.mainHand().get() == HumanoidArm.LEFT;
        return canhoto ? largura / 2F + 89F + 4F : largura / 2F - 89F - 4F - SLOT;
    }

    /** Casas de vidro escuro com borda fina; a selecionada ganha borda clara e brilho. */
    private static void desenharHotbar(Pincel p, int largura, int altura, Player jogador) {
        int sel = jogador.getInventory().selected;
        float x0 = largura / 2F - 89F;
        float y0 = altura - Y_SLOT_OFFSET;
        for (int i = 0; i < 9; i++) {
            if (i != sel) {
                casa(p, x0 + i * PASSO_SLOT, y0, false);
            }
        }
        if (!jogador.getOffhandItem().isEmpty()) {
            casa(p, xMaoSecundaria(largura), y0, false);
        }
        casa(p, x0 + sel * PASSO_SLOT, y0, true); // por último, para o brilho ficar por cima das vizinhas
    }

    private static void casa(Pincel p, float x, float y, boolean selecionada) {
        Forma f = new Forma(x, y, SLOT, SLOT, 0F);
        painel(p, f);
        if (selecionada) {
            float pulso = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 350.0);
            brilho(p, f, 0F, 1F, COR_OSSO, 3F, 0x48 + Math.round(0x20 * pulso));
            f.celula(p, 0F, 1F, 0F, 1F, 0x30FFFFFF, 0x30FFFFFF, 0x10FFFFFF, 0x10FFFFFF);
            moldura(p, f, COR_OSSO, 1F);
            cantos(p, x, y, SLOT, SLOT, COR_DOURADO, 3F); // a casa escolhida ganha os cantos dourados
        } else {
            moldura(p, f, COR_OSSO, 0F);
        }
    }

    /** Os itens (e a quantidade) por cima das casas, depois de todo o resto. */
    private static void desenharItensHotbar(GuiGraphics g, Minecraft mc, int largura, int altura, Player jogador) {
        float x0 = largura / 2F - 89F;
        int iy = Math.round(altura - Y_SLOT_OFFSET + 1F);
        for (int i = 0; i < 9; i++) {
            ItemStack item = jogador.getInventory().items.get(i);
            if (!item.isEmpty()) {
                int ix = Math.round(x0 + 1F + i * PASSO_SLOT);
                g.renderItem(jogador, item, ix, iy, i);
                g.renderItemDecorations(mc.font, item, ix, iy);
            }
        }
        ItemStack mao = jogador.getOffhandItem();
        if (!mao.isEmpty()) {
            int ix = Math.round(xMaoSecundaria(largura) + 1F);
            g.renderItem(jogador, mao, ix, iy, 10);
            g.renderItemDecorations(mc.font, mao, ix, iy);
        }
        g.flush();
    }

    /**
     * Uma barra: rótulo à esquerda e "atual/máximo" à direita. O preenchimento é a porcentagem que o atual é do
     * máximo. Quando o valor cai, o pedaço perdido fica um instante em branco translúcido e some devagar.
     */
    private static void barra(Pincel p, int indice, float x, float y, float w, int atual, int maximo, int cor,
            String rotulo, boolean avisaBaixo) {
        Forma f = new Forma(x, y, w, ALTURA, INCLINACAO);
        float fracao = maximo > 0 ? Mth.clamp(atual / (float) maximo, 0F, 1F) : 0F;
        float rastro = rastro(indice, fracao);
        boolean alerta = avisaBaixo && maximo > 0 && fracao <= LIMITE_ALERTA;
        float pulso = alerta ? 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 170.0) : 0F;

        float uI = 1.3F / f.bw;
        float vI = 1.3F / f.h;
        float util = 1F - 2F * uI;
        float uCheio = uI + util * fracao;
        float uRastro = uI + util * rastro;

        painel(p, f);
        if (fracao > 0F) {
            brilho(p, f, uI, uCheio, cor, 3.2F, 0x40 + Math.round(0x38 * pulso));
        }
        if (rastro > fracao) {
            f.celula(p, uCheio, uRastro, vI, 1F - vI, 0x78FFFFFF, 0x78FFFFFF, 0x38FFFFFF, 0x38FFFFFF);
        }
        if (fracao > 0F) {
            preencher(p, f, uI, uCheio, vI, cor, fracao < 1F);
        }
        graduacoes(p, f, uI, vI);
        moldura(p, f, cor, pulso);

        // rótulo (bem claro, para ler sobre qualquer cor de barra) e o "atual/máximo" em Tektur
        TEXTOS.add(new Texto(rotulo, x + INCLINACAO / 2F + 4F,
                ROTULO_CINZEL ? yTextoTituloCentrado(y, ALTURA) : yTextoCentrado(y, ALTURA),
                ROTULO_CINZEL ? ESCALA_ROTULO : ESCALA_TEXTO, misturar(cor, 0xFFFFFFFF, 0.8F), 0, ROTULO_CINZEL));
        TEXTOS.add(new Texto(atual + "/" + maximo, x + w - INCLINACAO / 2F - 4F, yTextoCentrado(y, ALTURA),
                ESCALA_TEXTO, COR_OSSO, 2, false));
    }

    /** Três marcas escuras discretas (25%, 50%, 75%) por cima do preenchimento, como a régua de um instrumento. */
    private static void graduacoes(Pincel p, Forma f, float uI, float vI) {
        float util = 1F - 2F * uI;
        for (int k = 1; k <= 3; k++) {
            float u = uI + util * k / 4F;
            f.traco(p, u, vI, u, 1F - vI, 0.8F, 0x30000000);
        }
    }

    /** NEX: 20 células inclinadas com um vão entre elas. Cada 5% de NEX acende mais uma. */
    private static void desenharNex(Pincel p, float x, float y, PacoteStatus s) {
        Forma f = new Forma(x, y, LARGURA, ALTURA, INCLINACAO);
        int partes = Mth.clamp(s.nivel(), 0, PARTES_NEX); // nível = NEX / 5 (99% = 20)
        float uI = 1.3F / f.bw;
        float vI = 1.3F / f.h;
        float passo = (1F - 2F * uI) / PARTES_NEX;
        float vao = 1.2F / f.bw;

        painel(p, f);
        if (partes > 0) {
            brilho(p, f, uI, uI + passo * partes, COR_NEX, 3.2F, 0x30);
        }
        for (int i = 0; i < PARTES_NEX; i++) {
            float u0 = uI + passo * i + vao / 2F;
            float u1 = uI + passo * (i + 1) - vao / 2F;
            if (i < partes) {
                f.celula(p, u0, u1, vI, 1F - vI, 0xFFFFFFFF, 0xFFFFFFFF, 0xFF9C9CAA, 0xFF9C9CAA);
            } else {
                f.celula(p, u0, u1, vI, 1F - vI, 0xA0262632, 0xA0262632, 0xA0131319, 0xA0131319);
            }
        }
        moldura(p, f, COR_NEX, 0F);
        TEXTOS.add(new Texto(s.nex() + "%", f.px(0.5F, 0.5F), yTextoCentrado(y, ALTURA),
                ESCALA_TEXTO, 0xFFFFFFFF, 1, false));
    }

    /** Y em que o texto deve começar para as letras ficarem centralizadas na vertical numa barra de {@code altura}. */
    private static float yTextoCentrado(float y, float altura) {
        return y + altura / 2F - ESCALA_TEXTO * CENTRO_GLIFO;
    }

    /** O mesmo para os rótulos em Cinzel. */
    private static float yTextoTituloCentrado(float y, float altura) {
        return y + altura / 2F - ESCALA_ROTULO * CENTRO_GLIFO_TITULO;
    }

    /** XP: uma linha fina e luminosa no mesmo estilo, com o número do nível centralizado dentro dela. */
    private static void desenharXp(Pincel p, float x, float y, float centroX, Player jogador) {
        Forma f = new Forma(x, y, LARGURA, ALTURA_XP, INCLINACAO_XP);
        float fracao = Mth.clamp(jogador.experienceProgress, 0F, 1F);
        float uI = 1F / f.bw;
        float vI = 1F / f.h;
        float uCheio = uI + (1F - 2F * uI) * fracao;

        painel(p, f);
        if (fracao > 0F) {
            brilho(p, f, uI, uCheio, COR_XP, 3F, 0x38);
            preencher(p, f, uI, uCheio, vI, COR_XP, fracao < 1F);
        }
        moldura(p, f, COR_XP, 0F);
        if (jogador.experienceLevel > 0) {
            TEXTOS.add(new Texto(String.valueOf(jogador.experienceLevel), centroX, yTextoCentrado(y, ALTURA_XP), ESCALA_TEXTO,
                    misturar(COR_XP, 0xFFFFFFFF, 0.5F), 1, false));
        }
    }

    // ---------------------------------------------------------------- peças

    /** Fundo de "vidro": escuro e levemente translúcido, um pouco mais claro em cima. */
    private static void painel(Pincel p, Forma f) {
        f.celula(p, 0F, 1F, 0F, 1F, COR_VIDRO_TOPO, COR_VIDRO_TOPO, COR_VIDRO_BASE, COR_VIDRO_BASE);
    }

    /**
     * Pequenos colchetes nos quatro cantos, por FORA do retângulo (x, y, w, h) em 1 px, como os "cantos de ritual"
     * dourados dos painéis da TelaCriacao/TelaFicha. {@code t} é o comprimento de cada braço.
     */
    private static void cantos(Pincel p, float x, float y, float w, float h, int cor, float t) {
        p.ret(x - 1F, y - 1F, x + t, y, cor);
        p.ret(x - 1F, y - 1F, x, y + t, cor);
        p.ret(x + w - t, y - 1F, x + w + 1F, y, cor);
        p.ret(x + w, y - 1F, x + w + 1F, y + t, cor);
        p.ret(x - 1F, y + h, x + t, y + h + 1F, cor);
        p.ret(x - 1F, y + h - t, x, y + h + 1F, cor);
        p.ret(x + w - t, y + h, x + w + 1F, y + h + 1F, cor);
        p.ret(x + w, y + h - t, x + w + 1F, y + h + 1F, cor);
    }

    /** A parte cheia: degradê da cor, reflexo de vidro na metade de cima e um fio de luz na ponta. */
    private static void preencher(Pincel p, Forma f, float u0, float u1, float vI, int cor, boolean comPonta) {
        int topoEsq = misturar(misturar(cor, 0xFF000000, 0.15F), 0xFFFFFFFF, 0.30F);
        int topoDir = misturar(cor, 0xFFFFFFFF, 0.35F);
        int baseDir = misturar(cor, 0xFF000000, 0.45F);
        int baseEsq = misturar(cor, 0xFF000000, 0.62F);
        f.celula(p, u0, u1, vI, 1F - vI, topoEsq, topoDir, baseDir, baseEsq);
        f.celula(p, u0, u1, vI, 0.5F, 0x3CFFFFFF, 0x3CFFFFFF, 0x08FFFFFF, 0x08FFFFFF); // reflexo
        if (comPonta) {
            f.traco(p, u1, vI, u1, 1F - vI, 1F, 0xC8FFFFFF);
        }
    }

    /** Borda fina: mais clara em cima, mais escura embaixo. Em alerta, pulsa na cor da barra. */
    private static void moldura(Pincel p, Forma f, int cor, float pulso) {
        int topo = misturar(0xFF42424E, cor, 0.9F * pulso);
        int lados = misturar(0xFF2A2A33, cor, 0.9F * pulso);
        int base = misturar(0xFF17171D, cor, 0.9F * pulso);
        f.traco(p, 0F, 0F, 1F, 0F, 1F, topo);
        f.traco(p, 1F, 0F, 1F, 1F, 1F, lados);
        f.traco(p, 1F, 1F, 0F, 1F, 1F, base);
        f.traco(p, 0F, 1F, 0F, 0F, 1F, lados);
    }

    /** Brilho neon em volta da região [u0, u1]: quatro faixas que vão de "cor" até transparente. */
    private static void brilho(Pincel p, Forma f, float u0, float u1, int cor, float raio, int alfa) {
        int dentro = (Mth.clamp(alfa, 0, 255) << 24) | (cor & 0xFFFFFF);
        int fora = cor & 0xFFFFFF; // alfa 0
        float dv = raio / f.h;
        float du = raio / f.bw;
        f.celula(p, u0, u1, -dv, 0F, fora, fora, dentro, dentro);
        f.celula(p, u0, u1, 1F, 1F + dv, dentro, dentro, fora, fora);
        f.celula(p, u0 - du, u0, 0F, 1F, fora, dentro, dentro, fora);
        f.celula(p, u1, u1 + du, 0F, 1F, dentro, fora, fora, dentro);
    }

    /** O "rastro": sobe junto com a barra, mas quando a barra cai ele desce devagar. */
    private static float rastro(int indice, float alvo) {
        float atual = RASTRO[indice];
        if (alvo >= atual) {
            atual = alvo;
        } else {
            atual += (alvo - atual) * (1F - (float) Math.exp(-msDoQuadro / 400F));
        }
        RASTRO[indice] = atual;
        return atual;
    }

    private static int misturar(int cor, int alvo, float quanto) {
        int r = Math.round(((cor >> 16) & 255) * (1 - quanto) + ((alvo >> 16) & 255) * quanto);
        int gr = Math.round(((cor >> 8) & 255) * (1 - quanto) + ((alvo >> 8) & 255) * quanto);
        int b = Math.round((cor & 255) * (1 - quanto) + (alvo & 255) * quanto);
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    /** Escreve um texto pequeno com um halo escuro em volta (legível sobre qualquer cor de barra). */
    private static void escrever(GuiGraphics g, Font fonte, Texto t) {
        float largura = (t.titulo() ? FonteOP.larguraRotulo(fonte, t.conteudo()) : FonteOP.largura(fonte, t.conteudo()))
                * t.escala();
        float x = switch (t.alinhamento()) {
            case 1 -> t.x() - largura / 2F;
            case 2 -> t.x() - largura;
            default -> t.x();
        };
        g.pose().pushPose();
        g.pose().translate(x, t.y(), 0F);
        g.pose().scale(t.escala(), t.escala(), 1F);
        for (float[] o : HALO) {
            if (t.titulo()) {
                FonteOP.desenharRotulo(g, fonte, t.conteudo(), o[0], o[1], COR_HALO, false);
            } else {
                FonteOP.desenhar(g, fonte, t.conteudo(), o[0], o[1], COR_HALO, false);
            }
        }
        if (t.titulo()) {
            FonteOP.desenharRotulo(g, fonte, t.conteudo(), 0F, 0F, t.cor(), false);
        } else {
            FonteOP.desenhar(g, fonte, t.conteudo(), 0F, 0F, t.cor(), false);
        }
        g.pose().popPose();
    }

    // ---------------------------------------------------------------- geometria

    /**
     * Um paralelogramo (topo deslocado para a direita em {@code s}) com coordenadas (u, v) de 0 a 1:
     * u = posição ao longo da largura, v = de cima (0) para baixo (1). Valores fora de 0..1 saem da forma
     * mantendo a inclinação, o que é útil para o brilho.
     */
    private static final class Forma {
        final float x, y, w, h, s, bw;

        Forma(float x, float y, float w, float h, float s) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.s = s;
            this.bw = w - s;
        }

        float px(float u, float v) {
            return x + s * (1F - v) + bw * u;
        }

        float py(float v) {
            return y + h * v;
        }

        /** Um retângulo no espaço (u, v), com uma cor em cada canto (topo-esq, topo-dir, base-dir, base-esq). */
        void celula(Pincel p, float u0, float u1, float v0, float v1, int cTopoEsq, int cTopoDir, int cBaseDir,
                int cBaseEsq) {
            p.quad(px(u0, v0), py(v0), cTopoEsq, px(u1, v0), py(v0), cTopoDir,
                    px(u1, v1), py(v1), cBaseDir, px(u0, v1), py(v1), cBaseEsq);
        }

        void traco(Pincel p, float u0, float v0, float u1, float v1, float espessura, int cor) {
            p.linha(px(u0, v0), py(v0), px(u1, v1), py(v1), espessura, cor);
        }
    }

    /** Junta todos os quadrados coloridos num lote só e desenha de uma vez no fim. */
    private static final class Pincel {
        private final BufferBuilder buffer;
        private final Matrix4f matriz;

        Pincel(GuiGraphics g) {
            matriz = g.pose().last().pose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableCull();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            buffer = Tesselator.getInstance().getBuilder();
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }

        private void vertice(float x, float y, int argb) {
            buffer.vertex(matriz, x, y, 0F)
                    .color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255)
                    .endVertex();
        }

        void quad(float x1, float y1, int c1, float x2, float y2, int c2, float x3, float y3, int c3,
                float x4, float y4, int c4) {
            vertice(x1, y1, c1);
            vertice(x2, y2, c2);
            vertice(x3, y3, c3);
            vertice(x4, y4, c4);
        }

        /** Retângulo de uma cor só, em coordenadas de tela. */
        void ret(float x0, float y0, float x1, float y1, int argb) {
            quad(x0, y0, argb, x1, y0, argb, x1, y1, argb, x0, y1, argb);
        }

        /** Linha com espessura; estica meia espessura nas pontas para os cantos não ficarem com buraco. */
        void linha(float x1, float y1, float x2, float y2, float espessura, int argb) {
            float dx = x2 - x1;
            float dy = y2 - y1;
            float comprimento = (float) Math.sqrt(dx * dx + dy * dy);
            if (comprimento < 0.001F) {
                return;
            }
            float ux = dx / comprimento;
            float uy = dy / comprimento;
            float ext = espessura / 2F;
            x1 -= ux * ext;
            y1 -= uy * ext;
            x2 += ux * ext;
            y2 += uy * ext;
            float nx = -uy * espessura / 2F;
            float ny = ux * espessura / 2F;
            quad(x1 - nx, y1 - ny, argb, x1 + nx, y1 + ny, argb, x2 + nx, y2 + ny, argb, x2 - nx, y2 - ny, argb);
        }

        void fim() {
            BufferUploader.drawWithShader(buffer.end());
            RenderSystem.enableCull();
        }
    }

    // ================================================================== eventos (só no cliente)

    /** Registra a HUD no Forge (barramento do mod). */
    @Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registro {
        @SubscribeEvent
        public static void aoRegistrar(RegisterGuiOverlaysEvent e) {
            e.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(), "barras", HudOrdem::desenhar);
            // o painel da rolagem (acima da barra de sanidade) vem logo depois das barras
            e.registerAbove(new ResourceLocation(OrdemMod.MOD_ID, "barras"), "rolagem", RolagemHud::desenhar);
            // os avisos embaixo da mira (AvisoOrdem) são registrados aqui também, num lugar só
            try {
                e.registerAbove(VanillaGuiOverlay.CROSSHAIR.id(), "avisos", AvisoOrdem::desenhar);
            } catch (IllegalArgumentException jaRegistrado) {
                System.err.println("[OrdemMod] overlay 'avisos' já estava registrado (há uma cópia duplicada de AvisoOrdem?)");
            }
        }
    }

    /** Tira da tela o que as barras substituem e abre espaço para elas. */
    @Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
    public static final class Eventos {
        @SubscribeEvent
        public static void antes(RenderGuiOverlayEvent.Pre e) {
            if (!ativo()) {
                return;
            }
            ResourceLocation id = e.getOverlay().id();
            if (id.equals(VanillaGuiOverlay.PLAYER_HEALTH.id())) {
                e.setCanceled(true); // corações
                // o que o jogo empilha depois (bolhas de ar) começa acima das barras
                if (Minecraft.getInstance().gui instanceof ForgeGui gui) {
                    gui.leftHeight = ALTURA_PILHA;
                    gui.rightHeight = ALTURA_PILHA;
                }
            } else if (id.equals(VanillaGuiOverlay.HOTBAR.id())) {
                e.setCanceled(true); // hotbar do jogo (a nossa é desenhada junto com as barras)
            } else if (id.equals(VanillaGuiOverlay.ARMOR_LEVEL.id()) || id.equals(VanillaGuiOverlay.FOOD_LEVEL.id())) {
                e.setCanceled(true); // armadura e fome
            } else if (id.equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                    && Minecraft.getInstance().player.jumpableVehicle() == null) {
                e.setCanceled(true); // barra de XP (e o número do nível): agora é desenhada por nós
            }
        }

        /** Por último, depois de todo mundo: sobe o nome do item e a barra de ação para cima das barras. */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void subirTextos(RenderGuiOverlayEvent.Pre e) {
            if (e.isCanceled() || !ativo()) {
                return;
            }
            ResourceLocation id = e.getOverlay().id();
            if (id.equals(VanillaGuiOverlay.ITEM_NAME.id()) || id.equals(VanillaGuiOverlay.RECORD_OVERLAY.id())) {
                e.getGuiGraphics().pose().pushPose();
                e.getGuiGraphics().pose().translate(0F, -DESLOCAMENTO_TEXTOS, 0F);
                textosDeslocados = true;
            }
        }

        @SubscribeEvent
        public static void depois(RenderGuiOverlayEvent.Post e) {
            if (!textosDeslocados) {
                return;
            }
            ResourceLocation id = e.getOverlay().id();
            if (id.equals(VanillaGuiOverlay.ITEM_NAME.id()) || id.equals(VanillaGuiOverlay.RECORD_OVERLAY.id())) {
                e.getGuiGraphics().pose().popPose();
                textosDeslocados = false;
            }
        }

        /** Ao sair do mundo, esquece o que o servidor mandou (a HUD só volta quando chegar de novo). */
        @SubscribeEvent
        public static void aoSair(ClientPlayerNetworkEvent.LoggingOut e) {
            status = null;
            textosDeslocados = false;
        }
    }
}
