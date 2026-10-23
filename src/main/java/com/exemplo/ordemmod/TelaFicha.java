package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exemplo.ordemmod.marcado.VfxMarcado;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/**
 * O menu do personagem (tecla K). À esquerda fica sempre o resumo do personagem (skin e números);
 * à direita há quatro abas, como no rascunho:
 *  - ATRIBUTO: o pentágono de atributos e uma caixa com os NEX em que chegam pontos para distribuir;
 *  - PERÍCIA: as perícias separadas em Treinado, Veterano, Expert e Destreinado (e as melhorias de grau);
 *  - PODERES: no topo uma faixa "CLASSE" com os poderes que não são da árvore (Ataque Especial, que o Combatente recebe
 *    sozinho, Transcender e Treinamento em Perícia); embaixo a árvore de habilidades em ramos (Corpo a corpo, À distância,
 *    Defesa...). Só há linha entre poderes que dependem de verdade um do outro (pré-requisito) e entre os escalonamentos
 *    por NEX (I, II, III, IV), que descem em coluna; o jogador escolhe qual fica equipado (cada um gasta um PE diferente);
 *  - TRILHA: uma coluna de ícones para cada trilha da classe; escolher a trilha fecha as outras para sempre.
 *
 * Os dados chegam prontos do servidor ({@link PacoteFicha}); os cliques que mudam o personagem voltam
 * para o servidor em {@link PacoteArvore}, que confere tudo de novo antes de aceitar.
 *
 * Os botões "+" do pentágono gastam os pontos de atributo que chegam nos marcos de NEX 20%, 50%, 80% e 95%
 * ({@link Atributos#MARCOS_NEX}): o clique vai ao servidor em {@link PacoteArvore#subirAtributo}, que confere o saldo
 * e o limite de {@link Atributos#MAXIMO_NATURAL}.
 *
 * Fabric 26.2 (6b): render() virou extractRenderState(GuiGraphicsExtractor, ...); a entrada recebe MouseButtonEvent/KeyEvent;
 * o NBT usa getXxxOr/getListOrEmpty; Rede.CANAL.sendToServer virou ReceptoresCliente.enviar; as formas livres (pentágono,
 * linhas, losango) agora são desenhadas por varredura em {@link DesenhoOP}; o halo deixou de ser aditivo.
 */
public class TelaFicha extends Screen {
    private static final int COR_FUNDO = 0xFF050507;
    private static final int COR_PAINEL = 0xB40A0A10;
    private static final int COR_BORDA = 0xFF34343F;
    private static final int COR_ZONA = 0xC8141419;
    private static final int COR_OSSO = 0xFFEDE8DC;
    private static final int COR_APAGADO = 0xFF7C7C86;
    private static final int COR_DOURADO = 0xFFC2B48C;
    private static final int COR_SANGUE = 0xFFB0172B;
    private static final int COR_SANGUE_CLARO = 0xFFE0263E;
    private static final int COR_OK = 0xFF6FBF73;
    private static final int COR_VETERANO = 0xFF6FA8DC;
    private static final int COR_EXPERT = 0xFFE6C229;
    private static final int COR_PERIGO = 0xFFFF5555;
    private static final int COR_SETA = 0xFF3A3A44;

    /** NEX em que o personagem ganha pontos para distribuir nos atributos. */
    private static final int[] MARCOS_ATRIBUTO = Atributos.MARCOS_NEX;
    /** Níveis em que a trilha libera um poder (NEX 10%, 40%, 65% e 99%). */
    private static final int[] NIVEIS_TRILHA = {2, 8, 13, 20};

    private static final int ALTURA_ABA = 16;
    private static final int ALTURA_DETALHE = 82;
    private static final int ALTURA_INFO_ATRIBUTO = 84;
    private static final int ALTURA_CHIP = 13;
    private static final int ALTURA_TITULO_TRILHA = 24;

    private static final int ABA_ATRIBUTO = 0;
    private static final int ABA_PERICIA = 1;
    private static final int ABA_PODERES = 2;
    private static final int ABA_TRILHA = 3;
    private static final String[] NOMES_ABAS = {"ATRIBUTO", "PERÍCIA", "PODERES", "TRILHA"};
    /** Ordem dos grupos na aba Perícia (graus): Treinado, Veterano, Expert, Destreinado. */
    private static final int[] GRUPOS_PERICIA = {1, 2, 3, 0};
    private static final String[] NOMES_GRUPOS = {"DESTREINADO", "TREINADO", "VETERANO", "EXPERT"};

    /**
     * Ajuste do texto. A fonte Tektur (hud.json) desenha as letras ~2 px acima de onde o Minecraft espera,
     * por isso o texto vazava do topo das caixas. Se algum texto ainda ficar alto ou baixo demais,
     * mexa só nestes dois números (valem para a tela inteira):
     *  - DESLOC_TEXTO: desce as linhas soltas (parágrafos, títulos);
     *  - CENTRO_VISUAL: distância do "y" de desenho até o meio das letras (usado para centralizar em caixas).
     */
    private static final int DESLOC_TEXTO = 2;
    private static final float CENTRO_VISUAL = 2.2F;

