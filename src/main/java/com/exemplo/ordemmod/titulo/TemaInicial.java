package com.exemplo.ordemmod.titulo;

import com.exemplo.ordemmod.SonsOrdem;
import com.exemplo.ordemmod.marcado.VfxMarcado;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * O tema da tela inicial: um OGG em laco (sounds/menu/tema_inicial.ogg, ja com a emenda feita no proprio arquivo).
 * Toca na categoria MUSICA (o controle de volume de Musica do jogo vale), sobe devagar (~4 s) e continua tocando
 * nos submenus (Um jogador, Opcoes, Mods...). Ao entrar num mundo desce em ~1,5 s e para. So existe no cliente.
 */
@OnlyIn(Dist.CLIENT)
public final class TemaInicial extends AbstractTickableSoundInstance {
    /** Volume maximo do tema (0..1). O arquivo ja esta em -22 LUFS; baixe aqui se ainda ficar alto. */
    public static final float VOLUME = 1.0F;
    private static final float SUBIDA = 1.0F / 80.0F;   // por tick: ~4 s para chegar ao maximo
    private static final float DESCIDA = 1.0F / 30.0F;  // por tick: ~1,5 s para sumir

    private static TemaInicial atual;

    private boolean encerrar;
    private boolean liberouVfx;

    private TemaInicial() {
        super(SonsOrdem.TEMA_INICIAL.get(), SoundSource.MUSIC, RandomSource.create());
        this.looping = true;
        this.delay = 0;
        this.volume = 0.001F; // nao pode ser 0: o Minecraft descarta sons que comecam mudos
        this.relative = true;
        this.attenuation = SoundInstance.Attenuation.NONE;
    }

    /** Garante que o tema esteja tocando (nao faz nada se ja estiver). */
    public static void garantir() {
        Minecraft mc = Minecraft.getInstance();
        TemaInicial t = atual;
        if (t != null && !t.isStopped() && mc.getSoundManager().isActive(t)) {
            t.encerrar = false;
            return;
        }
        atual = new TemaInicial();
        mc.getSoundManager().play(atual);
    }

    /** True enquanto o tema esta ativo (usado para calar a musica do menu do jogo). */
    public static boolean tocando() {
        return atual != null && !atual.isStopped();
    }

    @Override
    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) { // entrou num mundo: o tema se despede
            encerrar = true;
            if (!liberouVfx) {
                liberouVfx = true;
                VfxMarcado.liberarTodos(); // solta a nevoa/poeira que o menu deixou na memoria de video
            }
        }
        if (encerrar) {
            volume = Math.max(0.0F, volume - DESCIDA * VOLUME);
            if (volume <= 0.001F) {
                stop();
                if (atual == this) {
                    atual = null;
                }
            }
        } else if (volume < VOLUME) {
            volume = Math.min(VOLUME, volume + SUBIDA * VOLUME);
        }
    }
}
