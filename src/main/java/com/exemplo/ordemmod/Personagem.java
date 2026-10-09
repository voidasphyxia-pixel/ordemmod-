package com.exemplo.ordemmod;

import java.util.List;
import java.util.Random;

/**
 * Um personagem de Ordem Paranormal: junta tudo o que você já criou
 * (classe, atributos, NEX, PE, habilidades, efeitos) em um objeto só.
 * Cada jogador do Minecraft terá um. Java puro, sem nada do Minecraft aqui.
 */
public class Personagem {
    public final ClasseOP classe;
    public final Atributos atributos = new Atributos();
    public final NivelExposicao nex = new NivelExposicao();
    public final Pericias pericias;
    public final EfeitosAtivos efeitos = new EfeitosAtivos();
    public final HabilidadesDoPersonagem habilidades;
    public final PontosEsforco pe;
    private int sanidade;
    private Origem origem;
    /** false até o jogador terminar a tela de criação de personagem. */
    private boolean criado = false;
    /** Como está a mente do personagem, conforme a sanidade. */
    public enum Estado { ESTAVEL, ABALADO, ENLOUQUECENDO }

    /** Chance base de crítico e de resistir a efeitos (placeholder: depois virá das perícias). */
    public static final double CRITICO_BASE = 0.15;
    public static final double RESISTENCIA_BASE = 0.15;

    /** Segundos seguidos no escuro para perder 1 de SAN. */
    public static final int ESCURO_SEGUNDOS = 20;
    /** Segundos seguidos num lugar bem iluminado para recuperar 1 de SAN. */
    public static final int CLARO_SEGUNDOS = 60;

    private int segundosNoEscuro = 0;
    private int segundosNoClaro = 0;
    private Estado estadoConhecido = Estado.ESTAVEL;
    private int selecionada = 0; // índice da habilidade ativa escolhida para a tecla (não é salvo)

    public Personagem(ClasseOP classe) {
        this.classe = classe;
        this.pericias = new Pericias(classe, atributos);
        // distribuição de teste (4 pontos): mude à vontade
        atributos.set(Atributo.FORCA, 3);
        atributos.set(Atributo.VIGOR, 3);
        this.habilidades = new HabilidadesDoPersonagem(classe, atributos);
        this.pe = new PontosEsforco(esforcoMaximo());
        this.sanidade = sanidadeMaxima();
    }

    /** O jogador já passou pela tela de criação? */
    public boolean isCriado() {
        return criado;
    }

    public void setCriado(boolean criado) {
        this.criado = criado;
    }

    // ------------------------------------------------------------------ origem

    public Origem getOrigem() {
        return origem;
    }

    /** Escolhe a origem (só uma vez). Dá as 2 perícias de graça e os bônus do poder. */
    public boolean escolherOrigem(Origem nova) {
        if (nova == null || origem != null) {
            return false;
        }
        origem = nova;
        pericias.definirOrigem(nova);
        pe.setMaximo(esforcoMaximo());
        pe.restaurarTudo();
        return true;
    }

    /** Ao carregar o jogo salvo. */
    public void restaurarOrigem(Origem salva) {
        origem = salva;
        pericias.restaurarOrigem(salva);
        pe.setMaximo(esforcoMaximo());
    }

    /** Valor de um efeito da origem (0 se não tem origem ou ela não mexe nisso). */
    public double origem(Origem.Efeito efeito) {
        return origem == null ? 0 : origem.valor(efeito);
    }

    /** Dano fixo da origem: Lutador (corpo a corpo e desarmado) e Militar (à distância). */
    public double danoFixoDeOrigem(TipoDano tipo) {
        return switch (tipo) {
            case CORPO_A_CORPO, DESARMADO -> origem(Origem.Efeito.DANO_CORPO);
            case A_DISTANCIA -> origem(Origem.Efeito.DANO_DISTANCIA);
            case OUTRO -> 0;
        };
    }

    public int nivel() {
        return nex.getNivel();
    }

    public int vidaMaxima() {
        return EfeitosAtributos.vidaMaximaTotal(classe, nivel(), atributos, habilidades)
                + (int) (origem(Origem.Efeito.VIDA_POR_NIVEL) * nivel()) // Desgarrado
                + (int) origem(Origem.Efeito.VIDA_FIXA); // Operário
    }

    public int sanidadeMaxima() {
        return classe.sanidadeMaxima(nivel());
    }

    public int esforcoMaximo() {
        return classe.esforcoMaximo(nivel(), atributos.get(Atributo.PRESENCA)) + (int) origem(Origem.Efeito.PE_EXTRA);
    }

