package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Util;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A HUD do mod, no mesmo visual dos menus e da cena do Marcado (TelaCriacao, TelaFicha): formas desenhadas "na mão",
 * painéis retos de vidro escuro translúcido com borda fina, cantos de ritual dourados, degradês, brilho neon,
 * rótulos em Cinzel, vinheta escura no pé da tela e o símbolo do medo (ciano) entre as barras.
 * Barras empilhadas em cima da hotbar (de baixo para cima):
 * XP (verde, fina) / NEX (branco, 20 células) / vida (vermelho) + fome (marrom) /
 * PE (amarelo) + armadura (cinza) / sanidade (azul). No meio das duas colunas (PE/DEF e PV/FOME) fica o símbolo do medo.
 * Os corações, a fome, a armadura, a barra de XP e a hotbar do jogo não são mais desenhados (a hotbar é refeita aqui). Só existe no cliente.
 *
 * ---- Fabric 26.2 (etapa 6a) ----
 * No Forge a HUD usava RegisterGuiOverlaysEvent + RenderGuiOverlayEvent.Pre (para cancelar os elementos do jogo) e
 * desenhava as barras com Tesselator/RenderSystem. Aqui:
 *  - os elementos do jogo são trocados por HudElementRegistry.replaceElement (só desenham se a nossa HUD não está ativa);
 *  - as barras são HudElements nossos (attachElementAfter) que desenham com GuiGraphicsExtractor;
 *  - o antigo "Pincel" (um lote de quadrados coloridos por vértice) virou chamadas de fill/fillGradient. Isso exige que
 *    as formas sejam retângulos alinhados aos eixos, o que já era o caso (INCLINACAO era 0). O degradê horizontal
 *    (que o fillGradient não faz) é desenhado em faixas verticais de cor interpolada;
 *  - o brilho aditivo (halo do símbolo do medo) agora usa a mistura normal: ficou mais fraco. Se quiser o aditivo de volta,
 *    é preciso um RenderPipeline próprio.
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
    /** Onde o ar (bolhas) e a vida da montaria ficam no jogo: 49 acima do fim da tela. Eles sobem (ALTURA_PILHA - 49). */
    private static final int ALTURA_AR_NO_JOGO = 49;
    /** O nome do item e a mensagem da barra de ação sobem para não ficar atrás das barras. */
    private static final int DESLOCAMENTO_TEXTOS = 22;

    private static final int ALTURA_XP = 7;
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
    private static final Identifier SIMBOLO_MEDO = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID,
            "textures/gui/hud_simbolo_medo.png");
    private static final Identifier HALO_TEX = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID,
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
    /** Um degradê horizontal é feito de no máximo tantas faixas verticais (mais que isso não muda nada a olho nu). */
    private static final int MAX_FAIXAS = 40;

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

    public static void receber(PacoteStatus novo) {
        status = novo;
    }

    /** Ao sair do mundo, esquece o que o servidor mandou (a HUD só volta quando chegar de novo). */
    public static void limpar() {
        status = null;
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

    // ================================================================== registro (entrypoint do cliente)

    /**
     * Liga a HUD ao Fabric: troca os elementos do jogo que as barras substituem, desloca os que precisam subir e
     * anexa os nossos (barras, painel da rolagem e avisos). Chamar uma vez, de OrdemModClienteFabric.
     */
    public static void registrar() {
        // o que as nossas barras substituem: só some quando a HUD do mod está ativa
        for (Identifier id : new Identifier[] { VanillaHudElements.HEALTH_BAR, VanillaHudElements.ARMOR_BAR,
                VanillaHudElements.FOOD_BAR, VanillaHudElements.HOTBAR, VanillaHudElements.EXPERIENCE_LEVEL }) {
            HudElementRegistry.replaceElement(id, anterior -> (g, delta) -> {
                if (!ativo()) {
                    anterior.extractRenderState(g, delta);
                }
            });
        }
        // a barra de XP (e a de pulo do cavalo, e a de localizador) ocupa o mesmo lugar: só some se NÓS a desenhamos
        HudElementRegistry.replaceElement(VanillaHudElements.INFO_BAR, anterior -> (g, delta) -> {
            if (!(ativo() && desenhaXpProprio())) {
                anterior.extractRenderState(g, delta);
            }
        });

        // o que o jogo desenha por cima das barras sobe para não ficar atrás delas
        deslocarParaCima(VanillaHudElements.HELD_ITEM_TOOLTIP, DESLOCAMENTO_TEXTOS);
        deslocarParaCima(VanillaHudElements.OVERLAY_MESSAGE, DESLOCAMENTO_TEXTOS);
        deslocarParaCima(VanillaHudElements.AIR_BAR, ALTURA_PILHA - ALTURA_AR_NO_JOGO);
        deslocarParaCima(VanillaHudElements.MOUNT_HEALTH, ALTURA_PILHA - ALTURA_AR_NO_JOGO);

        Identifier barras = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "barras");
        Identifier rolagem = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "rolagem");
        Identifier avisos = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "avisos");
        HudElementRegistry.attachElementAfter(VanillaHudElements.INFO_BAR, barras, HudOrdem::desenhar);
        // o painel da rolagem (acima da barra de sanidade) vem logo depois das barras
        HudElementRegistry.attachElementAfter(barras, rolagem, RolagemHud::desenhar);
        // os avisos embaixo da mira
        HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR, avisos, AvisoOrdem::desenhar);
    }

    /** No cavalo o jogo mostra a barra de pulo no lugar da de XP; nós só desenhamos a nossa fora disso. */
    private static boolean desenhaXpProprio() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.jumpableVehicle() == null;
    }

    private static void deslocarParaCima(Identifier id, float pixels) {
        HudElementRegistry.replaceElement(id, anterior -> (g, delta) -> {
            if (!ativo()) {
                anterior.extractRenderState(g, delta);
                return;
            }
            g.pose().pushMatrix();
            g.pose().translate(0F, -pixels);
            anterior.extractRenderState(g, delta);
            g.pose().popMatrix();
        });
    }

    // ================================================================== desenho

    /** Um texto para escrever depois da geometria. alinhamento: 0 esquerda, 1 centro, 2 direita. titulo = Cinzel. */
    private record Texto(String conteudo, float x, float y, float escala, int cor, int alinhamento, boolean titulo) {
    }

    private static final List<Texto> TEXTOS = new ArrayList<>();
    private static final float[][] HALO = {
            {-0.9F, 0F}, {0.9F, 0F}, {0F, -0.9F}, {0F, 0.9F},
            {-0.65F, -0.65F}, {0.65F, -0.65F}, {-0.65F, 0.65F}, {0.65F, 0.65F}};

    public static void desenhar(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!ativo()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Player jogador = mc.player;
        PacoteStatus s = status;
        int largura = mc.getWindow().getGuiScaledWidth();
        int altura = mc.getWindow().getGuiScaledHeight();

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
        if (desenhaXpProprio()) {
            desenharXp(p, x, altura - Y_XP, largura / 2F, jogador);
        }

        // hotbar no mesmo estilo (o jogo não desenha mais a dele)
        desenharHotbar(p, largura, altura, jogador);

        // cantos de ritual dourados em volta de todo o bloco de barras (de SAN até NEX), como nos painéis dos menus
        cantos(p, x, ySanidade, LARGURA, yNex + ALTURA - ySanidade, 0xCCC2B48C, 4F);

        // símbolo do medo no vão entre as colunas (PE/DEF e PV/FOME)
        desenharSimboloMedo(g, x + METADE + VAO_CENTRAL / 2F, yEsforco + (2 * ALTURA + 1) / 2F);
        for (Texto t : TEXTOS) {
            escrever(g, mc.font, t);
        }
        desenharItensHotbar(g, mc, largura, altura, jogador);

        // pentagonos das habilidades ativas (atual + fila) no canto inferior esquerdo
        HabilidadesHud.desenhar(g, mc, altura, s.habilidades(), s.selecionada(), s.pe());
    }

    /** Escurece de leve o pé da tela (transparente em cima, ~40% de preto embaixo), como a vinheta dos menus. */
    private static void vinheta(Pincel p, int largura, int altura) {
        float topo = altura - 100F;
        p.retangulo(0F, topo, largura, altura, 0x00000000, 0x00000000, 0x66000000, 0x66000000);
    }

    /**
     * O símbolo do medo (ciano) no centro do vão entre as colunas: um brilho suave por trás e o desenho por cima, os
     * dois respirando devagar, igual ao halo da silhueta nos menus. O desenho (fino) fica suave porque a textura tem
     * o arquivo hud_simbolo_medo.png.mcmeta com "blur": true. O brilho de trás já não é aditivo (veja a nota no topo).
     */
    private static void desenharSimboloMedo(GuiGraphicsExtractor g, float cx, float cy) {
        float respira = 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 900.0);
        // brilho: textura radial por trás
        int raio = 15;
        g.blit(RenderPipelines.GUI_TEXTURED, HALO_TEX, Math.round(cx) - raio, Math.round(cy) - raio, 0F, 0F,
                raio * 2, raio * 2, 128, 128, 128, 128, argb(0.28F + 0.17F * respira, MEDO_R, MEDO_G, MEDO_B));
        // o desenho
        g.blit(RenderPipelines.GUI_TEXTURED, SIMBOLO_MEDO, Math.round(cx - SIMBOLO_L / 2F),
                Math.round(cy - SIMBOLO_A / 2F), 0F, 0F, SIMBOLO_L, SIMBOLO_A, 22, 34, 22, 34,
                argb(0.80F + 0.20F * respira, MEDO_R, MEDO_G, MEDO_B));
    }

    private static int argb(float a, float r, float gr, float b) {
        return (Mth.clamp(Math.round(a * 255F), 1, 255) << 24) | (Mth.clamp(Math.round(r * 255F), 0, 255) << 16)
                | (Mth.clamp(Math.round(gr * 255F), 0, 255) << 8) | Mth.clamp(Math.round(b * 255F), 0, 255);
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
        int sel = jogador.getInventory().getSelectedSlot();
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
        Forma f = new Forma(x, y, SLOT, SLOT);
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
    private static void desenharItensHotbar(GuiGraphicsExtractor g, Minecraft mc, int largura, int altura,
            Player jogador) {
        float x0 = largura / 2F - 89F;
        int iy = Math.round(altura - Y_SLOT_OFFSET + 1F);
        for (int i = 0; i < 9; i++) {
            ItemStack item = jogador.getInventory().getItem(i);
            if (!item.isEmpty()) {
                int ix = Math.round(x0 + 1F + i * PASSO_SLOT);
                g.item(jogador, item, ix, iy, i);
                g.itemDecorations(mc.font, item, ix, iy);
            }
        }
        ItemStack mao = jogador.getOffhandItem();
        if (!mao.isEmpty()) {
            int ix = Math.round(xMaoSecundaria(largura) + 1F);
            g.item(jogador, mao, ix, iy, 10);
            g.itemDecorations(mc.font, mao, ix, iy);
        }
    }

    /**
     * Uma barra: rótulo à esquerda e "atual/máximo" à direita. O preenchimento é a porcentagem que o atual é do
     * máximo. Quando o valor cai, o pedaço perdido fica um instante em branco translúcido e some devagar.
     */
    private static void barra(Pincel p, int indice, float x, float y, float w, int atual, int maximo, int cor,
            String rotulo, boolean avisaBaixo) {
        Forma f = new Forma(x, y, w, ALTURA);
        float fracao = maximo > 0 ? Mth.clamp(atual / (float) maximo, 0F, 1F) : 0F;
        float rastro = rastro(indice, fracao);
        boolean alerta = avisaBaixo && maximo > 0 && fracao <= LIMITE_ALERTA;
        float pulso = alerta ? 0.5F + 0.5F * (float) Math.sin(Util.getMillis() / 170.0) : 0F;

        float uI = 1.3F / f.w;
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
        TEXTOS.add(new Texto(rotulo, x + 4F,
                ROTULO_CINZEL ? yTextoTituloCentrado(y, ALTURA) : yTextoCentrado(y, ALTURA),
                ROTULO_CINZEL ? ESCALA_ROTULO : ESCALA_TEXTO, misturar(cor, 0xFFFFFFFF, 0.8F), 0, ROTULO_CINZEL));
        TEXTOS.add(new Texto(atual + "/" + maximo, x + w - 4F, yTextoCentrado(y, ALTURA),
                ESCALA_TEXTO, COR_OSSO, 2, false));
    }

    /** Três marcas escuras discretas (25%, 50%, 75%) por cima do preenchimento, como a régua de um instrumento. */
    private static void graduacoes(Pincel p, Forma f, float uI, float vI) {
        float util = 1F - 2F * uI;
        for (int k = 1; k <= 3; k++) {
            float u = uI + util * k / 4F;
            p.coluna(f.px(u), f.py(vI), f.py(1F - vI), 0x30000000);
        }
    }

    /** NEX: 20 células com um vão entre elas. Cada 5% de NEX acende mais uma. */
    private static void desenharNex(Pincel p, float x, float y, PacoteStatus s) {
        Forma f = new Forma(x, y, LARGURA, ALTURA);
        int partes = Mth.clamp(s.nivel(), 0, PARTES_NEX); // nível = NEX / 5 (99% = 20)
        float uI = 1.3F / f.w;
        float vI = 1.3F / f.h;
        float passo = (1F - 2F * uI) / PARTES_NEX;
        float vao = 1.2F / f.w;

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
        TEXTOS.add(new Texto(s.nex() + "%", f.px(0.5F), yTextoCentrado(y, ALTURA),
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
        Forma f = new Forma(x, y, LARGURA, ALTURA_XP);
        float fracao = Mth.clamp(jogador.experienceProgress, 0F, 1F);
        float uI = 1F / f.w;
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
        p.retangulo(x - 1F, y - 1F, x + t, y, cor);
        p.retangulo(x - 1F, y - 1F, x, y + t, cor);
        p.retangulo(x + w - t, y - 1F, x + w + 1F, y, cor);
        p.retangulo(x + w, y - 1F, x + w + 1F, y + t, cor);
        p.retangulo(x - 1F, y + h, x + t, y + h + 1F, cor);
        p.retangulo(x - 1F, y + h - t, x, y + h + 1F, cor);
        p.retangulo(x + w - t, y + h, x + w + 1F, y + h + 1F, cor);
        p.retangulo(x + w, y + h - t, x + w + 1F, y + h + 1F, cor);
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
            p.coluna(f.px(u1) - 0.5F, f.py(vI), f.py(1F - vI), 0xC8FFFFFF);
        }
    }

    /** Borda fina (1 px, por dentro do retângulo): mais clara em cima, mais escura embaixo. Em alerta, pulsa na cor da barra. */
    private static void moldura(Pincel p, Forma f, int cor, float pulso) {
        int topo = misturar(0xFF42424E, cor, 0.9F * pulso);
        int lados = misturar(0xFF2A2A33, cor, 0.9F * pulso);
        int base = misturar(0xFF17171D, cor, 0.9F * pulso);
        float x1 = f.x + f.w;
        float y1 = f.y + f.h;
        p.retangulo(f.x, f.y, x1, f.y + 1F, topo);
        p.retangulo(f.x, y1 - 1F, x1, y1, base);
        p.retangulo(f.x, f.y + 1F, f.x + 1F, y1 - 1F, lados);
        p.retangulo(x1 - 1F, f.y + 1F, x1, y1 - 1F, lados);
    }

    /** Brilho neon em volta da região [u0, u1]: quatro faixas que vão de "cor" até transparente. */
    private static void brilho(Pincel p, Forma f, float u0, float u1, int cor, float raio, int alfa) {
        int dentro = (Mth.clamp(alfa, 0, 255) << 24) | (cor & 0xFFFFFF);
        int fora = cor & 0xFFFFFF; // alfa 0
        float dv = raio / f.h;
        float du = raio / f.w;
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

    /** Mistura dois ARGB canal a canal (alfa inclusive), de 0 (só {@code a}) a 1 (só {@code b}). */
    private static int interpolar(int a, int b, float t) {
        int aa = Math.round((a >>> 24) * (1 - t) + (b >>> 24) * t);
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (aa << 24) | (r << 16) | (gr << 8) | bl;
    }

    /** Escreve um texto pequeno com um halo escuro em volta (legível sobre qualquer cor de barra). */
    private static void escrever(GuiGraphicsExtractor g, Font fonte, Texto t) {
        float largura = (t.titulo() ? FonteOP.larguraRotulo(fonte, t.conteudo()) : FonteOP.largura(fonte, t.conteudo()))
                * t.escala();
        float x = switch (t.alinhamento()) {
            case 1 -> t.x() - largura / 2F;
            case 2 -> t.x() - largura;
            default -> t.x();
        };
        g.pose().pushMatrix();
        g.pose().translate(x, t.y());
        g.pose().scale(t.escala(), t.escala());
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
        g.pose().popMatrix();
    }

    // ---------------------------------------------------------------- geometria

    /**
     * Um retângulo com coordenadas (u, v) de 0 a 1: u = posição ao longo da largura, v = de cima (0) para baixo (1).
     * Valores fora de 0..1 saem da forma, o que é útil para o brilho. (No Forge era um paralelogramo inclinado.)
     */
    private static final class Forma {
        final float x, y, w, h;

        Forma(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        float px(float u) {
            return x + w * u;
        }

        float py(float v) {
            return y + h * v;
        }

        /** Um retângulo no espaço (u, v), com uma cor em cada canto (topo-esq, topo-dir, base-dir, base-esq). */
        void celula(Pincel p, float u0, float u1, float v0, float v1, int cTopoEsq, int cTopoDir, int cBaseDir,
                int cBaseEsq) {
            p.retangulo(px(u0), py(v0), px(u1), py(v1), cTopoEsq, cTopoDir, cBaseDir, cBaseEsq);
        }
    }

    /** Desenha retângulos coloridos (cor por canto) com fill/fillGradient, em coordenadas de tela. */
    private static final class Pincel {
        private final GuiGraphicsExtractor g;

        Pincel(GuiGraphicsExtractor g) {
            this.g = g;
        }

        /** Retângulo de uma cor só. */
        void retangulo(float x0, float y0, float x1, float y1, int argb) {
            retangulo(x0, y0, x1, y1, argb, argb, argb, argb);
        }

        /** Uma coluna de 1 px de largura centrada em {@code xCentro}, de y0 a y1. */
        void coluna(float xCentro, float y0, float y1, int argb) {
            int xi = Math.round(xCentro - 0.5F);
            retangulo(xi, y0, xi + 1F, y1, argb);
        }

        /**
         * Retângulo com uma cor em cada canto (topo-esq, topo-dir, base-dir, base-esq). A variação vertical é um
         * fillGradient; a horizontal vira faixas verticais, cada uma com a cor interpolada no meio dela.
         */
        void retangulo(float fx0, float fy0, float fx1, float fy1, int cTopoEsq, int cTopoDir, int cBaseDir,
                int cBaseEsq) {
            int x0 = Math.round(fx0);
            int y0 = Math.round(fy0);
            int x1 = Math.round(fx1);
            int y1 = Math.round(fy1);
            if (x1 <= x0 || y1 <= y0) {
                return;
            }
            if (cTopoEsq == cTopoDir && cBaseEsq == cBaseDir) {
                faixa(x0, y0, x1, y1, cTopoEsq, cBaseEsq);
                return;
            }
            int largura = x1 - x0;
            int faixas = Math.min(largura, MAX_FAIXAS);
            for (int i = 0; i < faixas; i++) {
                int xa = x0 + Math.round(largura * (float) i / faixas);
                int xb = x0 + Math.round(largura * (float) (i + 1) / faixas);
                if (xb <= xa) {
                    continue;
                }
                float t = (i + 0.5F) / faixas;
                faixa(xa, y0, xb, y1, interpolar(cTopoEsq, cTopoDir, t), interpolar(cBaseEsq, cBaseDir, t));
            }
        }

        private void faixa(int x0, int y0, int x1, int y1, int cima, int baixo) {
            if (cima == baixo) {
                g.fill(x0, y0, x1, y1, cima);
            } else {
                g.fillGradient(x0, y0, x1, y1, cima, baixo);
            }
        }
    }
}
