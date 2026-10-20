#!/usr/bin/env python3
"""
Gera os dois sons da HUD de habilidades A PARTIR DE GRAVACOES que o mod ja tem (sem sintese e sem camadas):

  sounds/habilidade/usar.ogg    ao USAR uma habilidade: UMA batida de coracao (o "lub" de marcado/batimento_loop), limpa.
  sounds/habilidade/sem_pe.ogg  quando NAO da para usar (falta PE): o som de GLITCH da cena do Marcado (marcado/glitch),
                                sem o silencio do comeco e so o primeiro trecho (o original tem 1,9 s; aqui ~0,5 s).

O som toca no instante da tecla, junto com a animacao, entao os dois comecam no tempo zero.

Uso (na pasta do projeto):   python3 tools/gerar_sons_habilidade.py
Precisa de: numpy, scipy e ffmpeg (com libvorbis) no PATH. Os "botoes" estao no bloco CONFIG abaixo.
Saida mono, 44,1 kHz, igual aos outros sons do mod.
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import find_peaks

SR = 44100
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "ordemmod",
                    "sounds")
SAIDA = os.path.join(RAIZ, "habilidade")

# ------------------------------------------------------------------ CONFIG
BATIDA_INDICE = 2          # qual "lub" do batimento_loop (0..9; todos parecidos)
BATIDA_DURACAO = 0.24      # s. O "dub" vem 0,27 s depois do "lub": mais que isso e ele entra no corte
BATIDA_FADE = 0.09         # s de fade-out no fim
BATIDA_PICO = 0.60         # volume de pico (0..1); 0,6 deixa a media perto dos outros sons do mod

GLITCH_INICIO = 0.115      # s: o glitch original so comeca a soar em ~0,13 s; cortamos o silencio antes
GLITCH_DURACAO = 0.50      # s de glitch usados (o original tem 1,9 s)
GLITCH_FADE = 0.14         # s de fade-out no fim
GLITCH_PICO = 0.70


# ------------------------------------------------------------------ utilitarios
def ler(rel):
    caminho = os.path.join(RAIZ, rel)
    bruto = subprocess.run(["ffmpeg", "-v", "error", "-i", caminho, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                           capture_output=True, check=True).stdout
    return np.frombuffer(bruto, dtype=np.float32).astype(np.float64)


def fade_in(x, seg):
    n = min(len(x), int(SR * seg))
    x = x.copy()
    x[:n] *= np.sin(np.linspace(0, np.pi / 2, n)) ** 2
    return x


def fade_out(x, seg):
    n = min(len(x), int(SR * seg))
    x = x.copy()
    x[-n:] *= np.cos(np.linspace(0, np.pi / 2, n)) ** 2
    return x


def normaliza(x, pico):
    x = x - np.mean(x)
    return x / (np.max(np.abs(x)) + 1e-9) * pico


def batidas(x):
    """Posicoes (s) dos golpes do coracao em batimento_loop (lub, dub, lub, dub...)."""
    env = np.convolve(np.abs(x), np.ones(int(SR * 0.006)) / int(SR * 0.006), "same")
    picos, _ = find_peaks(env, height=0.35 * env.max(), distance=int(SR * 0.12))
    return picos / SR


def salvar(nome, amostras):
    os.makedirs(SAIDA, exist_ok=True)
    destino = os.path.join(SAIDA, nome + ".ogg")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, nome + ".wav")
        wavfile.write(wav, SR, (np.clip(amostras, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        destino], check=True)
    print(f"ok: {os.path.normpath(destino)} ({len(amostras) / SR:.2f} s)")


# ------------------------------------------------------------------ os dois sons
def usar():
    loop = ler("marcado/batimento_loop.ogg")
    tempos = batidas(loop)
    t = tempos[(2 * BATIDA_INDICE) % len(tempos)]           # posicoes pares = "lub"
    i = int(SR * max(0.0, t - 0.008))                       # 8 ms antes do golpe: ele comeca no tempo zero
    seg = loop[i: i + int(SR * BATIDA_DURACAO)]
    return normaliza(fade_out(fade_in(seg, 0.004), BATIDA_FADE), BATIDA_PICO)


def sem_pe():
    gl = ler("marcado/glitch.ogg")
    i = int(SR * GLITCH_INICIO)
    seg = gl[i: i + int(SR * GLITCH_DURACAO)]
    return normaliza(fade_out(fade_in(seg, 0.003), GLITCH_FADE), GLITCH_PICO)


if __name__ == "__main__":
    salvar("usar", usar())
    salvar("sem_pe", sem_pe())