    // mesmo visual da tela de criação (névoa, poeira, círculo ritual, halo)
    private static final Identifier CIRCULO = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID,
            "textures/gui/titulo/simbolo_maior.png"); // o mesmo simbolo em HD (1024 px) da tela inicial e da criacao
    private static final Identifier HALO = Identifier.fromNamespaceAndPath(OrdemMod.MOD_ID, "textures/gui/marcado/halo.png");
    private final long abertaEm = Util.getMillis();

    private CompoundTag d;
    private int[] atributos;
    private byte[] graus;
    private byte[] deOrigem;
    private final List<CompoundTag> arvore = new ArrayList<>();
    private final List<CompoundTag> trilhas = new ArrayList<>();
    /**
     * Um poder na árvore de poderes: os poderes que dependem dele ficam embaixo (filhos) e os escalonamentos por NEX
     * descem em coluna (I, II, III, IV), cada um ligado ao anterior.
     */
    private static final class NoArvore {
        final CompoundTag tag;
        final List<NoArvore> filhos = new ArrayList<>();

        NoArvore(CompoundTag tag) {
            this.tag = tag;
        }
    }

    /**
     * Um ramo da árvore (ex.: CORPO A CORPO): um título e as colunas de poderes embaixo dele. Estar no mesmo ramo é só
     * organização visual (não há linha ligando); só os poderes ligados por linha clara dependem um do outro.
     */
    private static final class Ramo {
        final String nome;
        final List<NoArvore> raizes = new ArrayList<>();

        Ramo(String nome) {
            this.nome = nome;
        }
    }

    private final List<Ramo> ramos = new ArrayList<>();

    /** Uma região clicável desenhada neste quadro. {@code naLista}: só vale se o mouse estiver dentro da lista. */
    private record Alvo(int x, int y, int w, int h, String chave, boolean naLista) {
    }

    private final List<Alvo> alvos = new ArrayList<>();
    private String sobre = "";            // chave do alvo sob o mouse
    private Pericia periciaSobMouse;      // perícia sob o mouse (abas Perícia e Poderes)
    private String selecionado = "";      // "h:<id>" (poder), "t:<i>" (trilha) ou "p:<i>:<k>" (poder da trilha)
    private boolean confirmando = false;  // escolher trilha pede um segundo clique (não dá para trocar depois)
    private boolean escolhendo = false;   // Treinamento em Perícia: escolhendo as duas perícias
    private final List<Integer> escolhidas = new ArrayList<>();

    private int aba = ABA_ATRIBUTO;
    private int rolagem = 0;              // rolagem da lista, em pixels
    private int alturaConteudo = 0;       // altura total do que a lista mostra (medida a cada quadro)
    private int atributoSobMouse = -1;
    private boolean maisSobMouse = false;

    // layout (calculado em layout())
    private int margem, topo, base, altura;
    private int esqX, esqW, cx, cw;
    private int pentCx, pentCy, infoY;
    private float pentRaio;
    private int abaY, corpoY, listaY, listaH, rodapeY, detalheY, detalheH;
    private int iconePx, caixaPx, espacoSeta;

    public TelaFicha(CompoundTag dados) {
        super(Component.literal("Ficha do personagem"));
        carregar(dados);
    }

    private void carregar(CompoundTag dados) {
        this.d = dados;
        this.atributos = dados.getIntArray("atributos").filter(a -> a.length == Atributo.values().length)
                .orElseGet(() -> new int[Atributo.values().length]);
        this.graus = dados.getByteArray("graus").filter(a -> a.length == Pericia.values().length)
                .orElseGet(() -> new byte[Pericia.values().length]);
        this.deOrigem = dados.getByteArray("deOrigem").filter(a -> a.length == Pericia.values().length)
                .orElseGet(() -> new byte[Pericia.values().length]);
        lerLista(dados, "arvore", arvore);
        lerLista(dados, "trilhas", trilhas);
        montarArvore();
    }

    private static void lerLista(CompoundTag dados, String chave, List<CompoundTag> destino) {
        destino.clear();
        ListTag lista = dados.getListOrEmpty(chave);
        for (int i = 0; i < lista.size(); i++) {
            destino.add(lista.getCompoundOrEmpty(i));
        }
    }

    /** O servidor mandou a ficha de novo (ex.: depois de aprender algo): atualiza sem trocar de aba nem rolagem. */
    public void atualizar(CompoundTag dados) {
        carregar(dados);
        confirmando = false;
        escolhendo = false;
        escolhidas.clear();
        if (width > 0) {
            layout();
        }
        limitarRolagem();
    }

    @Override
    protected void init() {
        layout();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Solta da memória os quadros de névoa/poeira (recarregam sozinhos se a tela voltar, ex.: depois de um teste). */
    @Override
    public void removed() {
        VfxMarcado.liberarTodos();
        super.removed();
    }

    private void layout() {
        margem = 10;
        topo = 44;
        base = height - 12;
        altura = base - topo;
        esqW = Mth.clamp(width * 22 / 100, 112, 165);
        esqX = margem;
        cx = esqX + esqW + 8;
        cw = width - margem - cx;

        abaY = topo;
        corpoY = topo + ALTURA_ABA + 4;
        detalheH = ALTURA_DETALHE;
        detalheY = base - detalheH;
        rodapeY = detalheY - 14;
        listaY = corpoY;
        listaH = rodapeY - 3 - listaY;

        // ícones grandes (32 px) quando a tela é alta; pequenos (16 px) quando está apertado
        iconePx = altura >= 290 ? 32 : 16;
        caixaPx = iconePx + 6;
        espacoSeta = iconePx == 32 ? 12 : 9;

        int alturaPentagono = base - ALTURA_INFO_ATRIBUTO - 6 - corpoY;
        pentCx = cx + cw / 2;
        pentCy = corpoY + alturaPentagono / 2;
        pentRaio = Math.max(30F, Math.min(cw / 2F - 44F, alturaPentagono / 2F - 28F));
        infoY = base - ALTURA_INFO_ATRIBUTO;
    }

    // ================================================================== dados

    private int inteiro(String chave) {
        return d.getIntOr(chave, 0);
    }

    private static String nomeAtributo(Atributo a) {
        return switch (a) {
            case AGILIDADE -> "Agilidade";
            case FORCA -> "Força";
            case INTELECTO -> "Intelecto";
            case PRESENCA -> "Presença";
            case VIGOR -> "Vigor";
        };
    }

    private static int corGrau(int grau) {
        return switch (grau) {
            case 1 -> COR_OSSO;
            case 2 -> COR_VETERANO;
            case 3 -> COR_EXPERT;
            default -> COR_APAGADO;
        };
    }

    private static String romano(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(n);
        };
    }

    private static int nexVeterano() {
        return HabilidadesDoPersonagem.nexDoNivel(Pericias.NIVEL_VETERANO);
    }

    private static int nexExpert() {
        return HabilidadesDoPersonagem.nexDoNivel(Pericias.NIVEL_EXPERT);
    }

    /** Uma melhoria de grau sobrando permite subir essa perícia agora? (o servidor confere de novo) */
    private boolean podeMelhorar(Pericia p) {
        int grau = graus[p.ordinal()];
        return (grau == 1 && inteiro("melhoriasVeterano") > 0) || (grau == 2 && inteiro("melhoriasExpert") > 0);
    }

    /** O poder Treinamento em Perícia pode subir essa perícia agora? */
    private boolean podeSubirPorPoder(Pericia p) {
        return switch (graus[p.ordinal()]) {
            case 0 -> true;
            case 1 -> inteiro("nivel") >= Pericias.NIVEL_VETERANO;
            case 2 -> inteiro("nivel") >= Pericias.NIVEL_EXPERT;
            default -> false;
        };
    }

    private void montarArvore() {
        ramos.clear();
        // quem exige outro poder (do mesmo ramo) fica embaixo dele (o primeiro pré-requisito é o "pai")
        Map<String, NoArvore> porId = new LinkedHashMap<>();
        for (CompoundTag n : arvore) {
            porId.put(n.getStringOr("id", ""), new NoArvore(n));
        }
        Map<String, Ramo> porNome = new LinkedHashMap<>();
        for (String nome : RamosDePoderes.ORDEM) {
            porNome.put(nome, new Ramo(nome));
        }
        for (NoArvore no : porId.values()) {
            String nomeRamo = no.tag.getStringOr("ramo", RamosDePoderes.GERAL);
            String pai = no.tag.getStringOr("pai", "");
            NoArvore noPai = pai.isEmpty() ? null : porId.get(pai);
            if (noPai != null && noPai != no && nomeRamo.equals(noPai.tag.getStringOr("ramo", RamosDePoderes.GERAL))) {
                noPai.filhos.add(no);
            } else {
                porNome.computeIfAbsent(nomeRamo, Ramo::new).raizes.add(no);
            }
        }
        for (Ramo r : porNome.values()) {
            if (!r.raizes.isEmpty()) {
                ramos.add(r);
            }
        }
    }

    /** O id do poder numa chave "h:id" (base) ou "v:id:i" (escalonamento i); null se não for de poder. */
    private static String idDaChaveDePoder(String chave) {
        if (chave.startsWith("h:")) {
            return chave.substring(2);
        }
        if (chave.startsWith("v:")) {
            int fim = chave.lastIndexOf(':');
            return fim > 2 ? chave.substring(2, fim) : null;
        }
        return null;
    }

    /** Qual escalonamento a chave aponta: 0 para "h:id" (a primeira versão), i para "v:id:i"; -1 se não for de poder. */
    private static int versaoDaChave(String chave) {
        if (chave.startsWith("h:")) {
            return 0;
        }
        if (chave.startsWith("v:")) {
            try {
                return Integer.parseInt(chave.substring(chave.lastIndexOf(':') + 1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    private CompoundTag poderPorChave(String chave) {
        String id = idDaChaveDePoder(chave);
        if (id == null) {
            return null;
        }
        for (CompoundTag n : arvore) {
            if (id.equals(n.getStringOr("id", ""))) {
                return n;
            }
        }
        return null;
    }

    /** Índice da trilha (posição na lista) de uma chave "t:i" ou "p:i:k"; -1 se não for de trilha. */
    private int trilhaDaChave(String chave) {
        try {
            if (chave.startsWith("t:")) {
                return Integer.parseInt(chave.substring(2));
            }
            if (chave.startsWith("p:")) {
                return Integer.parseInt(chave.substring(2, chave.indexOf(':', 2)));
            }
        } catch (NumberFormatException e) {
            return -1;
        }
        return -1;
    }

    /** Posição do poder dentro da trilha numa chave "p:i:k"; -1 se for o título da trilha. */
    private int posicaoDaChave(String chave) {
        if (!chave.startsWith("p:")) {
            return -1;
        }
        try {
            return Integer.parseInt(chave.substring(chave.indexOf(':', 2) + 1));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private CompoundTag trilhaPorChave(String chave) {
        int i = trilhaDaChave(chave);
        return i >= 0 && i < trilhas.size() ? trilhas.get(i) : null;
    }

    private CompoundTag poderDeTrilhaPorChave(String chave) {
        CompoundTag t = trilhaPorChave(chave);
        int k = posicaoDaChave(chave);
        if (t == null || k < 0) {
            return null;
        }
        ListTag hs = t.getListOrEmpty("habilidades");
        return k < hs.size() ? hs.getCompoundOrEmpty(k) : null;
    }

    // ================================================================== ações

    /** O que o botão do rodapé faz com a seleção atual. */
    private record Acao(String rotulo, boolean ativa, boolean perigo) {
    }

    private Acao acaoDoPoder() {
        CompoundTag no = poderPorChave(selecionado);
        if (no == null) {
            return new Acao("SELECIONE", false, false);
        }
        int estado = no.getIntOr("estado", 0);
        int versao = versaoDaChave(selecionado);
        if (versao >= 1 || (versao == 0 && no.getBooleanOr("equipavel", false))) {
            return acaoDeEscalonamento(no, versao);
        }
        if (no.getBooleanOr("repetivel", false)) {
            if (estado == PacoteFicha.DISPONIVEL) {
                return new Acao("ESCOLHER PERÍCIAS (1 PT)", true, false);
            }
            return new Acao(inteiro("pontosHabilidade") <= 0 ? "SEM PONTOS" : motivoCurto(no), false, false);
        }
        if (estado == PacoteFicha.APRENDIDA) {
            return new Acao("APRENDIDO", false, false);
        }
        return estado == PacoteFicha.DISPONIVEL ? new Acao("APRENDER (1 PT)", true, false)
                : new Acao(motivoCurto(no), false, false);
    }

    /** Botão para um ícone de escalonamento: equipar (muda o gasto de PE), já equipado, ou o NEX que falta. */
    private Acao acaoDeEscalonamento(CompoundTag no, int versao) {
        ListTag lista = no.getListOrEmpty("versaoLista");
        CompoundTag v = versao < lista.size() ? lista.getCompoundOrEmpty(versao) : null;
        boolean liberada = versao == 0 ? no.getIntOr("estado", 0) == PacoteFicha.APRENDIDA
                : v != null && v.getBooleanOr("liberada", false);
        if (!liberada) {
            if (versao == 0) {
                return no.getIntOr("estado", 0) == PacoteFicha.DISPONIVEL ? new Acao("APRENDER (1 PT)", true, false)
                        : new Acao(motivoCurto(no), false, false);
            }
            return new Acao("NEX " + (v != null ? v.getIntOr("nex", 0) : 0) + "%", false, false);
        }
        if (!no.getBooleanOr("equipavel", false)) {
            return new Acao("APRENDIDO", false, false);
        }
        if (no.getIntOr("versaoEquipada", -1) == versao) {
            return new Acao("EQUIPADO", false, false);
        }
        return new Acao("EQUIPAR (" + (v != null ? v.getIntOr("custo", 0) : 0) + " PE)", true, false);
    }

    private Acao acaoDaTrilha() {
        CompoundTag t = trilhaPorChave(selecionado);
        if (t == null) {
            return new Acao("SELECIONE", false, false);
        }
        int escolhida = inteiro("trilha");
        if (escolhida >= 0) {
            return new Acao(escolhida == t.getIntOr("indice", 0) ? "ESCOLHIDA" : "FECHADA", false, false);
        }
        if (inteiro("nivel") < Trilha.NIVEL_MINIMO) {
            return new Acao("NEX " + HabilidadesDoPersonagem.nexDoNivel(Trilha.NIVEL_MINIMO) + "%", false, false);
        }
        return confirmando ? new Acao("CONFIRMAR?", true, true) : new Acao("ESCOLHER TRILHA", true, false);
    }

    private void executarAcaoDoPoder() {
        CompoundTag no = poderPorChave(selecionado);
        if (no == null || !acaoDoPoder().ativa()) {
            return;
        }
        int versao = versaoDaChave(selecionado);
        if (versao >= 0 && no.getBooleanOr("equipavel", false)
                && (versao > 0 || no.getIntOr("estado", 0) == PacoteFicha.APRENDIDA)) {
            ReceptoresCliente.enviar(PacoteArvore.equipar(no.getStringOr("id", ""), versao));
        } else if (no.getBooleanOr("repetivel", false)) {
            escolhendo = true; // Treinamento em Perícia: primeiro escolhe as duas perícias
            escolhidas.clear();
            rolagem = 0;
        } else {
            ReceptoresCliente.enviar(PacoteArvore.aprender(no.getStringOr("id", "")));
        }
    }

    private void executarAcaoDaTrilha() {
        CompoundTag t = trilhaPorChave(selecionado);
        if (t == null || !acaoDaTrilha().ativa()) {
            return;
        }
        if (!confirmando) {
            confirmando = true; // trilha não dá para trocar: pede o segundo clique
            return;
        }
        int indice = t.getIntOr("indice", 0);
        if (indice >= 0 && indice < Trilha.values().length) {
            ReceptoresCliente.enviar(PacoteArvore.escolherTrilha(Trilha.values()[indice]));
        }
        confirmando = false;
    }

    private void confirmarTreinamento() {
        if (escolhidas.size() != 2) {
            return;
        }
        Pericia a = Pericia.values()[escolhidas.get(0)];
        Pericia b = Pericia.values()[escolhidas.get(1)];
        ReceptoresCliente.enviar(PacoteArvore.treinamento(a, b));
        escolhendo = false;
        escolhidas.clear();
    }

    /** O motivo do bloqueio em poucas letras, para caber no botão ("NEX 25%", "AGI 2", nome do poder exigido). */
    private static String motivoCurto(CompoundTag t) {
        String motivo = t.getStringOr("motivo", "");
        if (motivo.startsWith("Requer NEX")) {
            return "NEX " + t.getIntOr("nex", 0) + "%";
        }
        if (motivo.startsWith("Sem pontos")) {
            return "SEM PONTOS";
        }
        if (motivo.startsWith("Requer ")) {
            return motivo.substring("Requer ".length());
        }
        return "BLOQUEADO";
    }

    // ================================================================== entrada

    @Override
    public boolean keyPressed(KeyEvent evento) {
        if (TeclasOrdem.MENU.matches(evento)) {
            onClose();
            return true;
        }
        return super.keyPressed(evento);
    }

    private boolean dentro(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private void limitarRolagem() {
        rolagem = Mth.clamp(rolagem, 0, Math.max(0, alturaConteudo - listaH));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (aba != ABA_ATRIBUTO && dentro(mx, my, cx, listaY, cw, listaH)) {
            rolagem -= (int) Math.signum(scrollY) * 16;
            limitarRolagem();
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    private int abaX(int i) {
        return cx + cw * i / NOMES_ABAS.length;
    }

    private int abaW(int i) {
        int largura = abaX(i + 1) - abaX(i);
        return i + 1 < NOMES_ABAS.length ? largura - 2 : largura;
    }

    private int abaEm(double mx) {
        for (int i = NOMES_ABAS.length - 1; i >= 0; i--) {
            if (mx >= abaX(i)) {
                return i;
            }
        }
        return 0;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent evento, boolean duploClique) {
        double mx = evento.x();
        double my = evento.y();
        int botao = evento.button();
        if (botao == 0) {
            if (my >= abaY && my < abaY + ALTURA_ABA && mx >= cx && mx < cx + cw) {
                int nova = abaEm(mx);
                if (nova != aba) {
                    aba = nova;
                    rolagem = 0;
                    alturaConteudo = 0;
                    confirmando = false;
                    escolhendo = false;
                    escolhidas.clear();
                    clique();
                }
                return true;
            }
            if (aba == ABA_ATRIBUTO) {
                int mais = indiceDoMais(mx, my);
                if (mais >= 0) {
                    if (podeSubirAtributo(mais)) {
                        ReceptoresCliente.enviar(PacoteArvore.subirAtributo(Atributo.values()[mais]));
                        clique();
                    }
                    return true;
                }
            }
            for (int i = alvos.size() - 1; i >= 0; i--) {
                Alvo a = alvos.get(i);
                if (!dentro(mx, my, a.x(), a.y(), a.w(), a.h())) {
                    continue;
                }
                if (a.naLista() && !dentro(mx, my, cx, listaY, cw, listaH)) {
                    continue;
                }
                tratarClique(a.chave());
                return true;
            }
        }
        if (botao == 1 && aba == ABA_PERICIA) { // botão direito numa perícia: sobe de grau, se der
            for (int i = alvos.size() - 1; i >= 0; i--) {
                Alvo a = alvos.get(i);
                if (!a.chave().startsWith("pe:") || !dentro(mx, my, a.x(), a.y(), a.w(), a.h())) {
                    continue;
                }
                if (a.naLista() && !dentro(mx, my, cx, listaY, cw, listaH)) {
                    continue;
                }
                Pericia p = Pericia.values()[Integer.parseInt(a.chave().substring(3))];
                if (podeMelhorar(p)) {
                    ReceptoresCliente.enviar(PacoteArvore.melhorarPericia(p));
                    clique();
                }
                return true;
            }
        }
        return super.mouseClicked(evento, duploClique);
    }

    private void tratarClique(String chave) {
        switch (chave) {
            case "btn:poder" -> executarAcaoDoPoder();
            case "btn:trilha" -> executarAcaoDaTrilha();
            case "btn:confirmar" -> confirmarTreinamento();
            case "btn:cancelar" -> {
                escolhendo = false;
                escolhidas.clear();
            }
            default -> {
                if (chave.startsWith("pe:")) { // perícia na aba Perícia: clique esquerdo rola o teste (o direito sobe de grau)
                    Pericia p = Pericia.values()[Integer.parseInt(chave.substring(3))];
                    ReceptoresCliente.enviar(new PacoteRolarPericia(p.ordinal()));
                } else if (chave.startsWith("pp:")) { // perícia na escolha do Treinamento em Perícia
                    Integer indice = Integer.valueOf(chave.substring(3));
                    if (!escolhidas.remove(indice)) {
                        if (escolhidas.size() >= 2) {
                            escolhidas.remove(0);
                        }
                        escolhidas.add(indice);
                    }
                } else {
                    if (!chave.equals(selecionado)) {
                        selecionado = chave;
                        confirmando = false;
                    }
                }
            }
        }
        clique();
    }

    private void clique() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    // ================================================================== texto

    /** Linha solta de texto: {@code y} é o topo da linha (já com o ajuste da fonte). */
    private void texto(GuiGraphicsExtractor g, String s, float x, float y, int cor) {
        FonteOP.desenhar(g, font, s, x, y + DESLOC_TEXTO, cor, false);
    }

    /** Texto centralizado na vertical dentro de uma caixa que começa em {@code topo} e tem {@code altura}. */
    private void textoNaCaixa(GuiGraphicsExtractor g, String s, float x, int topo, int altura, int cor) {
        FonteOP.desenhar(g, font, s, x, yCentrado(topo, altura), cor, false);
    }

    private static int yCentrado(int topo, int altura) {
        return Math.round(topo + altura / 2F - CENTRO_VISUAL);
    }

    /** O texto cortado com "..." para caber na largura (medido com a fonte do mod). */
    private String ajustar(String s, int larguraMax) {
        if (larguraMax <= 0) {
            return "";
        }
        if (FonteOP.largura(font, s) <= larguraMax) {
            return s;
        }
        int ponto = FonteOP.largura(font, "...");
        return FonteOP.cortar(font, s, Math.max(0, larguraMax - ponto)) + "...";
    }

    /** Texto centralizado numa caixa; se for largo demais, encolhe em vez de vazar. */
    private void textoAjustado(GuiGraphicsExtractor g, String s, int x, int w, int topo, int altura, int cor) {
        int tw = FonteOP.largura(font, s);
        float escala = Math.max(0.55F, Math.min(1F, (w - 6F) / Math.max(1, tw)));
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2F, topo + altura / 2F);
        g.pose().scale(escala, escala);
        FonteOP.desenhar(g, font, s, -tw / 2F, -CENTRO_VISUAL, cor, false);
        g.pose().popMatrix();
    }

    /** Daqui até {@link #soltar} nada é desenhado fora do retângulo: o texto nunca mais vaza da caixa. */
    private static void recortar(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + Math.max(0, w), y + Math.max(0, h));
    }

    private static void soltar(GuiGraphicsExtractor g) {
        g.disableScissor();
    }

    // ================================================================== desenho

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float parcial) {
        alvos.clear();
        sobre = "";
        periciaSobMouse = null;
        atributoSobMouse = -1;
        maisSobMouse = false;

        desenharFundo(g);
        desenharEsquerda(g, mx, my);
        desenharAbas(g, mx, my);
        switch (aba) {
            case ABA_ATRIBUTO -> desenharAtributo(g, mx, my);
            case ABA_PERICIA -> desenharPericias(g, mx, my);
            case ABA_PODERES -> desenharPoderes(g, mx, my);
            default -> desenharTrilhas(g, mx, my);
        }
    }

    private float relogio() {
        return (Util.getMillis() - abertaEm) / 1000F;
    }

    /** Fundo igual ao da tela de criação: névoa + poeira, vinheta, título em Cinzel e divisória com losango. */
    private void desenharFundo(GuiGraphicsExtractor g) {
        g.fill(0, 0, width, height, COR_FUNDO);
        float t = relogio();
        VfxMarcado.NEVOA.desenhar(g, t, 0.30F, 0, 0, width, height);
        VfxMarcado.POEIRA.desenhar(g, t, 0.55F, 0, 0, width, height);
        g.fillGradient(0, 0, width, height / 3, 0xDD000000, 0x00000000);
        g.fillGradient(0, height * 2 / 3, width, height, 0x00000000, 0xDD000000);

        g.pose().pushMatrix();
        g.pose().scale(1.25F, 1.25F);
        FonteOP.desenharTitulo(g, font, "FICHA DO PERSONAGEM", margem / 1.25F, 6 / 1.25F, COR_OSSO, true);
        g.pose().popMatrix();
        String dica = "K ou ESC para fechar";
        FonteOP.desenharEntidade(g, font, dica, width - margem - font.width(FonteOP.entidade(dica)), 8, COR_APAGADO, false);

        int meio = width / 2;
        g.fill(margem, 40, meio - 9, 41, 0xFF5A1620);
        g.fill(meio + 9, 40, width - margem, 41, 0xFF5A1620);
        DesenhoOP.poligono(g, new float[] {meio, meio + 4.5F, meio, meio - 4.5F},
                new float[] {36F, 40.5F, 45F, 40.5F}, COR_SANGUE_CLARO);
    }

    /** Brilho suave (textura radial branca) tingido, por trás do personagem e do pentágono. */
    private void halo(GuiGraphicsExtractor g, float cx, float cy, int raio, float alfa, float r, float gc, float b) {
        DesenhoOP.halo(g, HALO, cx, cy, raio, alfa, r, gc, b);
    }

    // ---------------------------------------------------------------- abas

    private void desenharAbas(GuiGraphicsExtractor g, int mx, int my) {
        for (int i = 0; i < NOMES_ABAS.length; i++) {
            desenharAba(g, abaX(i), abaW(i), NOMES_ABAS[i], aba == i, mx, my, abaTemNovidade(i));
        }
    }

    /** Bolinha verde na aba quando há algo para fazer nela (pontos para gastar, trilha para escolher...). */
    private boolean abaTemNovidade(int i) {
        return switch (i) {
            case ABA_ATRIBUTO -> inteiro("pontosAtributo") > 0;
            case ABA_PERICIA -> inteiro("melhoriasVeterano") + inteiro("melhoriasExpert") > 0;
            case ABA_PODERES -> inteiro("pontosHabilidade") > 0;
            case ABA_TRILHA -> inteiro("trilha") < 0 && inteiro("nivel") >= Trilha.NIVEL_MINIMO;
            default -> false;
        };
    }

    private void desenharAba(GuiGraphicsExtractor g, int x, int w, String texto, boolean ativa, int mx, int my,
            boolean novidade) {
        boolean sobreAba = mx >= x && mx < x + w && my >= abaY && my < abaY + ALTURA_ABA;
        // como na tela de criação: só o texto em Cinzel e um sublinhado (sangue na aba ativa, cinza ao passar o mouse)
        int tw = FonteOP.larguraTitulo(font, texto);
        float escala = Math.max(0.6F, Math.min(1F, (w - 14F) / Math.max(1, tw)));
        g.pose().pushMatrix();
        g.pose().translate(x + w / 2F, abaY + ALTURA_ABA / 2F - 1F);
        g.pose().scale(escala, escala);
        FonteOP.desenharTitulo(g, font, texto, -tw / 2F, -4F, ativa ? COR_OSSO : sobreAba ? COR_DOURADO : COR_APAGADO,
                ativa);
        g.pose().popMatrix();
        if (ativa) {
            g.fill(x, abaY + ALTURA_ABA - 1, x + w, abaY + ALTURA_ABA, COR_SANGUE_CLARO);
        } else if (sobreAba) {
            g.fill(x, abaY + ALTURA_ABA - 1, x + w, abaY + ALTURA_ABA, COR_BORDA);
        }
        if (novidade) {
            g.fill(x + w - 7, abaY + 3, x + w - 3, abaY + 7, COR_OK);
        }
    }

    // ---------------------------------------------------------------- lista rolável (abas Perícia, Poderes e Trilha)

    private void iniciarLista(GuiGraphicsExtractor g, int y, int h) {
        listaY = y;
        listaH = h;
        painel(g, cx, y, cw, h);
        limitarRolagem();
        recortar(g, cx + 1, y + 1, cw - 2, h - 2);
    }

    /** Fecha a lista: {@code yFimTela} é o Y (na tela) logo abaixo do último item desenhado. */
    private void terminarLista(GuiGraphicsExtractor g, int yFimTela) {
        soltar(g);
        alturaConteudo = yFimTela + rolagem - listaY + 6;
        limitarRolagem();
        if (alturaConteudo > listaH) {
            int trilhoX = cx + cw - 6;
            g.fill(trilhoX, listaY + 2, trilhoX + 3, listaY + listaH - 2, COR_ZONA);
            float proporcao = listaH / (float) alturaConteudo;
            int tamanho = Math.max(10, Math.round((listaH - 4) * proporcao));
            int maximo = alturaConteudo - listaH;
            int topoBarra = listaY + 2 + Math.round((listaH - 4 - tamanho) * (maximo == 0 ? 0F : rolagem / (float) maximo));
            g.fill(trilhoX, topoBarra, trilhoX + 3, topoBarra + tamanho, COR_DOURADO);
        }
    }

    private boolean visivel(int y, int h) {
        return y + h > listaY + 1 && y < listaY + listaH - 1;
    }

    /** Registra uma região clicável; devolve true se o mouse está sobre ela. */
    private boolean alvo(int mx, int my, int x, int y, int w, int h, String chave, boolean naLista) {
        if (naLista && !visivel(y, h)) {
            return false;
        }
        alvos.add(new Alvo(x, y, w, h, chave, naLista));
        boolean sobreAlvo = dentro(mx, my, x, y, w, h) && (!naLista || dentro(mx, my, cx, listaY, cw, listaH));
        if (sobreAlvo) {
            sobre = chave;
        }
        return sobreAlvo;
    }

    // ---------------------------------------------------------------- aba ATRIBUTO

    private void desenharAtributo(GuiGraphicsExtractor g, int mx, int my) {
        for (int i = 0; i < Atributo.values().length; i++) {
            double lx = pontoX(i, pentRaio + RAIO_ROTULO);
            double ly = pontoY(i, pentRaio + RAIO_ROTULO);
            if (Math.abs(mx - lx) <= 14 && Math.abs(my - ly) <= 16) {
                atributoSobMouse = i;
            }
        }
        int mais = indiceDoMais(mx, my);
        if (mais >= 0) {
            atributoSobMouse = mais;
            maisSobMouse = true;
        }
        desenharPentagono(g);
        desenharInfoAtributo(g);
    }

    /** O índice do atributo cujo botão "+" está sob o mouse, ou -1. */
    private int indiceDoMais(double mx, double my) {
        for (int i = 0; i < Atributo.values().length; i++) {
            double lx = pontoX(i, pentRaio + RAIO_ROTULO);
            double ly = pontoY(i, pentRaio + RAIO_ROTULO);
            if (mx >= lx + 12 && mx < lx + 23 && my >= ly + DESLOC_MAIS && my < ly + DESLOC_MAIS + 11) {
                return i;
            }
        }
        return -1;
    }

    /** Tem ponto para gastar e o atributo ainda não chegou ao máximo natural (o servidor confere de novo). */
    private boolean podeSubirAtributo(int i) {
        return inteiro("pontosAtributo") > 0 && atributos[i] < Atributos.MAXIMO_NATURAL;
    }

    private void desenharInfoAtributo(GuiGraphicsExtractor g) {
        int h = ALTURA_INFO_ATRIBUTO;
        painel(g, cx, infoY, cw, h);
        int x = cx + 6;
        int w = cw - 12;
        int yMax = infoY + h - 4;
        int y = infoY + 6;
        int nex = inteiro("nex");

        recortar(g, cx + 1, infoY + 1, cw - 2, h - 2);
        int pontos = inteiro("pontosAtributo");
        texto(g, ajustar("PONTOS PARA DISTRIBUIR NOS ATRIBUTOS: " + pontos, w), x, y, pontos > 0 ? COR_OK : COR_APAGADO);
        y += 13;
        // os NEX em que chegam pontos: já liberado (verde), o próximo (dourado) e os que faltam (apagado)
        boolean proximoMarcado = false;
        int chipX = x;
        for (int marco : MARCOS_ATRIBUTO) {
            String rotulo = "NEX " + marco + "%";
            int chipW = FonteOP.largura(font, rotulo) + 12;
            boolean liberado = nex >= marco;
            boolean proximo = !liberado && !proximoMarcado;
            proximoMarcado |= proximo;
            int borda = liberado ? COR_OK : proximo ? COR_DOURADO : COR_BORDA;
            g.fill(chipX, y, chipX + chipW, y + ALTURA_CHIP, borda);
            g.fill(chipX + 1, y + 1, chipX + chipW - 1, y + ALTURA_CHIP - 1, COR_ZONA);
            textoNaCaixa(g, rotulo, chipX + 6, y, ALTURA_CHIP, liberado ? COR_OK : proximo ? COR_DOURADO : COR_APAGADO);
            chipX += chipW + 4;
        }
        y += ALTURA_CHIP + 6;

        if (maisSobMouse) {
            Atributo a = Atributo.values()[atributoSobMouse];
            int atual = atributos[atributoSobMouse];
            String aviso;
            int corAviso = COR_OSSO;
            if (atual >= Atributos.MAXIMO_NATURAL) {
                aviso = nomeAtributo(a) + " já está no máximo (" + Atributos.MAXIMO_NATURAL + ").";
                corAviso = COR_APAGADO;
            } else if (pontos <= 0) {
                aviso = "Sem pontos para gastar agora. " + proximoAtributo();
                corAviso = COR_APAGADO;
            } else {
                aviso = "Clique para gastar 1 ponto em " + nomeAtributo(a) + " (" + atual + " → " + (atual + 1) + ").";
            }
            paragrafo(g, aviso, x, y, w, corAviso, yMax);
        } else if (atributoSobMouse >= 0) {
            Atributo a = Atributo.values()[atributoSobMouse];
            texto(g, ajustar(nomeAtributo(a).toUpperCase(java.util.Locale.ROOT) + "  " + atributos[atributoSobMouse], w),
                    x, y, COR_DOURADO);
            y += 12;
            y = paragrafo(g, DescricoesOP.atributo(a), x, y, w, COR_OSSO, yMax);
            paragrafo(g, DescricoesOP.atributoNoMod(a), x, y + 2, w, COR_DOURADO, yMax);
        } else {
            y = paragrafo(g, proximoAtributo(), x, y, w, COR_OSSO, yMax);
            paragrafo(g, "Passe o mouse sobre um atributo para ver o que ele faz.", x, y + 2, w, COR_APAGADO, yMax);
        }
        soltar(g);
    }

    private String proximoAtributo() {
        int nex = inteiro("nex");
        for (int marco : MARCOS_ATRIBUTO) {
            if (marco > nex) {
                return "Próximo aumento de atributo: NEX " + marco + "%.";
            }
        }
        return "Todos os aumentos de atributo já foram liberados.";
    }

    // ---------------------------------------------------------------- aba PERÍCIA

    private void desenharPericias(GuiGraphicsExtractor g, int mx, int my) {
        int melhoriasV = inteiro("melhoriasVeterano");
        int melhoriasE = inteiro("melhoriasExpert");
        int porMarco = Pericias.MELHORIAS_POR_MARCO + atributos[Atributo.INTELECTO.ordinal()];
        String regra = "No NEX " + nexVeterano() + "% e no NEX " + nexExpert() + "%, " + porMarco
                + " perícias (2 + Intelecto) sobem um grau: de Treinado para Veterano e de Veterano para Expert.";
        String disponiveis = melhoriasV + melhoriasE > 0
                ? "Melhorias sobrando: " + melhoriasV + " para Veterano, " + melhoriasE
                        + " para Expert. Clique numa perícia com a seta verde."
                : "";

        // caixa com a regra, no topo (não rola)
        int w = cw - 12;
        int linhas = font.split(FonteOP.c(regra), w).size()
                + (disponiveis.isEmpty() ? 0 : font.split(FonteOP.c(disponiveis), w).size());
        int notaH = 8 + linhas * 11 + (disponiveis.isEmpty() ? 0 : 2);
        painel(g, cx, corpoY, cw, notaH);
        recortar(g, cx + 1, corpoY + 1, cw - 2, notaH - 2);
        int ny = paragrafo(g, regra, cx + 6, corpoY + 4, w, COR_APAGADO, corpoY + notaH - 1);
        if (!disponiveis.isEmpty()) {
            paragrafo(g, disponiveis, cx + 6, ny + 2, w, COR_OK, corpoY + notaH - 1);
        }
        soltar(g);

        // as quatro seções: Treinado, Veterano, Expert e Destreinado
        int ly = corpoY + notaH + 4;
        iniciarLista(g, ly, rodapeY - 3 - ly);
        int y = listaY + 5 - rolagem;
        int xIni = cx + 7;
        int larguraMax = cw - 7 - 12;
        for (int grau : GRUPOS_PERICIA) {
            List<Pericia> membros = new ArrayList<>();
            for (Pericia p : Pericia.values()) {
                if (graus[p.ordinal()] == grau) {
                    membros.add(p);
                }
            }
            String titulo = NOMES_GRUPOS[grau] + "  (" + membros.size() + ")";
            int tituloW = FonteOP.largura(font, titulo) + 16;
            g.fill(xIni, y, xIni + tituloW, y + 14, grau == 0 ? COR_BORDA : corGrau(grau));
            g.fill(xIni + 1, y + 1, xIni + tituloW - 1, y + 13, 0xFF1B1B22);
            textoNaCaixa(g, titulo, xIni + 8, y, 14, grau == 0 ? COR_APAGADO : corGrau(grau));
            y += 14 + 5;
            if (membros.isEmpty()) {
                texto(g, "Nenhuma.", xIni + 4, y, COR_APAGADO);
                y += 12;
            } else {
                y = fluxoDePericias(g, mx, my, membros, xIni, y, larguraMax, "pe:") + 4;
            }
            y += 6;
        }
        terminarLista(g, y);

        // rodapé e detalhes
        int livres = inteiro("vagasLivres");
        textoNaCaixa(g, ajustar("Vagas de treino livres: " + livres + " de " + inteiro("vagasTotal"), cw - 4),
                cx + 2, rodapeY, 11, livres == 0 ? COR_OK : COR_DOURADO);
        desenharDetalhePericia(g);
    }

    /** Distribui as perícias lado a lado (quebrando de linha). Devolve o Y logo abaixo da última linha. */
    private int fluxoDePericias(GuiGraphicsExtractor g, int mx, int my, List<Pericia> lista, int x0, int y0, int larguraMax,
            String prefixo) {
        int x = x0;
        int y = y0;
        for (Pericia p : lista) {
            int w = larguraDoChip(p, prefixo);
            if (x + w > x0 + larguraMax && x > x0) {
                x = x0;
                y += ALTURA_CHIP + 4;
            }
            desenharChip(g, mx, my, p, x, y, w, prefixo);
            x += w + 4;
        }
        return y + ALTURA_CHIP;
    }

    private int larguraDoChip(Pericia p, String prefixo) {
        int w = FonteOP.largura(font, p.getNome()) + 12;
        if (deOrigem[p.ordinal()] != 0) {
            w += 5; // quadradinho dourado: veio da origem
        }
        if (prefixo.equals("pe:") && podeMelhorar(p)) {
            w += 9; // seta verde: dá para subir de grau
        }
        return w;
    }

    private void desenharChip(GuiGraphicsExtractor g, int mx, int my, Pericia p, int x, int y, int w, String prefixo) {
        int grau = graus[p.ordinal()];
        boolean escolhida = prefixo.equals("pp:") && escolhidas.contains(p.ordinal());
        boolean subir = prefixo.equals("pe:") && podeMelhorar(p);
        boolean passou = alvo(mx, my, x, y, w, ALTURA_CHIP, prefixo + p.ordinal(), true);
        if (passou) {
            periciaSobMouse = p;
        }
        int borda = escolhida ? COR_SANGUE_CLARO : subir ? COR_OK : passou ? COR_APAGADO : COR_BORDA;
        g.fill(x, y, x + w, y + ALTURA_CHIP, borda);
        g.fill(x + 1, y + 1, x + w - 1, y + ALTURA_CHIP - 1,
                escolhida ? 0xFF2A0A10 : passou ? 0xFF1F1F27 : COR_ZONA);
        int tx = x + 6;
        if (deOrigem[p.ordinal()] != 0) {
            g.fill(x + 3, y + 5, x + 6, y + 8, COR_DOURADO);
            tx += 5;
        }
        textoNaCaixa(g, p.getNome(), tx, y, ALTURA_CHIP, grau > 0 ? corGrau(grau) : COR_APAGADO);
        if (subir) { // setinha para cima
            int ax = x + w - 8;
            g.fill(ax + 2, y + 3, ax + 3, y + 4, COR_OK);
            g.fill(ax + 1, y + 4, ax + 4, y + 5, COR_OK);
            g.fill(ax, y + 5, ax + 5, y + 6, COR_OK);
            g.fill(ax + 2, y + 6, ax + 3, y + 10, COR_OK);
        }
    }

    private void desenharDetalhePericia(GuiGraphicsExtractor g) {
        painel(g, cx, detalheY, cw, detalheH);
        int x = cx + 6;
        int w = cw - 12;
        int yMax = detalheY + detalheH - 4;
        int y = detalheY + 6;
        recortar(g, cx + 1, detalheY + 1, cw - 2, detalheH - 2);
        if (periciaSobMouse != null) {
            Pericia p = periciaSobMouse;
            int grau = graus[p.ordinal()];
            GrauTreinamento atual = GrauTreinamento.values()[grau];
            texto(g, ajustar(p.getNome().toUpperCase(java.util.Locale.ROOT) + "  (" + p.getAtributo().getSigla()
                    + " · " + atual.getNome() + ")", w), x, y, COR_DOURADO);
            y += 12;
            y = paragrafo(g, DescricoesOP.pericia(p), x, y, w, COR_OSSO, yMax);
            int valor = atributos[p.getAtributo().ordinal()];
            String teste = valor == 0 ? "Teste: 2d20 (o menor)" : "Teste: " + valor + "d20 (o maior)";
            teste += " +" + atual.getBonus();
            if (aba == ABA_PERICIA) {
                teste += "  ·  clique para rolar (DT " + Pericias.DT_MEDIA + ")";
            }
            y = paragrafo(g, teste, x, y + 2, w, COR_APAGADO, yMax);
            if (aba == ABA_PERICIA && podeMelhorar(p)) {
                paragrafo(g, "Botão direito: sobe para " + GrauTreinamento.values()[grau + 1].getNome()
                        + " (gasta 1 melhoria).", x, y + 2, w, COR_OK, yMax);
            } else if (escolhendo && podeSubirPorPoder(p)) {
                paragrafo(g, atual.getNome() + " passa para " + GrauTreinamento.values()[grau + 1].getNome() + ".",
                        x, y + 2, w, COR_OK, yMax);
            }
        } else {
            y = paragrafo(g, "Passe o mouse sobre uma perícia para ver o que ela faz e como é o teste.", x, y, w,
                    COR_APAGADO, yMax);
            paragrafo(g, "O quadradinho dourado marca as perícias que vieram da origem.", x, y + 4, w, COR_APAGADO, yMax);
        }
        soltar(g);
    }

    // ---------------------------------------------------------------- aba PODERES

    private void desenharPoderes(GuiGraphicsExtractor g, int mx, int my) {
        iniciarLista(g, corpoY, rodapeY - 3 - corpoY);
        if (escolhendo) {
            desenharEscolhaDePericias(g, mx, my);
        } else if (ramos.isEmpty()) {
            texto(g, "Nenhum poder para mostrar.", cx + 8, listaY + 6, COR_APAGADO);
            terminarLista(g, listaY + 20);
        } else {
            int xIni = cx + 10;
            int disponivel = cw - 10 - 14;
            // 1) mede cada ramo e separa em faixas (cada faixa é uma linha de ramos lado a lado);
            //    a faixa da CLASSE (Ataque Especial, Transcender, Treinamento em Perícia) fica sempre sozinha no topo
            List<List<Ramo>> faixas = new ArrayList<>();
            List<Ramo> atual = new ArrayList<>();
            int somaAtual = 0;
            for (Ramo r : ramos) {
                int w = larguraDoRamo(r);
                boolean daClasse = r.nome.equals(RamosDePoderes.CLASSE);
                if (!atual.isEmpty() && (daClasse || somaAtual + GAP_RAMOS + w > disponivel)) {
                    faixas.add(atual);
                    atual = new ArrayList<>();
                    somaAtual = 0;
                }
                somaAtual += (atual.isEmpty() ? 0 : GAP_RAMOS) + w;
                atual.add(r);
                if (daClasse) {
                    faixas.add(atual);
                    atual = new ArrayList<>();
                    somaAtual = 0;
                }
            }
            if (!atual.isEmpty()) {
                faixas.add(atual);
            }
            // 2) desenha: os ramos de cada faixa se espalham pela largura toda (sem sobrar vazio nas laterais)
            int y = listaY + 8 - rolagem;
            for (List<Ramo> faixa : faixas) {
                int soma = 0;
                int maior = 0;
                for (Ramo r : faixa) {
                    soma += larguraDoRamo(r);
                    maior = Math.max(maior, alturaDoRamo(r));
                }
                int gap = faixa.size() > 1 ? Mth.clamp((disponivel - soma) / (faixa.size() - 1), GAP_RAMOS, 64) : 0;
                int total = soma + gap * (faixa.size() - 1);
                int x = xIni + Math.max(0, (disponivel - total) / 2);
                for (Ramo r : faixa) {
                    desenharRamo(g, mx, my, r, x, y);
                    x += larguraDoRamo(r) + gap;
                }
                if (faixa.get(0).nome.equals(RamosDePoderes.CLASSE)) { // divisória entre a faixa da classe e a árvore
                    g.fill(xIni, y + maior + 10, xIni + disponivel, y + maior + 11, COR_BORDA);
                }
                y += maior + 22;
            }
            terminarLista(g, y - 22);
        }
        desenharRodapePoderes(g, mx, my);
        desenharDetalhePoder(g);
    }

    private static final int GAP_RAMOS = 22;     // espaço mínimo entre ramos
    private static final int GAP_COLUNAS = 10;   // espaço entre as colunas de um ramo
    private static final int GAP_LINHAS = 14;    // espaço vertical entre poderes empilhados na mesma coluna
    private static final int GAP_PASSO = 10;     // espaço vertical entre escalonamentos (onde passa a linha)
    private static final int ALTURA_TITULO = 20; // título do ramo
    private static final int ALTURA_BARRA = 10;  // espaço entre o título e os poderes

    private int numeroDeColunas(Ramo r) {
        if (r.nome.equals(RamosDePoderes.CLASSE)) { // faixa da classe: cada poder na sua coluna, lado a lado
            return Math.max(1, r.raizes.size());
        }
        return Math.min(3, Math.max(1, (r.raizes.size() + 1) / 2));
    }

    private List<List<NoArvore>> colunasDo(Ramo r) {
        int k = numeroDeColunas(r);
        List<List<NoArvore>> colunas = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            colunas.add(new ArrayList<>());
        }
        // cada poder vai para a coluna mais baixa do momento: o ramo fica equilibrado, sem uma coluna enorme ao lado de outra vazia
        for (NoArvore raiz : r.raizes) {
            List<NoArvore> menor = colunas.get(0);
            for (List<NoArvore> c : colunas) {
                if (alturaDaColuna(c) < alturaDaColuna(menor)) {
                    menor = c;
                }
            }
            menor.add(raiz);
        }
        return colunas;
    }

    private static int versoesDe(NoArvore no) {
        return Math.max(1, no.tag.getListOrEmpty("versaoLista").size());
    }

    /** Largura da árvore que nasce neste poder: o ícone dele ou a soma dos filhos, o que for maior. */
    private int larguraDoNo(NoArvore no) {
        int filhos = 0;
        for (NoArvore f : no.filhos) {
            filhos += larguraDoNo(f);
        }
        if (!no.filhos.isEmpty()) {
            filhos += GAP_COLUNAS * (no.filhos.size() - 1);
        }
        return Math.max(caixaPx, filhos);
    }

    /** Altura da coluna de escalonamentos (I, II, III, IV) de um poder. */
    private int alturaDaCadeia(NoArvore no) {
        int n = versoesDe(no);
        return n * caixaPx + (n - 1) * GAP_PASSO;
    }

    private int alturaDoNo(NoArvore no) {
        int maisFundo = 0;
        for (NoArvore f : no.filhos) {
            maisFundo = Math.max(maisFundo, alturaDoNo(f));
        }
        return alturaDaCadeia(no) + (no.filhos.isEmpty() ? 0 : GAP_PASSO + maisFundo);
    }

    private int larguraDaColuna(List<NoArvore> coluna) {
        int w = caixaPx;
        for (NoArvore n : coluna) {
            w = Math.max(w, larguraDoNo(n));
        }
        return w;
    }

    private int alturaDaColuna(List<NoArvore> coluna) {
        int h = 0;
        for (NoArvore n : coluna) {
            h += alturaDoNo(n) + (h > 0 ? GAP_LINHAS : 0);
        }
        return h;
    }

    private int larguraDoRamo(Ramo r) {
        int w = 0;
        List<List<NoArvore>> colunas = colunasDo(r);
        for (List<NoArvore> c : colunas) {
            w += larguraDaColuna(c);
        }
        w += GAP_COLUNAS * (colunas.size() - 1);
        return Math.max(w, FonteOP.largura(font, r.nome) + 8);
    }

    private int alturaDoRamo(Ramo r) {
        int h = 0;
        for (List<NoArvore> c : colunasDo(r)) {
            h = Math.max(h, alturaDaColuna(c));
        }
        return ALTURA_TITULO + ALTURA_BARRA + h;
    }

    /**
     * Um ramo: o título (com um sublinhado) e, embaixo, as colunas de poderes. Não há tronco nem barra ligando as colunas, nem
     * espinha ligando poderes empilhados: estar no mesmo ramo não quer dizer que um depende do outro. As únicas linhas são as
     * de {@link #desenharNo} (pré-requisito e escalonamento por NEX).
     */
    private void desenharRamo(GuiGraphicsExtractor g, int mx, int my, Ramo r, int xEsq, int y) {
        int w = larguraDoRamo(r);
        int meio = xEsq + w / 2;
        int tw = FonteOP.largura(font, r.nome);
        texto(g, r.nome, meio - tw / 2F, y, COR_DOURADO);
        g.fill(meio - tw / 2 - 2, y + 12, meio + tw / 2 + 2, y + 13, COR_BORDA);

        List<List<NoArvore>> colunas = colunasDo(r);
        int somaCols = GAP_COLUNAS * (colunas.size() - 1);
        for (List<NoArvore> c : colunas) {
            somaCols += larguraDaColuna(c);
        }
        int x = xEsq + (w - somaCols) / 2;
        int yTopo = y + ALTURA_TITULO + ALTURA_BARRA;
        for (List<NoArvore> coluna : colunas) {
            int cw2 = larguraDaColuna(coluna);
            int yy = yTopo;
            for (NoArvore n : coluna) {
                desenharNo(g, mx, my, n, x, yy, cw2);
                yy += alturaDoNo(n) + GAP_LINHAS;
            }
            x += cw2 + GAP_COLUNAS;
        }
    }

    /**
     * Desenha um poder (a coluna dos escalonamentos I, II, III, IV, cada um ligado ao anterior) centralizado na faixa
     * {@code larguraFaixa} e, abaixo, os poderes que dependem dele, ligados por linhas em ângulo reto.
     */
    private void desenharNo(GuiGraphicsExtractor g, int mx, int my, NoArvore no, int xEsq, int y, int larguraFaixa) {
        int w = Math.max(larguraDoNo(no), caixaPx);
        int centro = xEsq + larguraFaixa / 2;
        int bx = centro - caixaPx / 2;
        boolean base = no.tag.getIntOr("estado", 0) == PacoteFicha.APRENDIDA;

        ListTag lista = no.tag.getListOrEmpty("versaoLista");
        int n = versoesDe(no);
        for (int i = 0; i < n; i++) {
            int by = y + i * (caixaPx + GAP_PASSO);
            if (i > 0) { // linha clara entre escalonamentos: dourada se este já foi liberado
                boolean lib = lista.getCompoundOrEmpty(i).getBooleanOr("liberada", false);
                g.fill(centro, by - GAP_PASSO, centro + 1, by, lib ? COR_DOURADO : COR_SETA);
            }
            desenharVersao(g, mx, my, no, i, i < lista.size() ? lista.getCompoundOrEmpty(i) : null, bx, by);
        }

        if (!no.filhos.isEmpty()) {
            int yBase = y + alturaDaCadeia(no);
            int yFilhos = yBase + GAP_PASSO;
            int yMeio = yBase + GAP_PASSO / 2;
            int cor = base ? COR_DOURADO : COR_SETA;
            int total = 0;
            for (NoArvore f : no.filhos) {
                total += larguraDoNo(f);
            }
            total += GAP_COLUNAS * (no.filhos.size() - 1);
            int xf = centro - total / 2;
            int minC = centro;
            int maxC = centro;
            List<Integer> descidas = new ArrayList<>();
            for (NoArvore f : no.filhos) {
                int fw = larguraDoNo(f);
                int fc = xf + fw / 2;
                descidas.add(fc);
                minC = Math.min(minC, fc);
                maxC = Math.max(maxC, fc);
                desenharNo(g, mx, my, f, xf, yFilhos, fw);
                xf += fw + GAP_COLUNAS;
            }
            g.fill(centro, yBase, centro + 1, yMeio + 1, cor);          // desce do poder
            g.fill(minC, yMeio, maxC + 1, yMeio + 1, cor);              // corre na horizontal
            for (int c : descidas) {
                g.fill(c, yMeio, c + 1, yFilhos, cor);                  // desce até cada filho
            }
        }
    }

    /**
     * Um ícone de poder / de escalonamento. Moldura dourada = liberado, verde = dá para aprender, cinza = bloqueado;
     * o selo no canto diz qual escalonamento é (II, III, IV) e o quadradinho verde marca o que está equipado.
     */
    private void desenharVersao(GuiGraphicsExtractor g, int mx, int my, NoArvore no, int i, CompoundTag versao, int x, int y) {
        String idRaiz = no.tag.getStringOr("id", "");
        String chave = i == 0 ? "h:" + idRaiz : "v:" + idRaiz + ":" + i;
        int estadoBase = no.tag.getIntOr("estado", 0);
        boolean liberada = i == 0 ? estadoBase == PacoteFicha.APRENDIDA
                : versao != null && versao.getBooleanOr("liberada", false);
        boolean disponivel = i == 0 && estadoBase == PacoteFicha.DISPONIVEL;
        boolean equipavel = no.tag.getBooleanOr("equipavel", false);
        boolean equipada = liberada && no.tag.getIntOr("versaoEquipada", -1) == i;
        boolean selecionada = chave.equals(selecionado);
        boolean passou = alvo(mx, my, x, y, caixaPx, caixaPx, chave, true);
        desenharMoldura(g, x, y, selecionada, passou, liberada ? COR_DOURADO : disponivel ? COR_OK : COR_BORDA);
        String idIcone = versao != null ? versao.getStringOr("id", idRaiz) : idRaiz;
        IconesOP.desenharOuReserva(g, idIcone, idRaiz, x + (caixaPx - iconePx) / 2, y + (caixaPx - iconePx) / 2, iconePx,
                liberada || disponivel ? 1F : 0.35F);

        String selo = null;
        if (no.tag.getBooleanOr("repetivel", false)) {
            if (i == 0 && no.tag.getIntOr("vezes", 0) >= 1) {
                selo = "x" + no.tag.getIntOr("vezes", 0);
            }
        } else if (i > 0) {
            selo = romano(i + 1); // II, III, IV: o escalonamento deste ícone
        }
        if (selo != null) {
            int sw = FonteOP.largura(font, selo) + 4;
            g.fill(x + caixaPx - 1 - sw, y + caixaPx - 11, x + caixaPx - 1, y + caixaPx - 1, 0xFF000000);
            textoNaCaixa(g, selo, x + caixaPx - 1 - sw + 2, y + caixaPx - 11, 10, COR_DOURADO);
        }
        if (equipavel && equipada) { // marca o escalonamento equipado
            g.fill(x + 2, y + 2, x + 8, y + 8, 0xFF000000);
            g.fill(x + 3, y + 3, x + 7, y + 7, COR_OK);
        }
    }

    private void desenharMoldura(GuiGraphicsExtractor g, int x, int y, boolean selecionada, boolean passou, int borda) {
        if (selecionada) {
            g.fill(x - 2, y - 2, x + caixaPx + 2, y + caixaPx + 2, COR_SANGUE_CLARO);
        }
        g.fill(x, y, x + caixaPx, y + caixaPx, borda);
        g.fill(x + 1, y + 1, x + caixaPx - 1, y + caixaPx - 1, passou ? 0xFF1F1F27 : COR_ZONA);
    }

    /** Seta para baixo, ligando um poder ao que depende dele. */
    private static void desenharSeta(GuiGraphicsExtractor g, int meio, int y, int alt, int cor) {
        g.fill(meio, y + 1, meio + 1, y + alt - 3, cor);
        for (int i = 0; i < 3; i++) {
            g.fill(meio - 2 + i, y + alt - 4 + i, meio + 3 - i, y + alt - 3 + i, cor);
        }
    }

    /** Treinamento em Perícia: a lista das perícias que podem subir de grau; o jogador marca duas. */
    private void desenharEscolhaDePericias(GuiGraphicsExtractor g, int mx, int my) {
        int x = cx + 8;
        int w = cw - 8 - 14;
        int y = listaY + 6 - rolagem;
        texto(g, "ESCOLHA DUAS PERÍCIAS  (" + escolhidas.size() + "/2)", x, y, COR_DOURADO);
        y += 14;
        y = paragrafo(g, "Cada uma sobe um grau: Destreinado vira Treinado; a partir do NEX " + nexVeterano()
                + "%, Treinado vira Veterano; a partir do NEX " + nexExpert() + "%, Veterano vira Expert.",
                x, y, w, COR_APAGADO, Integer.MAX_VALUE / 2);
        y += 6;
        List<Pericia> elegiveis = new ArrayList<>();
        for (Pericia p : Pericia.values()) {
            if (podeSubirPorPoder(p)) {
                elegiveis.add(p);
            }
        }
        if (elegiveis.isEmpty()) {
            texto(g, "Nenhuma perícia pode subir de grau agora.", x, y, COR_APAGADO);
            y += 12;
        } else {
            y = fluxoDePericias(g, mx, my, elegiveis, x, y, w, "pp:");
        }
        terminarLista(g, y);
    }

    private void desenharRodapePoderes(GuiGraphicsExtractor g, int mx, int my) {
        int pontos = inteiro("pontosHabilidade");
        if (escolhendo) {
            int xConfirmar = botaoRodape(g, mx, my, "CONFIRMAR", cx + cw, escolhidas.size() == 2, false, "btn:confirmar");
            botaoRodape(g, mx, my, "CANCELAR", xConfirmar - 4, true, false, "btn:cancelar");
            textoNaCaixa(g, ajustar("Gasta 1 ponto de habilidade", Math.max(10, xConfirmar - cx - 80)), cx + 2,
                    rodapeY, 11, COR_DOURADO);
            return;
        }
        Acao acao = acaoDoPoder();
        int xBotao = botaoRodape(g, mx, my, acao.rotulo(), cx + cw, acao.ativa(), acao.perigo(), "btn:poder");
        textoNaCaixa(g, ajustar("Pontos de habilidade: " + pontos, xBotao - cx - 8), cx + 2, rodapeY, 11,
                pontos > 0 ? COR_OK : COR_DOURADO);
    }

    /** Botão de 11 px de altura encostado em {@code xDireita}; devolve o X da borda esquerda dele. */
    private int botaoRodape(GuiGraphicsExtractor g, int mx, int my, String rotulo, int xDireita, boolean ativo, boolean perigo,
            String chave) {
        int w = Math.min(cw - 20, FonteOP.largura(font, rotulo) + 10);
        int x = xDireita - w;
        boolean passou = dentro(mx, my, x, rodapeY, w, 11);
        desenharBotao(g, rotulo, x, rodapeY, w, passou && ativo, ativo, perigo);
        if (ativo) {
            alvos.add(new Alvo(x, rodapeY, w, 11, chave, false));
        }
        return x;
    }

    private void desenharDetalhePoder(GuiGraphicsExtractor g) {
        painel(g, cx, detalheY, cw, detalheH);
        int x = cx + 6;
        int w = cw - 12;
        int yMax = detalheY + detalheH - 4;
        int y = detalheY + 6;
        recortar(g, cx + 1, detalheY + 1, cw - 2, detalheH - 2);
        if (escolhendo) {
            if (periciaSobMouse != null) {
                soltar(g);
                desenharDetalhePericia(g);
                return;
            }
            paragrafo(g, "Marque duas perícias diferentes e clique em CONFIRMAR. Passe o mouse para ver cada uma.",
                    x, y, w, COR_APAGADO, yMax);
            soltar(g);
            return;
        }
        String chaveDetalhe = idDaChaveDePoder(sobre) != null ? sobre : selecionado;
        CompoundTag no = poderPorChave(chaveDetalhe);
        if (no == null) {
            y = paragrafo(g, "Clique num poder para ler a descrição e aprender. No topo ficam os poderes da classe (o Ataque Especial "
                    + "você já recebe); embaixo, a árvore em ramos. Só há linha entre poderes que dependem um do outro. "
                    + "Poderes que escalonam por NEX têm um ícone para cada escalonamento: escolha qual "
                    + "fica equipado (cada um gasta um PE diferente).", x, y, w, COR_APAGADO, yMax);
            paragrafo(g, "Moldura dourada: aprendido. Verde: dá para aprender. Cinza: bloqueado. Quadrado verde: equipado.",
                    x, y + 4, w, COR_APAGADO, yMax);
            soltar(g);
            return;
        }
        int versaoDetalhe = versaoDaChave(chaveDetalhe);
        ListTag listaVersoes = no.getListOrEmpty("versaoLista");
        CompoundTag dv = versaoDetalhe >= 0 && versaoDetalhe < listaVersoes.size()
                ? listaVersoes.getCompoundOrEmpty(versaoDetalhe) : null;
        boolean concedido = RamosDePoderes.CLASSE.equals(no.getStringOr("ramo", ""))
                && !no.getBooleanOr("repetivel", false) && no.getIntOr("estado", 0) == PacoteFicha.APRENDIDA
                && CatalogoHabilidades.ID_ATAQUE_ESPECIAL.equals(no.getStringOr("id", ""));
        if (dv != null && listaVersoes.size() > 1) { // ícone de escalonamento: mostra os dados dele
            boolean liberadaV = versaoDetalhe == 0 ? no.getIntOr("estado", 0) == PacoteFicha.APRENDIDA
                    : dv.getBooleanOr("liberada", false);
            String tipoV = dv.getBooleanOr("ativa", false) ? "ATIVA · " + dv.getIntOr("custo", 0) + " PE" : "PASSIVA";
            texto(g, ajustar(dv.getStringOr("nome", "").toUpperCase(java.util.Locale.ROOT) + "  (" + tipoV + ")", w), x, y,
                    COR_DOURADO);
            y += 12;
            y = paragrafo(g, dv.getStringOr("descricao", ""), x, y, w, COR_OSSO, yMax);
            String sit;
            int corV;
            if (!liberadaV && versaoDetalhe > 0) {
                sit = "Libera sozinho no NEX " + dv.getIntOr("nex", 0) + "%.";
                corV = COR_SANGUE_CLARO;
            } else if (!liberadaV) {
                sit = no.getIntOr("estado", 0) == PacoteFicha.DISPONIVEL ? "Pronto para aprender: custa 1 ponto de habilidade."
                        : no.getStringOr("motivo", "") + ".";
                corV = no.getIntOr("estado", 0) == PacoteFicha.DISPONIVEL ? COR_OK : COR_SANGUE_CLARO;
            } else if (no.getBooleanOr("equipavel", false)) {
                sit = no.getIntOr("versaoEquipada", -1) == versaoDetalhe ? "Equipado: é esta versão que vale ao usar o poder."
                        : "Clique em EQUIPAR para usar esta versão (gasta " + dv.getIntOr("custo", 0) + " PE).";
                corV = COR_OK;
            } else {
                sit = "Aprendido.";
                corV = COR_OK;
            }
            if (concedido && liberadaV) {
                sit = "Concedido pela classe. " + sit;
            }
            paragrafo(g, sit, x, y + 3, w, corV, yMax);
            soltar(g);
            return;
        }
        String tipo = no.getBooleanOr("ativa", false) ? "ATIVA · " + no.getIntOr("custo", 0) + " PE" : "PASSIVA";
        texto(g, ajustar(no.getStringOr("nome", "").toUpperCase(java.util.Locale.ROOT) + "  (" + tipo + ")", w), x, y,
                COR_DOURADO);
        y += 12;
        y = paragrafo(g, no.getStringOr("descricao", ""), x, y, w, COR_OSSO, yMax);
        int estado = no.getIntOr("estado", 0);
        String situacao;
        int cor;
        if (no.getBooleanOr("repetivel", false)) {
            int vezes = no.getIntOr("vezes", 0);
            situacao = (vezes > 0 ? "Escolhido " + vezes + (vezes == 1 ? " vez. " : " vezes. ") : "")
                    + (estado == PacoteFicha.DISPONIVEL ? "Cada escolha custa 1 ponto de habilidade."
                            : no.getStringOr("motivo", "") + ".");
            cor = estado == PacoteFicha.DISPONIVEL ? COR_OK : COR_SANGUE_CLARO;
        } else if (estado == PacoteFicha.APRENDIDA) {
            situacao = concedido ? "Concedido pela classe." : "Aprendido.";
            cor = COR_OK;
        } else if (estado == PacoteFicha.DISPONIVEL) {
            situacao = "Pronto para aprender: custa 1 ponto de habilidade.";
            cor = COR_OK;
        } else {
            situacao = no.getStringOr("motivo", "") + ".";
            cor = COR_SANGUE_CLARO;
        }
        if (no.getIntOr("versoes", 0) > 1) {
            situacao += " Escalona nos NEX " + no.getStringOr("evolucoes", "") + ": cada ícone ao lado é uma versão.";
        }
        paragrafo(g, situacao, x, y + 3, w, cor, yMax);
        soltar(g);
    }

    // ---------------------------------------------------------------- aba TRILHA

    private void desenharTrilhas(GuiGraphicsExtractor g, int mx, int my) {
        iniciarLista(g, corpoY, rodapeY - 3 - corpoY);
        if (trilhas.isEmpty()) {
            texto(g, "Nenhuma trilha para mostrar.", cx + 8, listaY + 6, COR_APAGADO);
            terminarLista(g, listaY + 20);
        } else {
            int escolhida = inteiro("trilha");
            int colunaW = Math.max(caixaPx + 22, 66);
            int xIni = cx + 8;
            int xMax = cx + cw - 8 - 6;
            int porFaixa = Math.max(1, (xMax - xIni) / colunaW);
            int n = trilhas.size();
            int x = xIni + (n <= porFaixa ? (xMax - xIni - n * colunaW) / 2 : 0);
            int y = listaY + 6 - rolagem;
            int alturaFaixa = 0;
            for (int i = 0; i < n; i++) {
                if (i > 0 && i % porFaixa == 0) { // nova faixa de colunas
                    x = xIni;
                    y += alturaFaixa + 14;
                    alturaFaixa = 0;
                }
                CompoundTag t = trilhas.get(i);
                boolean essa = escolhida == t.getIntOr("indice", 0);
                boolean fechada = escolhida >= 0 && !essa;
                alturaFaixa = Math.max(alturaFaixa, desenharColunaDeTrilha(g, mx, my, i, t, x, y, colunaW, essa, fechada));
                x += colunaW;
            }
            terminarLista(g, y + alturaFaixa);
        }
        desenharRodapeTrilha(g, mx, my);
        desenharDetalheTrilha(g);
    }

    /** Uma coluna: o nome da trilha e os 4 poderes dela (NEX 10%, 40%, 65% e 99%). Devolve a altura usada. */
    private int desenharColunaDeTrilha(GuiGraphicsExtractor g, int mx, int my, int i, CompoundTag t, int x, int y, int colunaW,
            boolean essa, boolean fechada) {
        int nivel = inteiro("nivel");
        boolean podeEscolher = inteiro("trilha") < 0 && nivel >= Trilha.NIVEL_MINIMO;
        int hx = x + 3;
        int hw = colunaW - 6;

        // título da trilha
        String chaveT = "t:" + i;
        boolean passouT = alvo(mx, my, hx, y, hw, ALTURA_TITULO_TRILHA, chaveT, true);
        if (chaveT.equals(selecionado)) {
            g.fill(hx - 2, y - 2, hx + hw + 2, y + ALTURA_TITULO_TRILHA + 2, COR_SANGUE_CLARO);
        }
        g.fill(hx, y, hx + hw, y + ALTURA_TITULO_TRILHA, essa ? COR_DOURADO : podeEscolher ? COR_OK : COR_BORDA);
        g.fill(hx + 1, y + 1, hx + hw - 1, y + ALTURA_TITULO_TRILHA - 1, passouT ? 0xFF1F1F27 : COR_ZONA);
        List<FormattedCharSequence> linhasNome = font.split(FonteOP.c(t.getStringOr("nome", "")), hw - 4);
        int nLinhas = Math.min(2, linhasNome.size());
        int topoTexto = y + 2 + (ALTURA_TITULO_TRILHA - 4 - nLinhas * 10) / 2;
        for (int k = 0; k < nLinhas; k++) {
            int lw = font.width(linhasNome.get(k));
            FonteOP.desenhar(g, font, linhasNome.get(k), hx + (hw - lw) / 2F, topoTexto + k * 10 + DESLOC_TEXTO,
                    essa ? COR_DOURADO : fechada ? COR_APAGADO : COR_OSSO, false);
        }

        // os 4 poderes
        ListTag hs = t.getListOrEmpty("habilidades");
        int passo = caixaPx + 11 + espacoSeta;
        int py = y + ALTURA_TITULO_TRILHA + 8;
        int bx = x + (colunaW - caixaPx) / 2;
        for (int k = 0; k < NIVEIS_TRILHA.length; k++) {
            CompoundTag h = k < hs.size() ? hs.getCompoundOrEmpty(k) : null;
            int by = py + k * passo;
            boolean liberado = h != null && h.getBooleanOr("liberada", false);
            boolean daParaEscolher = h != null && podeEscolher;
            String chaveP = "p:" + i + ":" + k;
            boolean passouP = alvo(mx, my, bx, by, caixaPx, caixaPx, chaveP, true);
            desenharMoldura(g, bx, by, chaveP.equals(selecionado), passouP,
                    liberado ? COR_DOURADO : daParaEscolher ? COR_OK : COR_BORDA);
            if (h != null) {
                IconesOP.desenhar(g, h.getStringOr("id", ""), bx + (caixaPx - iconePx) / 2, by + (caixaPx - iconePx) / 2,
                        iconePx, liberado ? 1F : fechada ? 0.2F : daParaEscolher ? 0.9F : 0.4F);
            } else {
                String interrogacao = "?";
                textoNaCaixa(g, interrogacao, bx + (caixaPx - FonteOP.largura(font, interrogacao)) / 2F, by, caixaPx,
                        COR_APAGADO);
            }
            String nex = "NEX " + HabilidadesDoPersonagem.nexDoNivel(NIVEIS_TRILHA[k]) + "%";
            textoNaCaixa(g, nex, x + (colunaW - FonteOP.largura(font, nex)) / 2F, by + caixaPx + 1, 10,
                    liberado ? COR_DOURADO : COR_APAGADO);
            if (k < NIVEIS_TRILHA.length - 1) {
                boolean proximoLiberado = k + 1 < hs.size() && hs.getCompoundOrEmpty(k + 1).getBooleanOr("liberada", false);
                desenharSeta(g, bx + caixaPx / 2, by + caixaPx + 11, espacoSeta, proximoLiberado ? COR_DOURADO : COR_SETA);
            }
        }
        int usado = ALTURA_TITULO_TRILHA + 8 + NIVEIS_TRILHA.length * passo - espacoSeta;
        if (fechada) {
            String rotulo = "FECHADA";
            texto(g, rotulo, x + (colunaW - FonteOP.largura(font, rotulo)) / 2F, y + usado + 2, COR_SANGUE);
            usado += 12;
        }
        return usado;
    }

    private void desenharRodapeTrilha(GuiGraphicsExtractor g, int mx, int my) {
        int indiceTrilha = inteiro("trilha");
        Acao acao = acaoDaTrilha();
        int xBotao = botaoRodape(g, mx, my, acao.rotulo(), cx + cw, acao.ativa(), acao.perigo(), "btn:trilha");
        String situacao = indiceTrilha >= 0 ? "Trilha: " + Trilha.values()[indiceTrilha].getNome()
                : "Trilha: ainda não escolhida";
        textoNaCaixa(g, ajustar(situacao, xBotao - cx - 8), cx + 2, rodapeY, 11, indiceTrilha >= 0 ? COR_OK : COR_DOURADO);
    }

    private void desenharDetalheTrilha(GuiGraphicsExtractor g) {
        painel(g, cx, detalheY, cw, detalheH);
        int x = cx + 6;
        int w = cw - 12;
        int yMax = detalheY + detalheH - 4;
        int y = detalheY + 6;
        recortar(g, cx + 1, detalheY + 1, cw - 2, detalheH - 2);

        String chave = sobre.startsWith("t:") || sobre.startsWith("p:") ? sobre : selecionado;
        CompoundTag t = trilhaPorChave(chave);
        if (t == null) {
            y = paragrafo(g, "Clique numa trilha ou num poder dela para ver os detalhes. Escolher uma trilha fecha "
                    + "as outras para sempre.", x, y, w, COR_APAGADO, yMax);
            paragrafo(g, "A trilha se escolhe uma vez só, a partir do NEX "
                    + HabilidadesDoPersonagem.nexDoNivel(Trilha.NIVEL_MINIMO) + "%.", x, y + 4, w, COR_APAGADO, yMax);
            soltar(g);
            return;
        }
        int escolhida = inteiro("trilha");
        boolean essa = escolhida == t.getIntOr("indice", 0);
        CompoundTag h = poderDeTrilhaPorChave(chave);
        String situacao;
        int cor;
        if (h == null) {
            texto(g, ajustar(t.getStringOr("nome", "").toUpperCase(java.util.Locale.ROOT) + "  (TRILHA)", w), x, y,
                    COR_DOURADO);
            y += 12;
            y = paragrafo(g, t.getStringOr("descricao", ""), x, y, w, COR_OSSO, yMax);
            if (t.getListOrEmpty("habilidades").isEmpty()) {
                y = paragrafo(g, "Os poderes desta trilha ainda não foram criados.", x, y + 2, w, COR_APAGADO, yMax);
            }
        } else {
            String tipo = h.getBooleanOr("ativa", false) ? "ATIVA · " + h.getIntOr("custo", 0) + " PE" : "PASSIVA";
            texto(g, ajustar(h.getStringOr("nome", "").toUpperCase(java.util.Locale.ROOT) + "  (" + tipo + " · NEX "
                    + h.getIntOr("nex", 0) + "%)", w), x, y, COR_DOURADO);
            y += 12;
            y = paragrafo(g, h.getStringOr("descricao", ""), x, y, w, COR_OSSO, yMax);
        }
        if (essa) {
            situacao = h != null && !h.getBooleanOr("liberada", false)
                    ? "Sua trilha. Este poder chega no NEX " + h.getIntOr("nex", 0) + "%."
                    : "Esta é a sua trilha.";
            cor = COR_OK;
        } else if (escolhida >= 0) {
            situacao = "Você já escolheu outra trilha: esta está fechada para sempre.";
            cor = COR_APAGADO;
        } else if (inteiro("nivel") < Trilha.NIVEL_MINIMO) {
            situacao = "Disponível a partir do NEX " + HabilidadesDoPersonagem.nexDoNivel(Trilha.NIVEL_MINIMO) + "%.";
            cor = COR_APAGADO;
        } else if (confirmando) {
            situacao = "Atenção: clique em CONFIRMAR para ficar com esta trilha. Não dá para trocar depois!";
            cor = COR_PERIGO;
        } else {
            situacao = "A escolha é definitiva: não dá para trocar de trilha depois.";
            cor = COR_DOURADO;
        }
        paragrafo(g, situacao, x, y + 3, w, cor, yMax);
        soltar(g);
    }

    // ---------------------------------------------------------------- coluna da esquerda

    private void desenharEsquerda(GuiGraphicsExtractor g, int mx, int my) {
        painel(g, esqX, topo, esqW, altura);
        int centro = esqX + esqW / 2;
        int alturaSkin = Mth.clamp((int) (altura * 0.30F), 54, 120);
        int pes = topo + 8 + alturaSkin;
        // brilho atrás do personagem, igual ao da tela de criação
        float pulso = 0.85F + 0.15F * (float) Math.sin(relogio() * 1.6);
        halo(g, centro, pes - alturaSkin * 0.5F, (int) (esqW * 0.95F), 0.30F * pulso, 0.75F, 0.85F, 1.0F);
        halo(g, centro, pes + 2, (int) (esqW * 0.55F), 0.18F * pulso, 0.9F, 0.15F, 0.25F);

        int y = pes + 8;
        if (minecraft != null && minecraft.player != null) {
            int escala = Math.max(18, (int) (alturaSkin / 1.95F));
            // a caixa tem o centro na altura do meio do corpo (a entidade é desenhada centrada na caixa)
            int meioCorpo = Math.round(pes - escala * 0.9F);
            DesenhoOP.entidade(g, esqX + 2, meioCorpo - alturaSkin / 2, esqX + esqW - 2, meioCorpo + alturaSkin / 2,
                    escala, mx, my, minecraft.player);
        }

        recortar(g, esqX + 1, topo + 1, esqW - 2, altura - 2); // todo o texto abaixo fica dentro da caixa
        if (minecraft != null && minecraft.player != null) {
            String nome = ajustar(minecraft.player.getName().getString(), esqW - 10);
            FonteOP.desenharTitulo(g, font, nome, centro - FonteOP.larguraTitulo(font, nome) / 2F, y, COR_OSSO, false);
        }
        y += 14;

        int px = esqX + 6;
        int pw = esqW - 12;
        int yMax = topo + altura - 4;

        ClasseOP classe = ClasseOP.values()[Mth.clamp(inteiro("classe"), 0, ClasseOP.values().length - 1)];
        int indiceOrigem = inteiro("origem");
        int indiceTrilha = inteiro("trilha");
        y = linha(g, "Classe", DescricoesOP.nomeClasse(classe), px, y, pw, yMax);
        y = linha(g, "Origem", indiceOrigem >= 0 ? Origem.values()[indiceOrigem].getNome() : "—", px, y, pw, yMax);
        y = linha(g, "Trilha", indiceTrilha >= 0 ? Trilha.values()[indiceTrilha].getNome() : "—", px, y, pw, yMax);

        // barra de NEX (5% a 99%)
        y += 2;
        if (y + 14 <= yMax) {
            int nex = inteiro("nex");
            String rotulo = ajustar("NEX " + nex + "%  ·  Nível " + inteiro("nivel"), pw);
            texto(g, rotulo, px, y, COR_OSSO);
            y += 11;
            float fracao = Mth.clamp((nex - 5) / 94F, 0F, 1F);
            g.fill(px, y, px + pw, y + 4, COR_BORDA);
            g.fill(px + 1, y + 1, px + pw - 1, y + 3, 0xFF050508);
            g.fill(px + 1, y + 1, px + 1 + Math.round((pw - 2) * fracao), y + 3, COR_SANGUE_CLARO);
            y += 10;
        }

        y = linha(g, "PV", String.valueOf(inteiro("vida")), px, y, pw, yMax);
        y = linha(g, "SAN", String.valueOf(inteiro("sanidade")), px, y, pw, yMax);
        y = linha(g, "PE", String.valueOf(inteiro("esforco")), px, y, pw, yMax);
        y += 3;
        y = linha(g, "Defesa (bônus)", "+" + inteiro("defesa"), px, y, pw, yMax);
        y = linha(g, "Crítico corpo", inteiro("critCorpo") + "%", px, y, pw, yMax);
        y = linha(g, "Crítico dist.", inteiro("critDistancia") + "%", px, y, pw, yMax);
        y = linha(g, "Resist. física", inteiro("resistFisica") + "%", px, y, pw, yMax);
        y = linha(g, "Resist. mental", inteiro("resistMental") + "%", px, y, pw, yMax);
        y = linha(g, "Velocidade", "+" + inteiro("velocidade") + "%", px, y, pw, yMax);
        String dano = String.format(java.util.Locale.forLanguageTag("pt-BR"), "+%.1f", d.getDoubleOr("danoForca", 0.0));
        linha(g, "Dano (Força)", dano, px, y, pw, yMax);
        soltar(g);
    }

    /** Rótulo à esquerda e valor à direita na mesma linha; se não couberem os dois, o rótulo é que encolhe. */
    private int linha(GuiGraphicsExtractor g, String rotulo, String valor, int x, int y, int largura, int yMax) {
        if (y + 10 > yMax) {
            return y;
        }
        // rótulo em Cinzel (dourado) e valor em Cormorant, como no painel da tela de criação
        String v = ajustar(valor, largura * 2 / 3);
        int larguraValor = FonteOP.largura(font, v);
        String r = rotulo;
        int maxRotulo = largura - larguraValor - 6;
        while (r.length() > 1 && FonteOP.larguraTitulo(font, r) > maxRotulo) {
            r = r.substring(0, r.length() - 1);
        }
        FonteOP.desenharTitulo(g, font, r, x, y, COR_DOURADO, false);
        texto(g, v, x + largura - larguraValor, y, COR_OSSO);
        return y + 12;
    }

    // ---------------------------------------------------------------- centro: pentágono

    /** Distância do rótulo (sigla + número) ao vértice do pentágono. */
    private static final float RAIO_ROTULO = 24F;
    /** Sigla acima do centro do rótulo, número abaixo, para as duas linhas nunca se tocarem. */
    private static final float DESLOC_SIGLA = -13F;
    private static final float DESLOC_NUMERO = 3.5F;
    /** Topo do botão "+" em relação ao centro do rótulo (alinhado com o número). */
    private static final int DESLOC_MAIS = -2;

    private double pontoX(int i, double r) {
        return pentCx + r * Math.cos(Math.toRadians(-90 + 72 * i));
    }

    private double pontoY(int i, double r) {
        return pentCy + r * Math.sin(Math.toRadians(-90 + 72 * i));
    }

    private void desenharPentagono(GuiGraphicsExtractor g) {
        int n = Atributo.values().length;
        // brilho + símbolo ritual (o mesmo em HD da tela inicial e da criação) atrás do pentágono
        float respira = 0.8F + 0.2F * (float) Math.sin(relogio() * 1.2);
        halo(g, pentCx, pentCy, (int) (pentRaio * 1.9F), 0.22F * respira, 0.7F, 0.8F, 1.0F);
        int lado = Math.round(pentRaio * 2.74F);
        // o centro do circulo interno do simbolo fica a 53,9% da altura da textura (igual a TelaInicial e TelaCriacao)
        g.blit(RenderPipelines.GUI_TEXTURED, CIRCULO, pentCx - lado / 2, pentCy - Math.round(0.539F * lado), 0F, 0F, lado,
                lado, 1024, 1024, 1024, 1024, DesenhoOP.argb(0.45F * respira, 0.85F, 0.9F, 1F));
        for (int nivel = 1; nivel <= Atributos.MAXIMO_NATURAL; nivel++) {
            double r = pentRaio * nivel / Atributos.MAXIMO_NATURAL;
            int corAnel = nivel == Atributos.MAXIMO_NA_CRIACAO ? 0xFF8A7D57
                    : nivel == Atributos.MAXIMO_NATURAL ? 0xFF4A4A56 : 0xFF2A2A33;
            float espessura = nivel == Atributos.MAXIMO_NA_CRIACAO ? 1.4F : 1F;
            for (int i = 0; i < n; i++) {
                linhaReta(g, pontoX(i, r), pontoY(i, r), pontoX(i + 1, r), pontoY(i + 1, r), espessura, corAnel);
            }
        }
        for (int i = 0; i < n; i++) {
            linhaReta(g, pentCx, pentCy, pontoX(i, pentRaio), pontoY(i, pentRaio), 1F, 0xFF2A2A33);
        }
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            double r = pentRaio * Mth.clamp(atributos[i], 0, Atributos.MAXIMO_NATURAL) / Atributos.MAXIMO_NATURAL;
            xs[i] = (float) pontoX(i, r);
            ys[i] = (float) pontoY(i, r);
        }
        DesenhoOP.poligono(g, xs, ys, 0x70B0172B);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            linhaReta(g, xs[i], ys[i], xs[j], ys[j], 1.6F, COR_SANGUE_CLARO);
        }

        for (int i = 0; i < n; i++) {
            int px = Math.round(xs[i]);
            int py = Math.round(ys[i]);
            int meio = i == atributoSobMouse ? 3 : 2;
            g.fill(px - meio, py - meio, px + meio + 1, py + meio + 1, COR_OSSO);

            // rótulo: sigla em cima, valor grande embaixo (bem separados), e o botão "+" ao lado do número
            double lx = pontoX(i, pentRaio + RAIO_ROTULO);
            double ly = pontoY(i, pentRaio + RAIO_ROTULO);
            boolean sobre = i == atributoSobMouse;
            String sigla = Atributo.values()[i].getSigla();
            FonteOP.desenhar(g, font, sigla, Math.round(lx - FonteOP.largura(font, sigla) / 2F),
                    Math.round(ly + DESLOC_SIGLA - CENTRO_VISUAL), sobre ? COR_OSSO : COR_DOURADO, false);
            String valor = String.valueOf(atributos[i]);
            g.pose().pushMatrix();
            g.pose().translate((float) lx, (float) (ly + DESLOC_NUMERO));
            g.pose().scale(1.4F, 1.4F);
            FonteOP.desenhar(g, font, valor, -FonteOP.largura(font, valor) / 2F, -CENTRO_VISUAL, COR_OSSO, false);
            g.pose().popMatrix();

            int bx = (int) lx + 12;
            int by = (int) ly + DESLOC_MAIS;
            boolean sobreMais = sobre && maisSobMouse;
            boolean ativo = podeSubirAtributo(i); // verde quando dá para gastar um ponto; apagado sem pontos ou no máximo
            int bordaMais = ativo ? (sobreMais ? COR_OSSO : COR_OK) : (sobreMais ? COR_APAGADO : COR_BORDA);
            g.fill(bx, by, bx + 11, by + 11, bordaMais);
            g.fill(bx + 1, by + 1, bx + 10, by + 10, COR_ZONA);
            textoNaCaixa(g, "+", bx + 5.5F - FonteOP.largura(font, "+") / 2F, by, 11, ativo ? COR_OK : COR_APAGADO);
        }
    }

    /** Botão de 11 px de altura com o texto centralizado (e preso dentro da caixa). */
    private void desenharBotao(GuiGraphicsExtractor g, String rotulo, int x, int y, int w, boolean sobre, boolean ativo,
            boolean perigo) {
        int borda = perigo ? COR_SANGUE_CLARO : sobre ? COR_APAGADO : COR_BORDA;
        g.fill(x, y, x + w, y + 11, borda);
        g.fill(x + 1, y + 1, x + w - 1, y + 10, perigo ? 0xFF2A0A10 : COR_ZONA);
        recortar(g, x + 1, y + 1, w - 2, 9);
        String t = ajustar(rotulo, w - 6);
        int cor = !ativo ? COR_APAGADO : perigo ? COR_PERIGO : COR_OSSO;
        textoNaCaixa(g, t, x + (w - FonteOP.largura(font, t)) / 2F, y, 11, cor);
        soltar(g);
    }

    /** Escreve um texto quebrando em linhas. Devolve o Y logo abaixo do que foi escrito. */
    private int paragrafo(GuiGraphicsExtractor g, String texto, int x, int y, int largura, int cor, int yMax) {
        // texto corrido em Tektur com sombra (a Cormorant desenha números em "estilo antigo", difíceis de ler)
        for (FormattedCharSequence linha : font.split(FonteOP.c(texto), largura)) {
            if (y + 10 > yMax) {
                return yMax + 1;
            }
            FonteOP.desenhar(g, font, linha, x, y + DESLOC_TEXTO, cor, true);
            y += 11;
        }
        return y;
    }

    // ---------------------------------------------------------------- desenho "na mão"

    /** Caixa com borda de 1 px. O fundo é desenhado só por dentro, para continuar translúcido (a névoa aparece). */
    private static void caixa(GuiGraphicsExtractor g, int x, int y, int w, int h, int fundo, int borda) {
        g.fill(x + 1, y + 1, x + w - 1, y + h - 1, fundo);
        g.fill(x, y, x + w, y + 1, borda);
        g.fill(x, y + h - 1, x + w, y + h, borda);
        g.fill(x, y + 1, x + 1, y + h - 1, borda);
        g.fill(x + w - 1, y + 1, x + w, y + h - 1, borda);
    }

    /** Painel translúcido com os "cantos de ritual" dourados (igual ao da tela de criação). */
    private static void painel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        caixa(g, x, y, w, h, COR_PAINEL, COR_BORDA);
        int t = 4;
        int cor = 0xCCC2B48C;
        g.fill(x - 1, y - 1, x + t, y, cor);
        g.fill(x - 1, y - 1, x, y + t, cor);
        g.fill(x + w - t, y - 1, x + w + 1, y, cor);
        g.fill(x + w, y - 1, x + w + 1, y + t, cor);
        g.fill(x - 1, y + h, x + t, y + h + 1, cor);
        g.fill(x - 1, y + h - t, x, y + h + 1, cor);
        g.fill(x + w - t, y + h, x + w + 1, y + h + 1, cor);
        g.fill(x + w, y + h - t, x + w + 1, y + h + 1, cor);
    }

    private static void linhaReta(GuiGraphicsExtractor g, double x1, double y1, double x2, double y2, float espessura,
            int argb) {
        DesenhoOP.linha(g, x1, y1, x2, y2, espessura, argb);
    }
}
