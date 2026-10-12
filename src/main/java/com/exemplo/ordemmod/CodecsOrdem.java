package com.exemplo.ordemmod;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * Ponte entre o jeito antigo (metodos estaticos escrever/ler num FriendlyByteBuf, como no Forge)
 * e o StreamCodec que o Fabric 26.x exige. Assim cada pacote mantem o escrever/ler que ja tinha.
 */
public final class CodecsOrdem {
    private CodecsOrdem() {
    }

    @FunctionalInterface
    public interface Escritor<T> {
        void escrever(T valor, FriendlyByteBuf buffer);
    }

    @FunctionalInterface
    public interface Leitor<T> {
        T ler(FriendlyByteBuf buffer);
    }

    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> de(Escritor<T> escritor, Leitor<T> leitor) {
        return new StreamCodec<>() {
            @Override
            public T decode(RegistryFriendlyByteBuf buffer) {
                return leitor.ler(buffer);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, T valor) {
                escritor.escrever(valor, buffer);
            }
        };
    }
}