    public double velocidadeExtra() {
        return EfeitosAtributos.velocidadeTotal(atributos, habilidades)
                + pericias.valor(Beneficio.ATLETISMO_VELOCIDADE) // Atletismo
                + origem(Origem.Efeito.VELOCIDADE); // Atleta
    }

    /** Chance de crítico para um tipo de dano: base + habilidades + Luta/Pontaria (máx. 100%). */
    public double chanceCritico(TipoDano tipo) {
        return Math.min(1.0, CRITICO_BASE + habilidades.chanceCritico(tipo) + pericias.bonusCritico(tipo)
                + origem(Origem.Efeito.CRITICO));
    }

    /** Chance de resistir a efeitos ruins, sem distinguir o tipo (usa a melhor entre Fortitude e Vontade). */
    public double chanceResistencia() {
        return Math.max(chanceResistencia(false), chanceResistencia(true));
    }

    /** Chance de resistir: base + habilidades + Vontade (efeito mental) ou Fortitude (físico). Máx. 100%. */
    public double chanceResistencia(boolean mental) {
        return Math.min(1.0, RESISTENCIA_BASE + habilidades.chanceResistirEfeitosTotal(efeitos, false)
                + pericias.bonusResistencia(mental)
                + origem(Origem.Efeito.RESISTENCIA)
                + (mental ? origem(Origem.Efeito.RESISTENCIA_MENTAL) : 0));
    }

    public boolean sorteouCritico(TipoDano tipo, Random random) {
        return random.nextDouble() < chanceCritico(tipo);
    }

    public boolean resistiuAoEfeito(Random random) {
        return random.nextDouble() < chanceResistencia();
    }

    /** Igual ao anterior, mas o efeito mental (escuridão, enjoo) pede Vontade e o físico pede Fortitude. */
    public boolean resistiuAoEfeito(Random random, boolean mental) {
        return random.nextDouble() < chanceResistencia(mental);
    }

    /**
     * Quanta SAN se perde de fato num susto: um teste de Vontade (DT 15, +5 se for paranormal)
     * tira 1 ponto da perda; Ocultismo treinado tira mais 1 se a fonte for paranormal.
     */
    public int perdaDeSanidadeAjustada(int perda, boolean paranormal, Random random) {
        int dt = Pericias.DT_MEDIA + (paranormal ? 5 : 0);
        int ajustada = perda;
        if (pericias.testar(Pericia.VONTADE, dt, random).sucesso()) {
            ajustada--;
            if (paranormal && pericias.estaTreinada(Pericia.OCULTISMO)) {
                ajustada--;
            }
        }
        if (paranormal) {
            ajustada -= (int) origem(Origem.Efeito.PERDA_SAN_PARANORMAL); // Cultista Arrependido
        }
        return Math.max(0, ajustada);
    }

    private static final int VONTADE_SEGUNDOS = 30;
    private int segundosDeVontade = 0;
    private int segundosDeOrigem = 0;

    public int getSanidade() {
        return sanidade;
    }

    public void setSanidade(int valor) {
        this.sanidade = Math.max(0, Math.min(valor, sanidadeMaxima()));
    }

    /** Abalado: SAN na metade ou menos. Enlouquecendo: SAN zerada. */
    public Estado estadoMental() {
        if (sanidade <= 0) {
            return Estado.ENLOUQUECENDO;
        }
        return sanidade * 2 <= sanidadeMaxima() ? Estado.ABALADO : Estado.ESTAVEL;
    }

    /** Se o estado mental mudou desde a última vez que perguntou, devolve o novo; senão, null. */
    public Estado verificarMudancaDeEstado() {
        Estado atual = estadoMental();
        if (atual == estadoConhecido) {
            return null;
        }
        estadoConhecido = atual;
        return atual;
    }

    public void perderSanidade(int quantidade) {
        sanidade = Math.max(0, sanidade - Math.max(0, quantidade));
        segundosNoClaro = 0; // um susto recente adia a recuperação
    }

    public void recuperarSanidade(int quantidade) {
        sanidade = Math.min(sanidadeMaxima(), sanidade + Math.max(0, quantidade));
    }

    /** Descansar de verdade (dormir): recupera toda a sanidade. */
    public void descansar() {
        sanidade = sanidadeMaxima();
    }

