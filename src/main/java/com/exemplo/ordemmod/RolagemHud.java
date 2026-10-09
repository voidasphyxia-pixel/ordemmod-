package com.exemplo.ordemmod;

import java.util.List;
import java.util.Random;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * O painel da rolagem, logo acima da barra de sanidade: os números embaralham um instante (como um dado rolando),
 * param no resultado real que o servidor mandou, ficam um tempo na tela e somem com fade.
 * É só estética: o servidor já decidiu tudo. Só existe no cliente.
 */
public final class RolagemHud {
    private RolagemHud() {
    }

    private static final int LARGURA = 182;           // a mesma das barras
    private static final int ALTURA = 26;
    private static final int INCLINACAO = 3;          // o topo escorrega para a direita, como nas barras
    private static final int FOLGA = 5;               // vão entre o painel e a barra de sanidade

    private static final float ROLANDO = 750F;        // ms embaralhando
    private static final float TROCA = 55F;           // ms entre um número embaralhado e outro
    private static final float POP = 220F;            // ms do "tranco" quando o total para
    private static final float PERMANENCIA = 2600F;   // ms parado no resultado
    private static final float FADE_ENTRADA = 150F;
    private static final float FADE_SAIDA = 500F;
    private static final float TRANSPARENCIA = 0.92F;
    private static final float CENTRO_GLIFO = -0.6F;  // mesma ideia da HUD (veja HudOrdem)

    private static final int COR_OSSO = 0xE8E2D4;
    private static final int COR_APAGADO = 0x8A8578;
    private static final int COR_BONUS = 0x9FB4E8;
    private static final int COR_NEUTRA = 0xB8B2A4;
    private static final int COR_CRITICO = 0xE6B422;
    private static final int COR_SUCESSO = 0x4CC27A;
    private static final int COR_FALHA = 0xD6404B;

    private static PacoteRolagemHud atual;
    private static long inicio;

    /** Chamado quando o servidor manda uma rolagem (já na thread do cliente). Uma nova substitui a que estiver na tela. */
    public static void receber(PacoteRolagemHud m) {
        atual = m;
        inicio = Util.getMillis();
    }

    public static void desenhar(ForgeGui gui, GuiGraphics g, float parcial, int largura, int altura) {
        PacoteRolagemHud m = atual;
        if (m == null || !HudOrdem.ativo()) {
            return;
        }
        float t = Util.getMillis() - inicio;
        float duracao = ROLANDO + PERMANENCIA + FADE_SAIDA;
        if (t >= duracao) {
            atual = null;
            return;
        }
        float opacidade = Math.min(t / FADE_ENTRADA, 1F);
        if (t > duracao - FADE_SAIDA) {
            opacidade = Math.min(opacidade, (duracao - t) / FADE_SAIDA);
        }
        int alfa = Math.round(255F * Mth.clamp(opacidade, 0F, 1F) * TRANSPARENCIA);
        if (alfa < 8) { // o jogo trata alfa muito baixo como opaco, então nem desenha
            return;
        }

        Font fonte = Minecraft.getInstance().font;
        boolean rolando = t < ROLANDO;
        int x = largura / 2 - LARGURA / 2;
        int y = HudOrdem.yTopoDasBarras(altura) - FOLGA - ALTURA;
        int corResultado = m.critico() ? COR_CRITICO : m.sucesso() ? COR_SUCESSO : COR_FALHA;
        int corBorda = rolando ? COR_NEUTRA : corResultado;

        painel(g, x, y, alfa, corBorda);

        float centroY = y + ALTURA / 2F;
        float xTexto = x + INCLINACAO / 2F + 6F;
        Pericia pericia = Pericia.values()[Mth.clamp(m.pericia(), 0, Pericia.values().length - 1)];
        String titulo = pericia.getNome().toUpperCase() + (m.motivo().isBlank() ? "" : " · " + m.motivo().toUpperCase())
                + " · DT " + m.dt();
        texto(g, fonte, titulo, xTexto, y + 7.5F, 0.62F, COR_NEUTRA, alfa, 0);

        // dados e bônus
        float cursor = xTexto;
        float yDados = y + 17.5F;
        cursor += texto(g, fonte, "d20", cursor, yDados, 0.62F, COR_APAGADO, alfa, 0) + 4F;
        List<Integer> dados = m.dados();
        int mostrar = Math.min(dados.size(), 6);
        for (int i = 0; i < mostrar; i++) {
            int valor = rolando ? 1 + new Random((long) (t / TROCA) * 31L + i * 7L).nextInt(20) : dados.get(i);
            boolean escolhido = !rolando && valor == m.escolhido() && jaEhOEscolhido(dados, i, m.escolhido());
            int cor = rolando || escolhido ? COR_OSSO : COR_APAGADO;
            cursor += texto(g, fonte, String.valueOf(valor), cursor, yDados, 0.74F, cor, alfa, 0) + 4F;
        }
        if (dados.size() > mostrar) {
            cursor += texto(g, fonte, "…", cursor, yDados, 0.74F, COR_APAGADO, alfa, 0) + 4F;
        }
        if (m.bonus() != 0) {
            String b = (m.bonus() > 0 ? "+" : "") + m.bonus();
            texto(g, fonte, b, cursor, yDados, 0.74F, COR_BONUS, alfa, 0);
        }

        // o que isso causou (só depois que o dado para)
        if (!rolando) {
            String efeito;
            if (m.reducao() >= 100) {
                efeito = "SEM DANO";
            } else if (m.reducao() > 0) {
                efeito = "-" + m.reducao() + "% DANO";
            } else if (m.reducao() == 0) {
                efeito = "SEM REDUÇÃO";
            } else {
                efeito = m.critico() ? "CRÍTICO!" : m.sucesso() ? "SUCESSO" : "FALHA";
            }
            texto(g, fonte, efeito, x + LARGURA - INCLINACAO / 2F - 46F, y + 7.5F, 0.62F, corResultado, alfa, 2);
        }

        // total grande, à direita
        int totalMostrado = rolando ? 1 + new Random((long) (t / TROCA) * 17L + 3L).nextInt(40) : m.total();
        float escala = 1.45F;
        if (!rolando && t - ROLANDO < POP) {
            float k = 1F - (t - ROLANDO) / POP;
            escala += 0.45F * k * k;
        }
        texto(g, fonte, String.valueOf(totalMostrado), x + LARGURA - INCLINACAO / 2F - 22F, centroY, escala,
                rolando ? COR_OSSO : corResultado, alfa, 1);
        g.flush();
    }

