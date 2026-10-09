package com.exemplo.ordemmod.marcado;

import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

/**
 * Cliente -> servidor: "terminei a cena, estas foram as opções que escolhi" (índice por pergunta).
 * O servidor refaz a soma das tags sozinho; nunca confia em tags vindas do cliente.
 */
public class PacoteMarcado {
    private final int[] escolhas;

    public PacoteMarcado(int[] escolhas) {
        this.escolhas = escolhas;
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

    public static void tratar(PacoteMarcado m, Supplier<NetworkEvent.Context> contexto) {
        NetworkEvent.Context ctx = contexto.get();
        ctx.enqueueWork(() -> {
            ServerPlayer j = ctx.getSender();
            if (j == null) {
                return;
            }
            PerfilMarcado perfil = PerfilMarcado.carregar(j);
            if (perfil.concluido) {
                return; // a cena só vale uma vez (use /ordem resetar para repetir)
            }
            Map<String, Integer> tags = RoteiroMarcado.carregar().somarTags(m.escolhas);
            if (tags == null) {
                return; // escolha inválida: a cena abre de novo no próximo login
            }
            perfil.respostas = m.escolhas;
            perfil.tags.clear();
            perfil.tags.putAll(tags);
            perfil.concluido = true;
            perfil.salvar(j);
        });
        ctx.setPacketHandled(true);
    }
}