    /**
     * Chame uma vez por segundo. Ficar no escuro por muito tempo tira SAN;
     * ficar num lugar bem iluminado por muito tempo devolve um pouco.
     */
    public void passarSegundo(boolean escuro, boolean claro) {
        int regenOrigem = (int) origem(Origem.Efeito.SAN_A_CADA_60S); // Religioso
        if (regenOrigem > 0 && ++segundosDeOrigem >= 60) {
            segundosDeOrigem = 0;
            recuperarSanidade(regenOrigem);
        }
        int regenVontade = (int) pericias.valor(Beneficio.VONTADE_SANIDADE); // Vontade Veterano/Expert
        if (regenVontade > 0 && ++segundosDeVontade >= VONTADE_SEGUNDOS) {
            segundosDeVontade = 0;
            recuperarSanidade(regenVontade);
        }
        if (escuro) {
            segundosNoClaro = 0;
            segundosNoEscuro++;
            if (segundosNoEscuro >= ESCURO_SEGUNDOS) {
                segundosNoEscuro = 0;
                perderSanidade(1);
            }
        } else {
            segundosNoEscuro = 0;
            if (claro) {
                segundosNoClaro++;
                if (segundosNoClaro >= CLARO_SEGUNDOS) {
                    segundosNoClaro = 0;
                    recuperarSanidade(1);
                }
            } else {
                segundosNoClaro = 0;
            }
        }
    }

    /** Sobe NEX em %. Cada marco atingido dá SAN, pontos de habilidade e libera habilidades. */
    public boolean ganharNex(int porcento) {
        boolean subiu = false;
        for (int i = 0; i < porcento; i++) {
            if (nex.ganharUmPorCento()) {
                int n = nivel();
                sanidade += classe.sanidadeMaxima(n) - classe.sanidadeMaxima(n - 1);
                habilidades.aoSubirDeNivel(n);
                pericias.aoSubirDeNivel(n);
                subiu = true;
            }
        }
        pe.setMaximo(esforcoMaximo());
        return subiu;
    }

    /** Texto curto para a tela. A vida atual vem do jogo. */
    public String barra(float vidaAtual) {
        String texto = "PV " + (int) Math.ceil(vidaAtual) + "/" + vidaMaxima()
                + "   SAN " + sanidade + "/" + sanidadeMaxima() + sufixoDoEstado()
                + "   PE " + pe.getAtual() + "/" + pe.getMaximo()
                + "   " + nex;
        Habilidade escolhida = habilidadeSelecionada();
        if (escolhida != null) {
            texto += "  |  > " + escolhida.getNome() + " (" + escolhida.getCustoPe() + " PE)";
        }
        String ativos = efeitosNaTela();
        return ativos.isEmpty() ? texto : texto + "  |  " + ativos;
    }

    /**
     * Só a parte de habilidades da antiga barra de texto: a habilidade selecionada e os efeitos rodando.
     * (Vida, SAN, PE e NEX agora são barras na HUD.) Vazio se não há nada para mostrar.
     */
    public String linhaDeHabilidades() {
        Habilidade escolhida = habilidadeSelecionada();
        String texto = escolhida != null ? "> " + escolhida.getNome() + " (" + escolhida.getCustoPe() + " PE)" : "";
        String ativos = efeitosNaTela();
        if (ativos.isEmpty()) {
            return texto;
        }
        return texto.isEmpty() ? ativos : texto + "  |  " + ativos;
    }

    private String sufixoDoEstado() {
        return switch (estadoMental()) {
            case ESTAVEL -> "";
            case ABALADO -> " (abalado)";
            case ENLOUQUECENDO -> " (enlouquecendo)";
        };
    }

    /** A habilidade ativa que a tecla de usar vai disparar (null se não há nenhuma). */
    public Habilidade habilidadeSelecionada() {
        List<Habilidade> ativas = habilidades.getAtivas();
        if (ativas.isEmpty()) {
            return null;
        }
        if (selecionada >= ativas.size()) {
            selecionada = 0;
        }
        return ativas.get(selecionada);
    }

    /** Passa para a próxima habilidade ativa (volta à primeira depois da última). */
    public void proximaHabilidade() {
        List<Habilidade> ativas = habilidades.getAtivas();
        if (!ativas.isEmpty()) {
            selecionada = (selecionada + 1) % ativas.size();
        }
    }

    /** Habilidades ativas com efeito rodando agora, ex.: "Técnica Secreta 24s". */
    public String efeitosNaTela() {
        StringBuilder sb = new StringBuilder();
        for (Habilidade h : habilidades.getAtivas()) {
            String id = idRaiz(h);
            if (efeitos.estaAtivo(id)) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(h.getNome()).append(' ').append(efeitos.segundosRestantes(id)).append('s');
            }
        }
        return sb.toString();
    }

    /** O efeito é guardado com o id da primeira versão da habilidade. */
    private static String idRaiz(Habilidade h) {
        Habilidade atual = h;
        while (atual.getEvolucaoDe() != null) {
            Habilidade anterior = CatalogoHabilidades.porId(atual.getEvolucaoDe());
            if (anterior == null) {
                break;
            }
            atual = anterior;
        }
        return atual.getId();
    }
}
