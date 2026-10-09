package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.joml.Matrix4f;

import com.exemplo.ordemmod.marcado.VfxMarcado;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A tela de criação de personagem, em 4 etapas:
 * 1) atributos (pentágono)  2) origem  3) classe  4) perícias.
 *
 * Toda a parte visual mora aqui. As REGRAS ficam em {@link CriacaoPersonagem} e os TEXTOS em
 * {@link DescricoesOP}; no fim, a tela só manda as escolhas ao servidor ({@link PacoteCriarPersonagem}).
 *
 * Como funciona: cada botão/chip é uma {@link Zona} (um retângulo com uma ação). A cada clique a
 * lista de zonas é refeita em {@link #reconstruir()}, então o estado vive só nos campos abaixo.
 */
@OnlyIn(Dist.CLIENT)
public class TelaCriacao extends Screen {
    // ------------------------------------------------------------------ cores (ARGB)
    private static final int COR_FUNDO = 0xFF050507;
    private static final int COR_PAINEL = 0xB40A0A10;   // translúcido: a névoa aparece por trás
    private static final int COR_BORDA = 0xFF34343F;
    private static final int COR_ZONA = 0xC8141419;
    private static final int COR_OSSO = 0xFFEDE8DC;
    private static final int COR_APAGADO = 0xFF7C7C86;
    private static final int COR_DOURADO = 0xFFC2B48C;
    private static final int COR_SANGUE = 0xFFB0172B;
    private static final int COR_SANGUE_CLARO = 0xFFE0263E;
    private static final int COR_OK = 0xFF6FBF73;
    private static final int COR_RITUAL = 0xFFD8E4FF;   // o branco-azulado do círculo ritual

    private static final ResourceLocation CIRCULO = new ResourceLocation(OrdemMod.MOD_ID,
            "textures/gui/marcado/circulo_ritual.png");
    private static final ResourceLocation HALO = new ResourceLocation(OrdemMod.MOD_ID, "textures/gui/marcado/halo.png");

    // tipos de zona
    private static final int CHIP = 0;
    private static final int BOTAO = 1;
    private static final int PRINCIPAL = 2;
    private static final int PEQUENO = 3;
    private static final int CARTA = 4;
    private static final int ETAPA = 5;
    private static final int TEXTO = 6;

    private static final String[] NOMES_ETAPAS = {"Atributos", "Origem", "Classe", "Perícias"};

    /** Um retângulo clicável (ou só informativo) da tela. */
    private static final class Zona {
        final int tipo;
        final int x, y, w, h;
        String texto = "";
        String etiqueta = "";
        String[] linhas = new String[0];
        Object dado;          // o que a caixa de descrição mostra quando o mouse passa por aqui
        Runnable acao;        // null = não clica
        boolean ativa = true;
        boolean selecionada;
        boolean travada;      // perícia que já veio da origem
        boolean bloqueado;    // clicável, mas só para avisar que não dá (ex.: atributo no limite da criação)
        int attr = -1;        // índice do atributo a que a zona pertence (-1 = nenhum); é o que treme
        int cor = COR_OSSO;

        Zona(int tipo, int x, int y, int w, int h) {
            this.tipo = tipo;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        boolean contem(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    // ------------------------------------------------------------------ estado da criação
    private int etapa = 0;
    private int alcancada = 0; // maior etapa já liberada
    private final int[] valores = {1, 1, 1, 1, 1}; // na ordem de Atributo.values()
    private Origem origem;
    private ClasseOP classe;
    private final Set<Pericia> escolhidas = EnumSet.noneOf(Pericia.class);
    private Object foco;   // o último item clicado (a descrição fica nele)
    private Object hover;  // o item sob o mouse agora
    private boolean enviado;
    private final long abertaEm = Util.getMillis(); // relógio da névoa e do brilho

    // tremor e aviso de "atributo no limite da criação"
    private static final long DURACAO_TREMOR = 450L;
    private static final long DURACAO_AVISO = 6000L;
    private final long[] inicioTremor = new long[5]; // 0 = sem tremor
    private String aviso;
    private long avisoAte;

    // ------------------------------------------------------------------ layout
    private final List<Zona> zonas = new ArrayList<>();
    private Personagem previa;
    private int margem, topo, base, esqX, esqW, dirX, dirW, dirY, dirH;
    private int pentCx, pentCy, infoX, infoW;
    private float pentRaio;
    private int caixaY, caixaH; // caixa de descrição das etapas 2, 3 e 4
    private int contadorY;

    public TelaCriacao() {
        super(Component.literal("Criação de personagem"));
    }

    @Override
    protected void init() {
        reconstruir();
    }

    /** Solta da memória os quadros de névoa/poeira (a cena do Marcado os deixou carregados para cá). */
    @Override
    public void removed() {
        VfxMarcado.liberarTodos();
        super.removed();
    }

    /** Sem ESC: o personagem precisa ser criado. */
    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    /** Não pausa: o servidor precisa continuar rodando para receber a criação. */
    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ================================================================== montagem das zonas

    private void layout() {
        margem = 8;
        topo = 44;
        base = height - 34;
        esqX = margem;
        esqW = Mth.clamp(width / 4, 104, 150);
        dirX = esqX + esqW + 8;
        dirW = width - margem - dirX;
        dirY = topo;
        dirH = base - topo;
    }

    private Zona nova(int tipo, int x, int y, int w, int h, String texto) {
        Zona z = new Zona(tipo, x, y, w, h);
        z.texto = texto;
        zonas.add(z);
        return z;
    }

    private void reconstruir() {
        zonas.clear();
        layout();
        previa = CriacaoPersonagem.construir(classe != null ? classe : ClasseOP.COMBATENTE, valores, origem);
        montarEtapas();
        switch (etapa) {
            case 0 -> montarAtributos();
            case 1 -> montarOrigens();
            case 2 -> montarClasses();
            default -> montarPericias();
        }
        montarRodape();
    }

    private void irPara(int nova) {
        etapa = nova;
        foco = null;
        aviso = null;
        if (nova == 3) {
            sanearPericias();
        }
        reconstruir();
    }

    private void montarEtapas() {
        int gap = 6;
        int[] larguras = new int[NOMES_ETAPAS.length];
        int total = 0;
        for (int i = 0; i < larguras.length; i++) {
            larguras[i] = FonteOP.larguraTitulo(font, (i + 1) + "  " + NOMES_ETAPAS[i].toUpperCase()) + 16;
            total += larguras[i] + (i > 0 ? gap : 0);
        }
        int x = (width - total) / 2;
        for (int i = 0; i < larguras.length; i++) {
            final int destino = i;
            Zona z = nova(ETAPA, x, 22, larguras[i], 14, (i + 1) + "  " + NOMES_ETAPAS[i].toUpperCase());
            z.selecionada = i == etapa;
            z.ativa = i <= alcancada;
            z.acao = () -> irPara(destino);
            x += larguras[i] + gap;
        }
    }

    // ---------------------------------------------------------------- etapa 1: atributos

    private void montarAtributos() {
        Atributo[] atributos = Atributo.values();
        int pentW = (int) (dirW * 0.58);
        pentCx = dirX + pentW / 2;
        pentCy = dirY + dirH / 2 + 2;
        infoX = dirX + pentW + 8;
        infoW = dirW - pentW - 8;
        double porAltura = (dirH - 88) / 1.81;
        double porLargura = (pentW / 2.0 - 70) / 0.951;
        pentRaio = (float) Mth.clamp(Math.min(porAltura, porLargura), 36, 130);

        for (int i = 0; i < atributos.length; i++) {
            final int indice = i;
            final Atributo a = atributos[i];
            double ang = Math.toRadians(-90 + 72 * i);
            double dx = Math.cos(ang);
            double dy = Math.sin(ang);
            int bw = 56;
            int bh = 26;
            double afastamento = 12 + 0.5 * (Math.abs(dx) * bw + Math.abs(dy) * bh);
            int bx = (int) Math.round(pentCx + dx * (pentRaio + afastamento) - bw / 2.0);
            int by = (int) Math.round(pentCy + dy * (pentRaio + afastamento) - bh / 2.0);

            Zona rotulo = nova(TEXTO, bx, by, bw, 10, nome(a));
            rotulo.dado = a;
            rotulo.attr = indice;
            rotulo.cor = COR_DOURADO;

            Zona menos = nova(PEQUENO, bx, by + 12, 14, 14, "-");
            menos.dado = a;
            menos.attr = indice;
            menos.ativa = podeDiminuir(indice);
            menos.acao = () -> {
                valores[indice]--;
                foco = a;
                aviso = null;
                reconstruir();
            };

            Zona valor = nova(TEXTO, bx + 14, by + 12, bw - 28, 14, String.valueOf(valores[i]));
            valor.dado = a;
            valor.attr = indice;
            valor.cor = COR_SANGUE_CLARO;

            Zona mais = nova(PEQUENO, bx + bw - 14, by + 12, 14, 14, "+");
            mais.dado = a;
            mais.attr = indice;
            if (valores[indice] >= Atributos.MAXIMO_NA_CRIACAO) {
                // no limite da criação: continua clicável, mas o clique só treme e explica o porquê
                mais.bloqueado = true;
                mais.acao = () -> recusarAumento(indice, a);
            } else {
                mais.ativa = podeAumentar(indice);
                mais.acao = () -> {
                    valores[indice]++;
                    foco = a;
                    aviso = null;
                    reconstruir();
                };
            }
        }
    }

    /** Clique no "+" de um atributo que já está no limite da criação: treme o atributo e explica. */
    private void recusarAumento(int indice, Atributo a) {
        long agora = Util.getMillis();
        inicioTremor[indice] = agora;
        foco = a;
        aviso = nome(a) + " já está em " + Atributos.MAXIMO_NA_CRIACAO + ", o limite na criação de personagem. "
                + "O máximo natural é " + Atributos.MAXIMO_NATURAL + ", alcançado depois por aprimoramento de nível. "
                + "Habilidades e rituais podem deixar o atributo maior, mas só temporariamente.";
        avisoAte = agora + DURACAO_AVISO;
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.VILLAGER_NO, 1.0F));
        }
    }

    /** Deslocamento horizontal (em pixels) do tremor do atributo agora; 0 quando não está tremendo. */
    private float tremor(int indice) {
        long inicio = inicioTremor[indice];
        if (inicio == 0L) {
            return 0F;
        }
        long decorrido = Util.getMillis() - inicio;
        if (decorrido >= DURACAO_TREMOR) {
            return 0F;
        }
        float queda = 1F - decorrido / (float) DURACAO_TREMOR; // o tremor vai perdendo força
        return (float) Math.sin(decorrido * 0.09) * 4F * queda;
    }

    private boolean avisoAtivo() {
        return aviso != null && Util.getMillis() < avisoAte;
    }

    private boolean podeAumentar(int i) {
        int[] teste = valores.clone();
        teste[i]++;
        return teste[i] <= Atributos.MAXIMO_NA_CRIACAO && CriacaoPersonagem.pontosRestantes(teste) >= 0;
    }

    private boolean podeDiminuir(int i) {
        if (valores[i] <= 0) {
            return false;
        }
        if (valores[i] == 1) { // 1 -> 0 só para UM atributo
            for (int v : valores) {
                if (v == 0) {
                    return false;
                }
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- etapa 2: origem

    private void montarOrigens() {
        Origem[] todas = Origem.values();
        int gap = 4;
        int colunas = Mth.clamp(dirW / 104, 2, 5);
        int linhas = (todas.length + colunas - 1) / colunas;
        int largura = (dirW - (colunas - 1) * gap) / colunas;
        int altura = Mth.clamp((int) (dirH * 0.52) / linhas - gap, 12, 16);
        for (int k = 0; k < todas.length; k++) {
            final Origem o = todas[k];
            int x = dirX + (k % colunas) * (largura + gap);
            int y = dirY + (k / colunas) * (altura + gap);
            Zona z = nova(CHIP, x, y, largura, altura, o.getNome());
            z.dado = o;
            z.selecionada = o == origem;
            z.acao = () -> {
                origem = o;
                foco = o;
                reconstruir();
            };
        }
        caixaY = dirY + linhas * (altura + gap) + 4;
        caixaH = dirY + dirH - caixaY;
    }

    // ---------------------------------------------------------------- etapa 3: classe

    private void montarClasses() {
        ClasseOP[] todas = ClasseOP.values();
        int gap = 6;
        int largura = (dirW - 2 * gap) / 3;
        int altura = Mth.clamp((int) (dirH * 0.46), 74, 122);
        for (int k = 0; k < todas.length; k++) {
            final ClasseOP c = todas[k];
            Personagem p = CriacaoPersonagem.construir(c, valores, origem);
            Zona z = nova(CARTA, dirX + k * (largura + gap), dirY, largura, altura, DescricoesOP.nomeClasse(c));
            z.etiqueta = DescricoesOP.classePapel(c);
            z.linhas = new String[] {
                    "PV " + p.vidaMaxima() + " (" + coracoes(p.vidaMaxima()) + " corações)",
                    "SAN " + p.sanidadeMaxima(),
                    "PE " + p.esforcoMaximo() + " (+" + c.esforcoRecuperado() + " a cada 30 s)",
                    p.pericias.vagasDeTreino() + " perícias treinadas"
            };
            z.dado = c;
            z.selecionada = c == classe;
            z.acao = () -> {
                if (classe != c) {
                    classe = c;
                    escolhidas.clear(); // as vagas mudam com a classe: escolhe as perícias de novo
                }
                foco = c;
                reconstruir();
            };
        }
        caixaY = dirY + altura + 6;
        caixaH = dirY + dirH - caixaY;
    }

    private static String coracoes(int vida) {
        return vida % 2 == 0 ? String.valueOf(vida / 2) : (vida / 2) + ",5";
    }

    // ---------------------------------------------------------------- etapa 4: perícias

    private int vagasDeTreino() {
        return CriacaoPersonagem.construir(classe != null ? classe : ClasseOP.COMBATENTE, valores, origem)
                .pericias.vagasDeTreino();
    }

    private int vagasRestantes() {
        return vagasDeTreino() - escolhidas.size();
    }

    private boolean daOrigem(Pericia p) {
        return origem != null && origem.getPericias().contains(p);
    }

    /** Tira da seleção o que não vale mais (veio da origem ou passou das vagas). */
    private void sanearPericias() {
        escolhidas.removeIf(this::daOrigem);
        int vagas = vagasDeTreino();
        Iterator<Pericia> it = escolhidas.iterator();
        while (escolhidas.size() > vagas && it.hasNext()) {
            it.next();
            it.remove();
        }
    }

    private void montarPericias() {
        Pericia[] todas = Pericia.values();
        int gap = 4;
        int colunas = Mth.clamp(dirW / 110, 3, 5);
        int linhas = (todas.length + colunas - 1) / colunas;
        int largura = (dirW - (colunas - 1) * gap) / colunas;
        contadorY = dirY;
        int gradeY = dirY + 13;
        int altura = Mth.clamp(((int) (dirH * 0.55) - 13) / linhas - gap, 12, 16);
        for (int k = 0; k < todas.length; k++) {
            final Pericia p = todas[k];
            int x = dirX + (k % colunas) * (largura + gap);
            int y = gradeY + (k / colunas) * (altura + gap);
            Zona z = nova(CHIP, x, y, largura, altura, p.getNome());
            z.dado = p;
            boolean gratis = daOrigem(p);
            z.travada = gratis;
            z.selecionada = gratis || escolhidas.contains(p);
            z.etiqueta = gratis ? "origem" : p.getAtributo().getSigla();
            if (!gratis) {
                z.acao = () -> {
                    if (escolhidas.contains(p)) {
                        escolhidas.remove(p);
                    } else if (vagasRestantes() > 0) {
                        escolhidas.add(p);
                    }
                    foco = p;
                    reconstruir();
                };
                z.ativa = escolhidas.contains(p) || vagasRestantes() > 0;
            }
        }
        caixaY = gradeY + linhas * (altura + gap) + 2;
        caixaH = dirY + dirH - caixaY;
    }

    // ---------------------------------------------------------------- rodapé

    private void montarRodape() {
        int y = height - 26;
        Zona voltar = nova(BOTAO, dirX, y, 70, 18, "Voltar");
        voltar.ativa = etapa > 0;
        voltar.acao = () -> irPara(etapa - 1);

        boolean ultima = etapa == NOMES_ETAPAS.length - 1;
        Zona seguir = nova(PRINCIPAL, width - margem - 96, y, 96, 18, ultima ? "Confirmar" : "Avançar");
        seguir.ativa = dica().isEmpty();
        seguir.acao = () -> {
            if (ultima) {
                confirmar();
            } else {
                alcancada = Math.max(alcancada, etapa + 1);
                irPara(etapa + 1);
            }
        };
    }

    /** O que falta para poder avançar ("" = pode). */
    private String dica() {
        switch (etapa) {
            case 0: {
                int restam = CriacaoPersonagem.pontosRestantes(valores);
                return restam > 0 ? "Distribua todos os pontos (restam " + restam + ")" : "";
            }
            case 1:
                return origem == null ? "Escolha uma origem" : "";
            case 2:
                return classe == null ? "Escolha uma classe" : "";
            default: {
                int restam = vagasRestantes();
                return restam > 0 ? "Escolha mais " + restam + (restam == 1 ? " perícia" : " perícias") : "";
            }
        }
    }

    private void confirmar() {
        if (enviado || classe == null || origem == null) {
            return;
        }
        enviado = true;
        Rede.CANAL.sendToServer(new PacoteCriarPersonagem(classe, valores, origem, escolhidas));
        onClose();
    }

    // ================================================================== cliques

    @Override
    public boolean mouseClicked(double mx, double my, int botao) {
        if (botao == 0) {
            for (Zona z : new ArrayList<>(zonas)) {
                if (z.ativa && z.acao != null && z.contem(mx, my)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    z.acao.run();
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, botao);
    }

    // ================================================================== desenho

    @Override
    public void render(GuiGraphics g, int mx, int my, float parcial) {
        desenharFundo(g);

        hover = null;
        for (Zona z : zonas) {
            if (z.dado != null && z.contem(mx, my)) {
                hover = z.dado;
            }
        }
        Object alvo = hover != null ? hover : foco;

        desenharPainelEsquerdo(g, mx, my);
        switch (etapa) {
            case 0 -> {
                desenharPentagono(g);
                desenharInfoAtributos(g, alvo);
            }
            case 1, 2 -> desenharDetalhe(g, dirX, caixaY, dirW, caixaH, alvo, true);
            default -> {
                String contador = "Perícias treinadas: " + escolhidas.size() + " de " + vagasDeTreino()
                        + "   (as da origem são de graça)";
                FonteOP.desenharEntidade(g, font, contador, dirX, contadorY + 1, vagasRestantes() == 0 ? COR_OK : COR_DOURADO, false);
                desenharDetalhe(g, dirX, caixaY, dirW, caixaH, alvo, true);
            }
        }

        for (Zona z : zonas) {
            desenharZona(g, z, mx, my);
        }

        String dica = dica();
        if (!dica.isEmpty()) {
            FonteOP.desenharEntidade(g, font, dica, dirX + 78, height - 26 + 4, COR_DOURADO, false);
        }
    }

    private float relogio() {
        return (Util.getMillis() - abertaEm) / 1000F;
    }

    private void desenharFundo(GuiGraphics g) {
        g.fill(0, 0, width, height, COR_FUNDO);
        float t = relogio();
        VfxMarcado.NEVOA.desenhar(g, t, 0.30F, 0, 0, width, height);
        VfxMarcado.POEIRA.desenhar(g, t, 0.55F, 0, 0, width, height);
        // vinheta, igual à da cena do Marcado
        g.fillGradient(0, 0, width, height / 3, 0xDD000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xDD000000);

        // título em Cinzel
        g.pose().pushPose();
        g.pose().scale(1.25F, 1.25F, 1.0F);
        FonteOP.desenharTitulo(g, font, "CRIAÇÃO DE PERSONAGEM", margem / 1.25F, 6 / 1.25F, COR_OSSO, true);
        g.pose().popPose();
        String marca = "Ordem Paranormal RPG";
        FonteOP.desenharEntidade(g, font, marca, width - margem - font.width(FonteOP.entidade(marca)), 8, COR_APAGADO, false);

        // divisória com losango no meio
        int meio = width / 2;
        g.fill(margem, 40, meio - 9, 41, 0xFF5A1620);
        g.fill(meio + 9, 40, width - margem, 41, 0xFF5A1620);
        g.flush();
        poligono(g, meio, 40.5F, new float[] {meio, meio + 4.5F, meio, meio - 4.5F},
                new float[] {36F, 40.5F, 45F, 40.5F}, COR_SANGUE_CLARO);
    }

    /** Brilho suave (textura radial branca) somado ao fundo. */
    private void halo(GuiGraphics g, float cx, float cy, int raio, float alfa, float r, float gc, float b) {
        g.flush();
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.setColor(r, gc, b, alfa);
        g.blit(HALO, Math.round(cx) - raio, Math.round(cy) - raio, raio * 2, raio * 2, 0F, 0F, 128, 128, 128, 128);
        g.setColor(1F, 1F, 1F, 1F);
        RenderSystem.defaultBlendFunc();
    }

    private void desenharPainelEsquerdo(GuiGraphics g, int mx, int my) {
        painel(g, esqX, topo, esqW, dirH);
        int centro = esqX + esqW / 2;
        int alturaSkin = Mth.clamp((int) (dirH * 0.46), 70, 150);
        int pes = topo + 8 + alturaSkin;
        // brilho atrás do personagem (a "silhueta com brilho" da cena do Marcado)
        float pulso = 0.85F + 0.15F * (float) Math.sin(relogio() * 1.6);
        halo(g, centro, pes - alturaSkin * 0.5F, (int) (esqW * 0.95F), 0.30F * pulso, 0.75F, 0.85F, 1.0F);
        halo(g, centro, pes + 2, (int) (esqW * 0.55F), 0.18F * pulso, 0.9F, 0.15F, 0.25F);

        int y = pes + 10;
        if (minecraft != null && minecraft.player != null) {
            int escala = Math.max(20, (int) (alturaSkin / 1.95F));
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, centro, pes, escala,
                    (float) (centro - mx), pes - alturaSkin * 0.55F - my, minecraft.player);
            String nome = minecraft.player.getName().getString();
            FonteOP.desenharTitulo(g, font, nome, centro - FonteOP.larguraTitulo(font, nome) / 2F, y, COR_OSSO, false);
        }
        y += 14;

        int px = esqX + 6;
        int pw = esqW - 12;
        int yMax = topo + dirH - 4;
        y = linhaRotulada(g, "Origem", origem != null ? origem.getNome() : "—", px, y, pw, yMax);
        y = linhaRotulada(g, "Classe", classe != null ? DescricoesOP.nomeClasse(classe) : "—", px, y, pw, yMax);
        if (classe != null) {
            y += 3;
            y = linhaRotulada(g, "PV", String.valueOf(previa.vidaMaxima()), px, y, pw, yMax);
            y = linhaRotulada(g, "SAN", String.valueOf(previa.sanidadeMaxima()), px, y, pw, yMax);
            linhaRotulada(g, "PE", String.valueOf(previa.esforcoMaximo()), px, y, pw, yMax);
        }
    }

    private int linhaRotulada(GuiGraphics g, String rotulo, String valor, int x, int y, int largura, int yMax) {
        if (y + 9 > yMax) {
            return y;
        }
        FonteOP.desenharTitulo(g, font, rotulo, x, y, COR_DOURADO, false);
        String v = FonteOP.cortar(font, valor, largura - FonteOP.larguraTitulo(font, rotulo) - 6);
        FonteOP.desenharEntidade(g, font, v, x + largura - font.width(FonteOP.entidade(v)), y, COR_OSSO, false);
        return y + 11;
    }

    // ---------------------------------------------------------------- etapa 1: pentágono

    private double pontoX(int i, double r) {
        return pentCx + r * Math.cos(Math.toRadians(-90 + 72 * i));
    }

    private double pontoY(int i, double r) {
        return pentCy + r * Math.sin(Math.toRadians(-90 + 72 * i));
    }

    private void desenharPentagono(GuiGraphics g) {
        g.flush(); // termina o que já foi pedido antes de desenhar "na mão"
        int n = Atributo.values().length;
        // brilho + círculo ritual (o desenho branco da cena do Marcado) atrás do pentágono
        float respira = 0.8F + 0.2F * (float) Math.sin(relogio() * 1.2);
        halo(g, pentCx, pentCy, (int) (pentRaio * 1.9F), 0.22F * respira, 0.7F, 0.8F, 1.0F);
        int lado = Math.round(pentRaio * 2.74F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(0.85F, 0.9F, 1F, 0.55F * respira);
        g.blit(CIRCULO, pentCx - lado / 2, pentCy - lado / 2, lado, lado, 0F, 0F, 400, 400, 400, 400);
        g.setColor(1F, 1F, 1F, 1F);
        g.flush();
        // anéis dos níveis 1 a 5: o dourado (3) é o limite da criação; o último (5) é o máximo natural
        for (int nivel = 1; nivel <= Atributos.MAXIMO_NATURAL; nivel++) {
            double r = pentRaio * nivel / Atributos.MAXIMO_NATURAL;
            int corAnel = nivel == Atributos.MAXIMO_NA_CRIACAO ? 0xFFA89A6C
                    : nivel == Atributos.MAXIMO_NATURAL ? 0x886A7088 : 0x663A3F52;
            float espessura = nivel == Atributos.MAXIMO_NA_CRIACAO ? 1.4F : 1F;
            for (int i = 0; i < n; i++) {
                linha(g, pontoX(i, r), pontoY(i, r), pontoX(i + 1, r), pontoY(i + 1, r), espessura, corAnel);
            }
        }
        // raios
        for (int i = 0; i < n; i++) {
            linha(g, pentCx, pentCy, pontoX(i, pentRaio), pontoY(i, pentRaio), 1F, 0x553A3F52);
        }
        // área dos valores atuais
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            double r = pentRaio * valores[i] / Atributos.MAXIMO_NATURAL;
            xs[i] = (float) pontoX(i, r);
            ys[i] = (float) pontoY(i, r);
        }
        poligono(g, pentCx, pentCy, xs, ys, 0x66B0172B);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            linha(g, xs[i], ys[i], xs[j], ys[j], 1.6F, COR_SANGUE_CLARO);
        }
        g.flush();
        for (int i = 0; i < n; i++) {
            int px = Math.round(xs[i]);
            int py = Math.round(ys[i]);
            g.fill(px - 2, py - 2, px + 3, py + 3, COR_OSSO);
            halo(g, px, py, 9, 0.5F, 1F, 0.3F, 0.35F);
        }
    }

    private void desenharInfoAtributos(GuiGraphics g, Object alvo) {
        painel(g, infoX, dirY, infoW, dirH);
        int px = infoX + 6;
        int pw = infoW - 12;
        int restam = CriacaoPersonagem.pontosRestantes(valores);
        FonteOP.desenharTitulo(g, font, "PONTOS RESTANTES", px, dirY + 6, COR_APAGADO, false);
        g.pose().pushPose();
        g.pose().scale(2F, 2F, 1F);
        FonteOP.desenharTitulo(g, font, String.valueOf(restam), px / 2F, (dirY + 17) / 2F,
                restam == 0 ? COR_OK : COR_SANGUE_CLARO, true);
        g.pose().popPose();
        int y = dirY + 38;
        String regras = "Todos começam em " + Atributos.VALOR_INICIAL + " e nenhum passa de "
                + Atributos.MAXIMO_NA_CRIACAO + " na criação (anel dourado). Você tem "
                + Atributos.PONTOS_PARA_DISTRIBUIR + " pontos para distribuir. Reduzir um atributo a 0 rende +1 ponto. "
                + "O máximo natural é " + Atributos.MAXIMO_NATURAL + ".";
        y = paragrafo(g, regras, px, y, pw, COR_APAGADO, dirY + dirH - 4) + 2;
        g.fill(px, y, px + pw, y + 1, COR_BORDA);
        if (avisoAtivo()) {
            List<FormattedCharSequence> linhasAviso = font.split(FonteOP.entidade(aviso), pw - 10);
            int alturaAviso = 6 + 12 + linhasAviso.size() * 10 + 2;
            int topoAviso = y + 4;
            caixa(g, px, topoAviso, pw, alturaAviso, 0xFF2A0A10, COR_SANGUE);
            FonteOP.desenharTitulo(g, font, "NÃO DÁ PARA SUBIR MAIS", px + 5, topoAviso + 5, COR_SANGUE_CLARO, true);
            int ay = topoAviso + 17;
            for (FormattedCharSequence linha : linhasAviso) {
                if (ay + 9 > dirY + dirH - 4) {
                    break;
                }
                g.drawString(font, linha, px + 5, ay, COR_OSSO, false);
                ay += 10;
            }
            y = topoAviso + alturaAviso + 2;
        }
        desenharDetalhe(g, infoX, y + 2, infoW, dirY + dirH - (y + 2), alvo, false);
    }

    // ---------------------------------------------------------------- caixa de descrição

    private void desenharDetalhe(GuiGraphics g, int x, int y, int w, int h, Object alvo, boolean comCaixa) {
        if (comCaixa) {
            painel(g, x, y, w, h);
        }
        int px = x + 6;
        int pw = w - 12;
        int py = y + 6;
        int yMax = y + h - 4;

        if (alvo == null) {
            paragrafo(g, dicaGeral(), px, py, pw, COR_APAGADO, yMax);
            return;
        }

        String titulo;
        String sub = null;
        List<String> textos = new ArrayList<>();
        if (alvo instanceof Atributo a) {
            titulo = nome(a) + " (" + a.getSigla() + ")";
            textos.add(DescricoesOP.atributo(a));
            textos.add("Perícias: " + DescricoesOP.periciasDoAtributo(a) + ".");
            textos.add("No jogo: " + DescricoesOP.atributoNoMod(a));
        } else if (alvo instanceof Origem o) {
            titulo = o.getNome();
            sub = "Perícias treinadas de graça: " + o.getPericias().get(0).getNome() + " e "
                    + o.getPericias().get(1).getNome();
            textos.add("Poder — " + o.getPoder() + ": " + o.getDescricao());
        } else if (alvo instanceof ClasseOP c) {
            titulo = DescricoesOP.nomeClasse(c);
            sub = DescricoesOP.classePapel(c);
            textos.add(DescricoesOP.classe(c));
            textos.add("Trilhas (a partir do NEX 10%): " + DescricoesOP.trilhasDaClasse(c) + ".");
        } else if (alvo instanceof Pericia p) {
            titulo = p.getNome();
            sub = "Atributo: " + nome(p.getAtributo()) + (daOrigem(p) ? "  ·  já vem da sua origem" : "");
            textos.add(DescricoesOP.pericia(p));
            List<Beneficio> beneficios = Beneficio.de(p);
            if (beneficios.isEmpty()) {
                textos.add("Ainda sem efeito no jogo.");
            } else {
                for (Beneficio b : beneficios) {
                    textos.add("• " + b.linha());
                }
            }
        } else {
            return;
        }

        FonteOP.desenharTitulo(g, font, titulo, px, py, COR_SANGUE_CLARO, true);
        py += 12;
        if (sub != null) {
            py = paragrafo(g, sub, px, py, pw, COR_DOURADO, yMax) + 3;
        }
        for (String t : textos) {
            py = paragrafo(g, t, px, py, pw, COR_OSSO, yMax) + 3;
        }
    }

    private String dicaGeral() {
        return switch (etapa) {
            case 0 -> "Passe o mouse sobre um atributo para ler sobre ele. Use + e - para distribuir os pontos (máximo " + Atributos.MAXIMO_NA_CRIACAO + " cada na criação).";
            case 1 -> "Passe o mouse sobre uma origem para ler sobre ela. Clique para escolher.";
            case 2 -> "Passe o mouse sobre uma classe para ler sobre ela. Clique para escolher.";
            default -> "Passe o mouse sobre uma perícia para ler sobre ela. Clique para treinar.";
        };
    }

    /** Escreve um texto quebrando em linhas. Devolve o Y logo abaixo do que foi escrito. */
    private int paragrafo(GuiGraphics g, String texto, int x, int y, int largura, int cor, int yMax) {
        for (FormattedCharSequence linha : font.split(FonteOP.entidade(texto), largura)) {
            if (y + 9 > yMax) {
                return yMax + 1;
            }
            g.drawString(font, linha, x, y, cor, false);
            y += 10;
        }
        return y;
    }

    // ---------------------------------------------------------------- zonas

    /** Desenha a zona; se o atributo dela estiver tremendo, desloca tudo para o lado. */
    private void desenharZona(GuiGraphics g, Zona z, int mx, int my) {
        float deslocamento = z.attr >= 0 ? tremor(z.attr) : 0F;
        if (deslocamento == 0F) {
            desenharZonaBase(g, z, mx, my);
            return;
        }
        g.pose().pushPose();
        g.pose().translate(deslocamento, 0F, 0F);
        desenharZonaBase(g, z, mx, my);
        g.pose().popPose();
    }

    private void desenharZonaBase(GuiGraphics g, Zona z, int mx, int my) {
        boolean sobre = z.ativa && z.acao != null && z.contem(mx, my);
        int textoY = z.y + (z.h - 8) / 2;

        if (z.tipo == TEXTO) {
            // rótulo do atributo em Cinzel; o número (cor de sangue) em Cinzel também, com sombra
            FonteOP.desenharTitulo(g, font, z.texto, z.x + (z.w - FonteOP.larguraTitulo(font, z.texto)) / 2F, textoY,
                    z.cor, z.cor == COR_SANGUE_CLARO);
            return;
        }
        if (z.tipo == ETAPA) {
            int cor = z.selecionada ? COR_OSSO : z.ativa ? COR_DOURADO : COR_APAGADO;
            FonteOP.desenharTitulo(g, font, z.texto, z.x + (z.w - FonteOP.larguraTitulo(font, z.texto)) / 2F, textoY, cor, z.selecionada);
            if (z.selecionada) {
                g.fill(z.x, z.y + z.h - 1, z.x + z.w, z.y + z.h, COR_SANGUE_CLARO);
            } else if (sobre) {
                g.fill(z.x, z.y + z.h - 1, z.x + z.w, z.y + z.h, COR_BORDA);
            }
            return;
        }
        if (z.tipo == CARTA) {
            desenharCarta(g, z, sobre);
            return;
        }

        int fundo = COR_ZONA;
        int borda = COR_BORDA;
        int cor = COR_OSSO;
        if (z.travada) {
            fundo = 0xFF2A2415;
            borda = COR_DOURADO;
        } else if (z.selecionada) {
            fundo = sobre ? 0xFF4A1019 : 0xFF3A0D15;
            borda = COR_SANGUE;
        } else if (z.bloqueado) {
            fundo = 0xFF0E0E12;
            cor = COR_APAGADO;
            if (sobre || (z.attr >= 0 && tremor(z.attr) != 0F)) {
                borda = COR_SANGUE;
            }
        } else if (!z.ativa) {
            fundo = 0xFF0E0E12;
            cor = COR_APAGADO;
        } else if (sobre) {
            fundo = 0xFF25252E;
            borda = 0xFF50505C;
        }
        if (z.tipo == PRINCIPAL && z.ativa) {
            fundo = sobre ? 0xFFB01A30 : 0xFF8E1424;
            borda = COR_SANGUE_CLARO;
            cor = 0xFFFFFFFF;
        }
        caixa(g, z.x, z.y, z.w, z.h, fundo, borda);

        if (z.tipo == CHIP) {
            int larguraEtiqueta = z.etiqueta.isEmpty() ? 0 : FonteOP.largura(font, z.etiqueta) + 6;
            String t = FonteOP.cortar(font, z.texto, z.w - 8 - larguraEtiqueta);
            FonteOP.desenhar(g, font, t, z.x + 4, textoY, cor, false);
            if (!z.etiqueta.isEmpty()) {
                FonteOP.desenhar(g, font, z.etiqueta, z.x + z.w - 4 - FonteOP.largura(font, z.etiqueta), textoY,
                        z.travada ? COR_DOURADO : COR_APAGADO, false);
            }
        } else {
            FonteOP.desenharTitulo(g, font, z.texto, z.x + (z.w - FonteOP.larguraTitulo(font, z.texto)) / 2F, textoY, cor, false);
        }
        if (z.tipo == PRINCIPAL && z.ativa) {
            cantos(g, z.x, z.y, z.w, z.h, 0xFFFFD0D6);
        }
    }

    private void desenharCarta(GuiGraphics g, Zona z, boolean sobre) {
        int fundo = z.selecionada ? (sobre ? 0xFF4A1019 : 0xFF3A0D15) : (sobre ? 0xFF25252E : COR_ZONA);
        caixa(g, z.x, z.y, z.w, z.h, fundo, z.selecionada ? COR_SANGUE : sobre ? 0xFF50505C : COR_BORDA);
        if (z.selecionada) {
            cantos(g, z.x, z.y, z.w, z.h, COR_SANGUE_CLARO);
        }
        int px = z.x + 6;
        int y = z.y + 6;
        g.pose().pushPose();
        g.pose().scale(1.25F, 1.25F, 1F);
        FonteOP.desenharTitulo(g, font, z.texto.toUpperCase(), px / 1.25F, y / 1.25F, z.selecionada ? COR_SANGUE_CLARO : COR_OSSO, true);
        g.pose().popPose();
        y += 15;
        FonteOP.desenharEntidade(g, font, z.etiqueta, px, y, COR_DOURADO, false);
        y += 14;
        for (String linha : z.linhas) {
            if (y + 9 > z.y + z.h - 3) {
                break;
            }
            FonteOP.desenharEntidade(g, font, FonteOP.cortar(font, linha, z.w - 12), px, y, COR_OSSO, false);
            y += 11;
        }
    }

    private static void caixa(GuiGraphics g, int x, int y, int w, int h, int fundo, int borda) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fundo);
        g.fill(x, y, x + w, y + 1, borda);
        g.fill(x, y + h - 1, x + w, y + h, borda);
        g.fill(x, y + 1, x + 1, y + h - 1, borda);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, borda);
    }

    /** Painel translúcido com os "cantos de ritual" dourados. */
    private static void painel(GuiGraphics g, int x, int y, int w, int h) {
        caixa(g, x, y, w, h, COR_PAINEL, COR_BORDA);
        cantos(g, x, y, w, h, 0xCCC2B48C);
    }

    /** Pequenos colchetes nos quatro cantos. */
    private static void cantos(GuiGraphics g, int x, int y, int w, int h, int cor) {
        int t = 4;
        g.fill(x - 1, y - 1, x + t, y, cor);
        g.fill(x - 1, y - 1, x, y + t, cor);
        g.fill(x + w - t, y - 1, x + w + 1, y, cor);
        g.fill(x + w, y - 1, x + w + 1, y + t, cor);
        g.fill(x - 1, y + h, x + t, y + h + 1, cor);
        g.fill(x - 1, y + h - t, x, y + h + 1, cor);
        g.fill(x + w - t, y + h, x + w + 1, y + h + 1, cor);
        g.fill(x + w, y + h - t, x + w + 1, y + h + 1, cor);
    }

    private static String nome(Atributo a) {
        return switch (a) {
            case AGILIDADE -> "Agilidade";
            case FORCA -> "Força";
            case INTELECTO -> "Intelecto";
            case PRESENCA -> "Presença";
            case VIGOR -> "Vigor";
        };
    }

    // ---------------------------------------------------------------- desenho "na mão" (linhas e polígonos)

    /** Uma linha reta com espessura. O Minecraft não tem isso pronto no GuiGraphics. */
    private static void linha(GuiGraphics g, double x1, double y1, double x2, double y2, float espessura, int argb) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double comprimento = Math.sqrt(dx * dx + dy * dy);
        if (comprimento < 0.001) {
            return;
        }
        float nx = (float) (-dy / comprimento * espessura / 2);
        float ny = (float) (dx / comprimento * espessura / 2);
        Matrix4f m = g.pose().last().pose();
        preparar();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        vertice(b, m, (float) x1 - nx, (float) y1 - ny, argb);
        vertice(b, m, (float) x1 + nx, (float) y1 + ny, argb);
        vertice(b, m, (float) x2 + nx, (float) y2 + ny, argb);
        vertice(b, m, (float) x2 - nx, (float) y2 - ny, argb);
        BufferUploader.drawWithShader(b.end());
        terminar();
    }

    /** Polígono preenchido, ligando cada ponto ao centro (serve para formas convexas como o pentágono). */
    private static void poligono(GuiGraphics g, float cx, float cy, float[] xs, float[] ys, int argb) {
        Matrix4f m = g.pose().last().pose();
        preparar();
        BufferBuilder b = Tesselator.getInstance().getBuilder();
        b.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        vertice(b, m, cx, cy, argb);
        for (int i = 0; i <= xs.length; i++) {
            vertice(b, m, xs[i % xs.length], ys[i % xs.length], argb);
        }
        BufferUploader.drawWithShader(b.end());
        terminar();
    }

    private static void vertice(BufferBuilder b, Matrix4f m, float x, float y, int argb) {
        b.vertex(m, x, y, 0F)
                .color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >>> 24) & 255)
                .endVertex();
    }

    private static void preparar() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
    }

    private static void terminar() {
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }
}
