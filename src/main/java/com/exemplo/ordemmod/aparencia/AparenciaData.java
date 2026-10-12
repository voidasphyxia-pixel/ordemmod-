package com.exemplo.ordemmod.aparencia;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * A aparência de um personagem: tom de pele, uma peça por slot e a cor das peças que aceitam cor.
 * Imutável. Todo dado que entra (NBT ou rede) passa pelo construtor, que descarta o que não existe,
 * então um cliente mal-intencionado não consegue mandar lixo.
 */
public final class AparenciaData {
    /** Chave dentro dos dados do jogador (separada da chave "OrdemMod" do personagem). */
    public static final String RAIZ = "OrdemMod_Aparencia";
    private static final int SEM_COR = -1;
    private static final int MAX_PECAS = 16;

    private final int pele;
    private final Map<String, String> pecas;
    private final Map<String, Integer> cores;

    private AparenciaData(int pele, Map<String, String> pecas, Map<String, Integer> cores) {
        this.pele = Math.max(0, Math.min(Paletas.PELE.length - 1, pele));
        Map<String, String> pecasValidas = new LinkedHashMap<>();
        Map<String, Integer> coresValidas = new LinkedHashMap<>();
        for (String slot : CatalogoPecas.SLOTS) {
            String id = pecas.get(slot);
            CatalogoPecas.Peca peca = id == null ? null : CatalogoPecas.buscar(slot, id);
            if (peca == null) {
                continue;
            }
            pecasValidas.put(slot, id);
            if (peca.tingivel()) {
                Integer cor = cores.get(slot);
                coresValidas.put(slot, (cor == null ? peca.corPadrao() : cor) & 0xFFFFFF);
            }
        }
        this.pecas = Collections.unmodifiableMap(pecasValidas);
        this.cores = Collections.unmodifiableMap(coresValidas);
    }

    /** Pele mais clara, sem nenhuma peça. */
    public static AparenciaData padrao() {
        return new AparenciaData(0, Map.of(), Map.of());
    }

    public AparenciaData comPele(int indice) {
        return new AparenciaData(indice, pecas, cores);
    }

    /** Troca a peça de um slot. {@code id} nulo ou vazio tira a peça. */
    public AparenciaData comPeca(String slot, String id) {
        Map<String, String> novas = new LinkedHashMap<>(pecas);
        if (id == null || id.isEmpty()) {
            novas.remove(slot);
        } else {
            novas.put(slot, id);
        }
        return new AparenciaData(pele, novas, cores);
    }

    /** Troca a cor de um slot (só vale para peças que aceitam cor). */
    public AparenciaData comCor(String slot, int rgb) {
        Map<String, Integer> novas = new LinkedHashMap<>(cores);
        novas.put(slot, rgb);
        return new AparenciaData(pele, pecas, novas);
    }

    public int getPele() {
        return pele;
    }

    /** O id da peça do slot, ou null se o slot está vazio. */
    public String getPeca(String slot) {
        return pecas.get(slot);
    }

    /** A cor do slot (RGB). Se o slot não tem cor, devolve branco. */
    public int getCor(String slot) {
        return cores.getOrDefault(slot, 0xFFFFFF);
    }

    // ---- salvamento (NBT) ----

    public CompoundTag paraNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("pele", pele);
        CompoundTag pecasNbt = new CompoundTag();
        for (Map.Entry<String, String> e : pecas.entrySet()) {
            pecasNbt.putString(e.getKey(), e.getValue());
        }
        nbt.put("pecas", pecasNbt);
        CompoundTag coresNbt = new CompoundTag();
        for (Map.Entry<String, Integer> e : cores.entrySet()) {
            coresNbt.putInt(e.getKey(), e.getValue());
        }
        nbt.put("cores", coresNbt);
        return nbt;
    }

    public static AparenciaData deNbt(CompoundTag nbt) {
        Map<String, String> pecas = new LinkedHashMap<>();
        CompoundTag pecasNbt = nbt.getCompound("pecas");
        for (String slot : pecasNbt.getAllKeys()) {
            pecas.put(slot, pecasNbt.getString(slot));
        }
        Map<String, Integer> cores = new LinkedHashMap<>();
        CompoundTag coresNbt = nbt.getCompound("cores");
        for (String slot : coresNbt.getAllKeys()) {
            cores.put(slot, coresNbt.getInt(slot));
        }
        return new AparenciaData(nbt.getInt("pele"), pecas, cores);
    }

    // ---- rede ----

    public void escrever(FriendlyByteBuf buffer) {
        buffer.writeVarInt(pele);
        buffer.writeVarInt(pecas.size());
        for (Map.Entry<String, String> e : pecas.entrySet()) {
            buffer.writeUtf(e.getKey(), 32);
            buffer.writeUtf(e.getValue(), 48);
            buffer.writeInt(cores.getOrDefault(e.getKey(), SEM_COR));
        }
    }

    public static AparenciaData ler(FriendlyByteBuf buffer) {
        int pele = buffer.readVarInt();
        int quantidade = buffer.readVarInt();
        if (quantidade < 0 || quantidade > MAX_PECAS) {
            throw new IllegalArgumentException("aparencia invalida");
        }
        Map<String, String> pecas = new LinkedHashMap<>();
        Map<String, Integer> cores = new LinkedHashMap<>();
        for (int i = 0; i < quantidade; i++) {
            String slot = buffer.readUtf(32);
            String id = buffer.readUtf(48);
            int cor = buffer.readInt();
            pecas.put(slot, id);
            if (cor != SEM_COR) {
                cores.put(slot, cor);
            }
        }
        return new AparenciaData(pele, pecas, cores);
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof AparenciaData d)) {
            return false;
        }
        return pele == d.pele && pecas.equals(d.pecas) && cores.equals(d.cores);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pele, pecas, cores);
    }
}
