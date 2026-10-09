package com.exemplo.ordemmod.marcado;

import com.exemplo.ordemmod.SonsOrdem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

/** Só cliente: toca os sons da cena (um disparo ou um laço com fade). Nomes = arquivos em sounds/marcado/. */
public final class AudioMarcado {
    private AudioMarcado() {
    }

    public static void tocar(String nome, float volume, float pitch) {
        var obj = SonsOrdem.MARCADO.get(nome);
        if (obj != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(obj.get(), pitch, volume));
        }
    }

    /** Um som em laço cujo volume sobe e desce devagar (o drone, o batimento). */
    public static final class Laco extends AbstractTickableSoundInstance {
        private float alvo;
        private boolean encerrar;

        public Laco(String nome, float alvoInicial) {
            super(SonsOrdem.MARCADO.get(nome).get(), SoundSource.MASTER, RandomSource.create());
            this.looping = true;
            this.delay = 0;
            // Não pode ser 0: o SoundEngine descarta sons que começam com volume zero (nunca tocariam).
            this.volume = 0.001f;
            this.relative = true;
            this.attenuation = SoundInstance.Attenuation.NONE;
            this.alvo = alvoInicial;
        }

        public void iniciar() {
            Minecraft.getInstance().getSoundManager().play(this);
        }

        public void volumeAlvo(float v) {
            this.alvo = v;
        }

        /** Some devagar e se encerra sozinho. */
        public void fim() {
            this.alvo = 0f;
            this.encerrar = true;
        }

        @Override
        public void tick() {
            float d = alvo - volume;
            volume += Math.max(-0.02f, Math.min(0.02f, d));
            if (encerrar && volume <= 0.001f) {
                stop();
            }
        }
    }
}
