package com.exemplo.ordemmod.marcado;

import com.exemplo.ordemmod.CodecsOrdem;
import com.exemplo.ordemmod.Rede;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Cliente -> servidor: "terminei a cena, estas foram as opcoes que escolhi" (indice por pergunta).
 * O servidor refaz a soma das tags sozinho; nunca confia em tags vindas do cliente (veja ReceptorMarcado).
 */
public class PacoteMarcado implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<PacoteMarcado> TYPE = Rede.tipo("marcado");
    public static final StreamCodec<RegistryFriendlyByteBuf, PacoteMarcado> CODEC =
            CodecsOrdem.de(PacoteMarcado::escrever, PacoteMarcado::ler);

    private final int[] escolhas;

    public PacoteMarcado(int[] escolhas) {
        this.escolhas = escolhas;
    }

    public int[] escolhas() {
        return escolhas;
    }

    public static void escrever(PacoteMarcado m, FriendlyByteBuf b) {
        b.writeVarInt(m.escolhas.length);
        for (int e : m.escolhas) {
            b.writeByte(e);
        }
    }

    public static PacoteMarcado ler(FriendlyByteBuf b) {
        int n = Math.min(b.readVarInt(), 64); // limite contra lixo
        int[] e = new int[n];
        for (int i = 0; i < n; i++) {
            e[i] = b.readByte();
        }
        return new PacoteMarcado(e);
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
