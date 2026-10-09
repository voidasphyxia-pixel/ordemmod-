package com.exemplo.ordemmod;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guarda o personagem dentro dos dados do próprio jogador, que o Minecraft já salva
 * junto com o mundo. Ao voltar ao mundo, o personagem é lido de lá.
 */
public final class SalvamentoOrdem {
    private static final String RAIZ = "OrdemMod";

    private SalvamentoOrdem() {
    }

    public static void salvar(ServerPlayer jogador, Personagem p) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("classe", p.classe.name());
        nbt.putBoolean("criado", p.isCriado());
        nbt.putInt("nex", p.nex.getNex());
        nbt.putInt("san", p.getSanidade());
        nbt.putInt("pe", p.pe.getAtual());
        for (Atributo atributo : Atributo.values()) {
            nbt.putInt("atr_" + atributo.name(), p.atributos.get(atributo));
        }
        nbt.putInt("pontos", p.habilidades.getPontos());
        if (p.habilidades.getTrilha() != null) {
            nbt.putString("trilha", p.habilidades.getTrilha().name());
        }
        ListTag lista = new ListTag();
        for (Habilidade habilidade : p.habilidades.getAprendidas()) {
            lista.add(StringTag.valueOf(habilidade.getId()));
        }
        nbt.put("habilidades", lista);

        CompoundTag pericias = new CompoundTag();
        for (Pericia pericia : Pericia.values()) {
            GrauTreinamento grau = p.pericias.getGrau(pericia);
            if (grau != GrauTreinamento.DESTREINADO) {
                pericias.putString(pericia.name(), grau.name());
            }
        }
        nbt.put("pericias", pericias);
        if (p.getOrigem() != null) {
            nbt.putString("origem", p.getOrigem().name());
        }
        nbt.putInt("melhorias_veterano", p.pericias.getMelhoriasVeterano());
        nbt.putInt("melhorias_expert", p.pericias.getMelhoriasExpert());
        nbt.putInt("treinos_extras", p.pericias.getTreinosExtras());
        CompoundTag repeticoes = new CompoundTag();
        for (Map.Entry<String, Integer> e : p.habilidades.getRepeticoes().entrySet()) {
            repeticoes.putInt(e.getKey(), e.getValue());
        }
        nbt.put("repeticoes", repeticoes);
        jogador.getPersistentData().put(RAIZ, nbt);
    }

    /** Lê o personagem salvo. Retorna null se não há nada salvo (jogador novo). */
    public static Personagem carregar(ServerPlayer jogador) {
        CompoundTag nbt = jogador.getPersistentData().getCompound(RAIZ);
        if (!nbt.contains("classe")) {
            return null;
        }
        try {
            Personagem p = new Personagem(ClasseOP.valueOf(nbt.getString("classe")));
            for (Atributo atributo : Atributo.values()) {
                p.atributos.set(atributo, nbt.getInt("atr_" + atributo.name()));
            }
            if (nbt.contains("origem")) {
                try {
                    p.restaurarOrigem(Origem.valueOf(nbt.getString("origem")));
                } catch (IllegalArgumentException ignorar) {
                    // origem que não existe mais: fica sem origem
                }
            }
            p.setCriado(nbt.getBoolean("criado")); // saves antigos não têm: abrem a tela de criação
            p.nex.setNex(nbt.getInt("nex"));
            p.pe.setMaximo(p.esforcoMaximo());
            p.pe.restaurarTudo();
            p.pe.gastar(p.pe.getMaximo() - nbt.getInt("pe"));
            p.setSanidade(nbt.getInt("san"));

            Trilha trilha = nbt.contains("trilha") ? Trilha.valueOf(nbt.getString("trilha")) : null;
            List<Habilidade> aprendidas = new ArrayList<>();
            for (Tag tag : nbt.getList("habilidades", Tag.TAG_STRING)) {
                Habilidade habilidade = CatalogoHabilidades.porId(tag.getAsString());
                if (habilidade != null) {
                    aprendidas.add(habilidade);
                }
            }
            p.habilidades.restaurar(nbt.getInt("pontos"), trilha, aprendidas);
            Map<String, Integer> repeticoes = new java.util.HashMap<>();
            CompoundTag salvasRep = nbt.getCompound("repeticoes");
            for (String chave : salvasRep.getAllKeys()) {
                repeticoes.put(chave, salvasRep.getInt(chave));
            }
            p.habilidades.restaurarRepeticoes(repeticoes);

            if (nbt.contains("pericias")) {
                CompoundTag salvas = nbt.getCompound("pericias");
                Map<Pericia, GrauTreinamento> graus = new EnumMap<>(Pericia.class);
                for (String chave : salvas.getAllKeys()) {
                    try {
                        graus.put(Pericia.valueOf(chave), GrauTreinamento.valueOf(salvas.getString(chave)));
                    } catch (IllegalArgumentException ignorar) {
                        // perícia ou grau que não existe mais: pula
                    }
                }
                p.pericias.restaurar(graus, nbt.getInt("melhorias_veterano"), nbt.getInt("melhorias_expert"));
                p.pericias.setTreinosExtras(nbt.getInt("treinos_extras"));
            } else {
                p.pericias.concederRetroativo(p.nivel()); // save de antes das perícias
            }
            return p;
        } catch (IllegalArgumentException erro) {
            return null; // dado estragado: começa um personagem novo
        }
    }
}