    /** Só o primeiro dado com o valor escolhido fica em destaque (se dois saírem iguais, não acender os dois). */
    private static boolean jaEhOEscolhido(List<Integer> dados, int indice, int escolhido) {
        return dados.indexOf(escolhido) == indice;
    }

    /** Painel de vidro escuro inclinado, com borda fina na cor do estado. */
    private static void painel(GuiGraphics g, int x, int y, int alfa, int corBorda) {
        int fundo = (Math.round(alfa * 0.78F) << 24) | 0x0B0D12;
        int borda = (alfa << 24) | corBorda;
        int w = LARGURA - INCLINACAO;
        for (int i = 0; i < ALTURA; i++) {
            int desloc = Math.round(INCLINACAO * (1F - (i + 0.5F) / ALTURA));
            int x0 = x + desloc;
            if (i == 0 || i == ALTURA - 1) {
                g.fill(x0, y + i, x0 + w, y + i + 1, borda);
            } else {
                g.fill(x0, y + i, x0 + w, y + i + 1, fundo);
                g.fill(x0, y + i, x0 + 1, y + i + 1, borda);
                g.fill(x0 + w - 1, y + i, x0 + w, y + i + 1, borda);
            }
        }
    }

    /**
     * Escreve um texto com o CENTRO vertical em {@code centroY}. alinhamento: 0 esquerda, 1 centro, 2 direita.
     * Retorna a largura escrita (em pixels da interface).
     */
    private static float texto(GuiGraphics g, Font fonte, String conteudo, float x, float centroY, float escala, int rgb,
            int alfa, int alinhamento) {
        float w = FonteOP.largura(fonte, conteudo) * escala;
        float px = switch (alinhamento) {
            case 1 -> x - w / 2F;
            case 2 -> x - w;
            default -> x;
        };
        float py = centroY - escala * CENTRO_GLIFO - escala * fonte.lineHeight / 2F;
        g.pose().pushPose();
        g.pose().translate(px, py, 0F);
        g.pose().scale(escala, escala, 1F);
        FonteOP.desenhar(g, fonte, conteudo, 0F, 0F, (alfa << 24) | rgb, false);
        g.pose().popPose();
        return w;
    }

    @Mod.EventBusSubscriber(modid = OrdemMod.MOD_ID, value = Dist.CLIENT)
    public static final class Eventos {
        /** Ao sair do mundo, a rolagem na tela some. */
        @SubscribeEvent
        public static void aoSair(ClientPlayerNetworkEvent.LoggingOut e) {
            atual = null;
        }
    }
}
