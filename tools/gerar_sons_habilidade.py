#!/usr/bin/env python3
"""
Gera os dois sons da HUD de habilidades (sintetizados aqui, sem amostras de terceiros):

  sounds/habilidade/usar.ogg    ao USAR uma habilidade: sopro que sobe, baque grave, brilho "ritual" e eco curto
  sounds/habilidade/sem_pe.ogg  quando NAO da para usar (falta PE): dois zumbidos graves e secos, descendo, dissonantes

Uso (na pasta do projeto):   python3 tools/gerar_sons_habilidade.py
Precisa de: numpy, scipy e ffmpeg (com libvorbis) no PATH.

Para trocar um som por outro seu, basta substituir o .ogg (mono, 44,1 kHz) com o mesmo nome; o sounds.json ja aponta para eles.
Os numeros do topo de cada funcao (frequencias, tempos, volumes) sao os botoes para mexer no timbre.
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, lfilter

SR = 44100
SAIDA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets",
                     "ordemmod", "sounds", "habilidade")
RNG = np.random.default_rng(7)  # semente fixa: o som sai igual toda vez


def t_(dur):
    return np.arange(int(SR * dur)) / SR


def passa_faixa(x, f1, f2):
    b, a = butter(2, [f1 / (SR / 2), f2 / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def passa_baixa(x, f):
    b, a = butter(2, f / (SR / 2))
    return lfilter(b, a, x)


def passa_alta(x, f):
    b, a = butter(2, f / (SR / 2), btype="high")
    return lfilter(b, a, x)


def eco(x, taps):
    """Ecos curtos (atraso em s, ganho), escurecidos, para dar corpo sem virar reverb longo."""
    saida = np.copy(x)
    for atraso, ganho in taps:
        n = int(SR * atraso)
        saida[n:] += passa_baixa(x, 2500)[: len(x) - n] * ganho
    return saida


def fecha(x, pico, fade=0.02):
    x = x / (np.max(np.abs(x)) + 1e-9) * pico
    n = int(SR * fade)
    x[-n:] *= np.linspace(1, 0, n)
    return x


def usar():
    dur = 0.62
    t = t_(dur)
    saida = np.zeros_like(t)
    # 1) sopro que sobe antes do golpe (0 .. 0.07 s)
    ataque = 0.07
    n = int(SR * ataque)
    sopro = passa_faixa(RNG.standard_normal(n), 700, 4200) * np.linspace(0, 1, n) ** 2 * 0.35
    saida[:n] += sopro
    # 2) baque grave: seno que desce de 150 para 46 Hz (a partir de 0.07 s)
    t2 = t_(dur - ataque)
    freq = 46 + (150 - 46) * np.exp(-t2 / 0.05)
    fase = 2 * np.pi * np.cumsum(freq) / SR
    baque = np.sin(fase) * np.exp(-t2 / 0.14) * 1.0
    saida[n:] += baque
    # 3) brilho "ritual": parciais de um sino escuro (quinta justa + oitava), decaimento rapido
    brilho = np.zeros_like(t2)
    for f, g, tau in [(220.0, 0.30, 0.22), (330.0, 0.22, 0.20), (660.0, 0.12, 0.14), (990.0, 0.07, 0.10),
                      (1397.0, 0.04, 0.07)]:
        brilho += g * np.sin(2 * np.pi * f * t2) * np.exp(-t2 / tau)
    saida[n:] += brilho * 0.55
    # 4) chiado agudo curto no impacto
    chiado = passa_alta(RNG.standard_normal(len(t2)), 3500) * np.exp(-t2 / 0.035) * 0.22
    saida[n:] += chiado
    # 5) ecos curtos
    saida = eco(saida, [(0.047, 0.30), (0.089, 0.20), (0.141, 0.11)])
    return fecha(saida, 0.72)


def sem_pe():
    dur = 0.42
    t = t_(dur)
    saida = np.zeros_like(t)

    def zumbido(inicio, f0, f1, tau, ganho):
        t2 = t_(dur - inicio)
        # duas serras levemente desafinadas (bate, soa "errado") descendo de f0 para f1
        freq = f1 + (f0 - f1) * np.exp(-t2 / 0.06)
        f_a = 2 * np.pi * np.cumsum(freq) / SR
        f_b = 2 * np.pi * np.cumsum(freq * 1.045) / SR
        serra = lambda ph: 2 * ((ph / (2 * np.pi)) % 1) - 1
        corpo = (serra(f_a) + serra(f_b)) * 0.5
        corpo = passa_baixa(corpo, 650)
        env = (1 - np.exp(-t2 / 0.004)) * np.exp(-t2 / tau)
        i = int(SR * inicio)
        saida[i:] += corpo * env * ganho

    zumbido(0.0, 175.0, 118.0, 0.075, 1.0)     # primeiro zumbido
    zumbido(0.15, 140.0, 84.0, 0.09, 0.9)      # segundo, mais grave (a "negativa")
    # estalo seco no inicio de cada zumbido
    for inicio in (0.0, 0.15):
        i = int(SR * inicio)
        n = int(SR * 0.012)
        clique = passa_alta(RNG.standard_normal(n), 1800) * np.exp(-np.arange(n) / (SR * 0.003)) * 0.25
        saida[i:i + n] += clique
    return fecha(saida, 0.62)


def salvar(nome, amostras):
    os.makedirs(SAIDA, exist_ok=True)
    destino = os.path.join(SAIDA, nome + ".ogg")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, nome + ".wav")
        wavfile.write(wav, SR, (amostras * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        destino], check=True)
    print(f"ok: {os.path.normpath(destino)} ({len(amostras) / SR:.2f} s)")


if __name__ == "__main__":
    salvar("usar", usar())
    salvar("sem_pe", sem_pe())
