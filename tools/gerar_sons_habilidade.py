#!/usr/bin/env python3
"""
Gera os sons da HUD de habilidades A PARTIR DE GRAVACOES que o mod ja tem (nada de ondas sinteticas puras):

  sounds/habilidade/usar_1..3.ogg    ao USAR uma habilidade (~0,6 s)
      batida de coracao (o golpe, no instante da tecla) + textura umida de "sangue" + sussurro como sopro que se solta
      logo depois, tudo numa sala curta. Fontes: marcado/batimento_loop, sangue, sussuros_1, sussuros_2.
  sounds/habilidade/sem_pe_1..3.ogg  quando NAO da para usar (falta PE) (~0,8 s)
      batimento duplo (lub-dub) mais grave e abafado, "o coracao falha", com um suspiro abafado logo depois.
      Fontes: marcado/batimento_loop, sussuros_1, sussuros_2.
  O golpe cai no tempo zero de proposito: som de interface precisa bater com a animacao no instante da tecla.

Cada evento tem 3 variacoes (cortes e afinacoes diferentes); o sounds.json lista as 3 e o jogo sorteia uma a cada vez,
entao nao soa igual toda hora. Para tirar uma que voce nao gostou, apague a linha dela no sounds.json.

Uso (na pasta do projeto):   python3 tools/gerar_sons_habilidade.py
Precisa de: numpy, scipy e ffmpeg (com libvorbis) no PATH. Os "botoes" estao nas tabelas VARIACOES_* abaixo.
Saida mono, 44,1 kHz, igual aos outros sons do mod.
"""
import os
import subprocess
import tempfile

import numpy as np
from scipy.io import wavfile
from scipy.signal import butter, find_peaks, lfilter, resample_poly

SR = 44100
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "ordemmod",
                    "sounds")
SAIDA = os.path.join(RAIZ, "habilidade")
RNG = np.random.default_rng(11)  # semente fixa: sai igual toda vez

# ------------------------------------------------------------------ botoes (uma linha por variacao)
# usar: (indice da batida, pitch da batida, indice do sussurro, pitch do sussurro, ganho da textura "sangue", pico final)
VARIACOES_USAR = [
    (0, 1.00, 0, 1.00, 0.30, 0.72),
    (2, 1.12, 1, 1.08, 0.24, 0.72),
    (4, 0.92, 2, 0.94, 0.34, 0.72),
]
# sem_pe: (indice do par lub-dub, pitch do par, indice do sussurro, pitch do sussurro, corte do passa-baixa em Hz, pico)
VARIACOES_SEM_PE = [
    (0, 0.82, 3, 0.92, 520.0, 0.62),
    (1, 0.76, 4, 0.86, 460.0, 0.62),
    (2, 0.88, 5, 0.97, 600.0, 0.62),
]


