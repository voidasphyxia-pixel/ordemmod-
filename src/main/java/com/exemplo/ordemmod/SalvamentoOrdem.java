package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerPlayer;

/**
 * Guarda o personagem dentro dos dados do proprio jogador (Data Attachment, veja AnexosOrdem),
 * que o Minecraft ja salva junto com o mundo. No Forge isso era um CompoundTag em getPersistentData();
 * agora e um record com Codec, o que tambem evita a API de NBT, que mudou bastante na 1.21.5.
 */
public final class SalvamentoOrdem {
    private SalvamentoOrdem() {
    }

    /** Tudo o que e salvo. Imutavel de proposito: o Fabric exige valores imutaveis nos attachments. */
    public record DadosSalvos(
            String classe,
            boolean criado,
            int nex,
            int san,
            int pe,
            Map<String, Integer> atributos,
            int pontos,
            String trilha,
            List<String> habilidades,
            Optional<Map<String, String>> pericias,
            String origem,
            int melhoriasVeterano,
            int melhoriasExpert,
            int treinosExtras,
            Map<String, Integer> repeticoes,
            // ausente = save de antes dos pontos de atributo (veja deDados)
            Optional<Integer> pontosAtributo) {

        public static final Codec<DadosSalvos> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("classe").forGetter(DadosSalvos::classe),
                // saves antigos nao tem "criado": abrem a tela de criacao
                Codec.BOOL.optionalFieldOf("criado", false).forGetter(DadosSalvos::criado),
                Codec.INT.optionalFieldOf("nex", 0).forGetter(DadosSalvos::nex),
                Codec.INT.optionalFieldOf("san", 0).forGetter(DadosSalvos::san),
                Codec.INT.optionalFieldOf("pe", 0).forGetter(DadosSalvos::pe),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("atributos", Map.of())
                        .forGetter(DadosSalvos::atributos),
                Codec.INT.optionalFieldOf("pontos", 0).forGetter(DadosSalvos::pontos),
                Codec.STRING.optionalFieldOf("trilha", "").forGetter(DadosSalvos::trilha),
                Codec.STRING.listOf().optionalFieldOf("habilidades", List.of()).forGetter(DadosSalvos::habilidades),
                // ausente = save de antes das pericias (veja deDados)
                Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("pericias")
                        .forGetter(DadosSalvos::pericias),
                Codec.STRING.optionalFieldOf("origem", "").forGetter(DadosSalvos::origem),
                Codec.INT.optionalFieldOf("melhorias_veterano", 0).forGetter(DadosSalvos::melhoriasVeterano),
                Codec.INT.optionalFieldOf("melhorias_expert", 0).forGetter(DadosSalvos::melhoriasExpert),
                Codec.INT.optionalFieldOf("treinos_extras", 0).forGetter(DadosSalvos::treinosExtras),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("repeticoes", Map.of())
                        .forGetter(DadosSalvos::repeticoes),
                Codec.INT.optionalFieldOf("pontos_atributo").forGetter(DadosSalvos::pontosAtributo)
        ).apply(i, DadosSalvos::new));
    }

    /** Personagem -> dados salvos. Java puro (da para testar sem mundo). */
    public static DadosSalvos paraDados(Personagem p) {
        Map<String, Integer> atributos = new LinkedHashMap<>();
        for (Atributo atributo : Atributo.values()) {
            atributos.put(atributo.name(), p.atributos.get(atributo));
        }
        List<String> habilidades = new ArrayList<>();
        for (Habilidade habilidade : p.habilidades.getAprendidas()) {
            habilidades.add(habilidade.getId());
        }
        Map<String, String> pericias = new LinkedHashMap<>();
        for (Pericia pericia : Pericia.values()) {
            GrauTreinamento grau = p.pericias.getGrau(pericia);
            if (grau != GrauTreinamento.DESTREINADO) {
                pericias.put(pericia.name(), grau.name());
            }
        }
        return new DadosSalvos(
                p.classe.name(),
                p.isCriado(),
                p.nex.getNex(),
                p.getSanidade(),
                p.pe.getAtual(),
                Map.copyOf(atributos),
                p.habilidades.getPontos(),
                p.habilidades.getTrilha() != null ? p.habilidades.getTrilha().name() : "",
                List.copyOf(habilidades),
                Optional.of(Map.copyOf(pericias)),
                p.getOrigem() != null ? p.getOrigem().name() : "",
                p.pericias.getMelhoriasVeterano(),
                p.pericias.getMelhoriasExpert(),
                p.pericias.getTreinosExtras(),
                Map.copyOf(p.habilidades.getRepeticoes()),
                Optional.of(p.atributos.getPontosLivres()));
    }

    /** Dados salvos -> personagem. Retorna null se os dados estao estragados. Java puro. */
    public static Personagem deDados(DadosSalvos d) {
        try {
            Personagem p = new Personagem(ClasseOP.valueOf(d.classe()));
            for (Atributo atributo : Atributo.values()) {
                p.atributos.set(atributo, d.atributos().getOrDefault(atributo.name(), 0));
            }
            if (!d.origem().isEmpty()) {
                try {
                    p.restaurarOrigem(Origem.valueOf(d.origem()));
                } catch (IllegalArgumentException ignorar) {
                    // origem que nao existe mais: fica sem origem
                }
            }
            p.setCriado(d.criado());
            p.nex.setNex(d.nex());
            // save de antes dos pontos de atributo: dá os dos marcos de NEX que ele já passou
            p.atributos.restaurarPontos(d.pontosAtributo().orElseGet(() -> Atributos.pontosAteNex(p.nex.getNex())));
            p.pe.setMaximo(p.esforcoMaximo());
            p.pe.restaurarTudo();
            p.pe.gastar(p.pe.getMaximo() - d.pe());
            p.setSanidade(d.san());

            Trilha trilha = d.trilha().isEmpty() ? null : Trilha.valueOf(d.trilha());
            List<Habilidade> aprendidas = new ArrayList<>();
            for (String id : d.habilidades()) {
                Habilidade habilidade = CatalogoHabilidades.porId(id);
                if (habilidade != null) {
                    aprendidas.add(habilidade);
                }
            }
            p.habilidades.restaurar(d.pontos(), trilha, aprendidas);
            p.habilidades.restaurarRepeticoes(new HashMap<>(d.repeticoes()));

            if (d.pericias().isPresent()) {
                Map<Pericia, GrauTreinamento> graus = new EnumMap<>(Pericia.class);
                for (Map.Entry<String, String> e : d.pericias().get().entrySet()) {
                    try {
                        graus.put(Pericia.valueOf(e.getKey()), GrauTreinamento.valueOf(e.getValue()));
                    } catch (IllegalArgumentException ignorar) {
                        // pericia ou grau que nao existe mais: pula
                    }
                }
                p.pericias.restaurar(graus, d.melhoriasVeterano(), d.melhoriasExpert());
                p.pericias.setTreinosExtras(d.treinosExtras());
            } else {
                p.pericias.concederRetroativo(p.nivel()); // save de antes das pericias
            }
            return p;
        } catch (IllegalArgumentException erro) {
            return null; // dado estragado: comeca um personagem novo
        }
    }

    public static void salvar(ServerPlayer jogador, Personagem p) {
        jogador.setAttached(AnexosOrdem.PERSONAGEM, paraDados(p));
    }

    /** Le o personagem salvo. Retorna null se nao ha nada salvo (jogador novo). */
    public static Personagem carregar(ServerPlayer jogador) {
        DadosSalvos dados = jogador.getAttached(AnexosOrdem.PERSONAGEM);
        return dados == null ? null : deDados(dados);
    }
}