# ------------------------------------------------------------------ utilitarios
def ler(rel):
    caminho = os.path.join(RAIZ, rel)
    bruto = subprocess.run(["ffmpeg", "-v", "error", "-i", caminho, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                           capture_output=True, check=True).stdout
    return np.frombuffer(bruto, dtype=np.float32).astype(np.float64)


def passa_baixa(x, f, ordem=2):
    b, a = butter(ordem, f / (SR / 2))
    return lfilter(b, a, x)


def passa_alta(x, f, ordem=2):
    b, a = butter(ordem, f / (SR / 2), btype="high")
    return lfilter(b, a, x)


def passa_faixa(x, f1, f2):
    b, a = butter(2, [f1 / (SR / 2), f2 / (SR / 2)], btype="band")
    return lfilter(b, a, x)


def afina(x, razao):
    """Muda o tom (e a duracao) por reamostragem, como acelerar/desacelerar uma fita. razao > 1 = mais agudo."""
    if abs(razao - 1.0) < 1e-6:
        return x
    base = 1000
    return resample_poly(x, base, int(round(base * razao)))


def entrada(x, seg):
    n = min(len(x), int(SR * seg))
    x = x.copy()
    x[:n] *= np.sin(np.linspace(0, np.pi / 2, n)) ** 2
    return x


def saida(x, seg):
    n = min(len(x), int(SR * seg))
    x = x.copy()
    x[-n:] *= np.cos(np.linspace(0, np.pi / 2, n)) ** 2
    return x


def soma(base, parte, inicio, ganho=1.0):
    i = int(SR * inicio)
    fim = min(len(base), i + len(parte))
    if fim > i:
        base[i:fim] += parte[: fim - i] * ganho


def sala(x, wet=0.20, dur=0.30, tau=0.07, corte=3200):
    """Sala curta: convolucao com ruido que decai (parece um espaco pequeno, nao um eco de efeito)."""
    n = int(SR * dur)
    ir = RNG.standard_normal(n) * np.exp(-np.arange(n) / (SR * tau))
    ir = passa_baixa(ir, corte)
    ir /= np.sqrt((ir ** 2).sum())
    molhado = np.convolve(x, ir)[: len(x)]
    return x + wet * molhado * (np.std(x) / (np.std(molhado) + 1e-9))


def fecha(x, pico, fade=0.03):
    x = x - np.mean(x)
    x = x / (np.max(np.abs(x)) + 1e-9) * pico
    return saida(x, fade)


# ------------------------------------------------------------------ extracao das fontes
def batidas(x):
    """Posicoes (s) dos golpes do coracao em batimento_loop (lub e dub), em ordem."""
    env = np.convolve(np.abs(x), np.ones(int(SR * 0.006)) / int(SR * 0.006), "same")
    picos, _ = find_peaks(env, height=0.35 * env.max(), distance=int(SR * 0.12))
    return picos / SR


def corte_batida(x, t, dur=0.24):  # 0,24 s: o "dub" vem 0,27 s depois do "lub", nao entra
    i = int(SR * max(0.0, t - 0.008))
    seg = x[i: i + int(SR * dur)]
    return saida(entrada(seg, 0.004), 0.09)


def corte_par(x, t_lub, t_dub, cauda=0.18):
    i = int(SR * max(0.0, t_lub - 0.008))
    j = int(SR * (t_dub + cauda))
    return saida(entrada(x[i:j], 0.004), 0.14)


def janelas_sussurro(arquivos, dur=0.32, quantas=6):
    """Escolhe as janelas de sussurro com mais energia nas frequencias de sopro (2-9 kHz), espacadas entre si."""
    candidatas = []
    for k, rel in enumerate(arquivos):
        x = ler(rel)
        banda = passa_faixa(x, 2000, 9000)
        passo = int(SR * 0.05)
        n = int(SR * dur)
        for i in range(0, len(x) - n, passo):
            candidatas.append((float((banda[i:i + n] ** 2).mean()), k, i, i + n))
    candidatas.sort(reverse=True)
    escolhidas = []
    for energia, k, a, b in candidatas:
        if all(not (k == k2 and abs(a - a2) < int(SR * 0.6)) for _, k2, a2, _ in escolhidas):
            escolhidas.append((energia, k, a, b))
        if len(escolhidas) == quantas:
            break
    fontes = [ler(rel) for rel in arquivos]
    return [fontes[k][a:b] for _, k, a, b in escolhidas]


def janela_textura(x, dur=0.26):
    """A janela de 'sangue' com mais energia (a textura umida/granulada)."""
    n = int(SR * dur)
    passo = int(SR * 0.02)
    melhor = max(range(0, len(x) - n, passo), key=lambda i: float((x[i:i + n] ** 2).mean()))
    return x[melhor: melhor + n]


# ------------------------------------------------------------------ os dois sons
def monta_usar(batida, sopro, textura, ganho_textura, pico):
    # golpe no tempo zero; a textura umida cola nele; o sussurro (sopro) se solta logo depois, como um exalar
    total = np.zeros(int(SR * 0.85))
    soma(total, batida, 0.0, 1.0)
    tex = passa_baixa(passa_alta(textura, 500), 7000)
    tex = saida(entrada(tex, 0.01), 0.16)
    soma(total, tex, 0.012, ganho_textura)
    ar = passa_faixa(sopro, 400, 9000)
    ar = saida(entrada(ar, 0.04), 0.22)
    soma(total, ar, 0.03, 0.45)
    total = sala(total, wet=0.20)
    fim = len(batida) / SR + 0.34
    return fecha(total[: int(SR * max(fim, len(sopro) / SR + 0.34))], pico)


def monta_sem_pe(par, suspiro, corte, pico):
    # batimento duplo abafado no tempo zero; depois um suspiro que vai ficando abafado (o passa-baixa fecha)
    n = len(suspiro)
    varrido = np.zeros(n)
    blocos = 8
    for k in range(blocos):
        a, b = k * n // blocos, (k + 1) * n // blocos
        fc = 6500 * (1 - k / blocos) + 700 * (k / blocos)
        varrido[a:b] = passa_baixa(suspiro, fc)[a:b]
    varrido = saida(entrada(varrido, 0.03), 0.20)
    surdo = passa_baixa(par, corte, ordem=3) * 1.8      # abafado, "sem forca"
    total = np.zeros(int(SR * 1.2))
    soma(total, surdo, 0.0)
    soma(total, varrido, 0.10, 0.40)
    total = sala(total, wet=0.12, dur=0.22, tau=0.05, corte=1800)
    fim = max(len(surdo) / SR, 0.10 + n / SR) + 0.25
    return fecha(total[: int(SR * fim)], pico)


# ------------------------------------------------------------------ saida
def salvar(nome, amostras):
    os.makedirs(SAIDA, exist_ok=True)
    destino = os.path.join(SAIDA, nome + ".ogg")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, nome + ".wav")
        wavfile.write(wav, SR, (np.clip(amostras, -1, 1) * 32767).astype(np.int16))
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                        destino], check=True)
    print(f"ok: {os.path.normpath(destino)} ({len(amostras) / SR:.2f} s)")


def main():
    loop = ler("marcado/batimento_loop.ogg")
    tempos = batidas(loop)
    print(f"batidas encontradas: {len(tempos)} (a cada ~{np.median(np.diff(tempos)):.2f} s em media)")
    sussurros = janelas_sussurro(["marcado/sussuros_1.ogg", "marcado/sussuros_2.ogg"], quantas=6)
    textura = janela_textura(ler("marcado/sangue.ogg"))

    for n, (ib, pb, isu, ps, gt, pico) in enumerate(VARIACOES_USAR, 1):
        batida = afina(corte_batida(loop, tempos[ib % len(tempos)]), pb)
        sopro = afina(sussurros[isu % len(sussurros)], ps)
        salvar(f"usar_{n}", monta_usar(batida, sopro, textura, gt, pico))

    for n, (ip, pp, isu, ps, corte, pico) in enumerate(VARIACOES_SEM_PE, 1):
        k = (2 * ip) % (len(tempos) - 1)                      # par lub-dub consecutivo
        par = afina(corte_par(loop, tempos[k], tempos[k + 1]), pp)
        suspiro = afina(sussurros[isu % len(sussurros)], ps)
        salvar(f"sem_pe_{n}", monta_sem_pe(par, suspiro, corte, pico))


if __name__ == "__main__":
    main()
