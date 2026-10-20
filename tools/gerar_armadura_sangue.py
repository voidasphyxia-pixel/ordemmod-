#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Gera a Armadura de Sangue para o mod (GeckoLib 5, formato Bedrock):

  src/main/resources/assets/ordemmod/geckolib/models/entity/armadura_sangue.geo.json
  src/main/resources/assets/ordemmod/geckolib/animations/entity/armadura_sangue.animation.json
  src/main/resources/assets/ordemmod/textures/entity/armadura_sangue.png

E um PORTE da animacao feita no Blender (armadura_de_sangue.blend, 90 quadros a 24 fps): mesma logica de
placas, espinhos, filamentos e gotas, com os mesmos tempos. Nao e identico vertice a vertice (o Blender
usava malhas irregulares, aqui tudo e cubo, como no Minecraft).

Uso (na raiz do projeto):   python tools/gerar_armadura_sangue.py
Para so conferir sem gravar: python tools/gerar_armadura_sangue.py --preview
Depende de: numpy e Pillow.

SE ALGO FICAR AO CONTRARIO NO JOGO (braco subindo para dentro, placas vindo de dentro para fora...):
inverta o sinal correspondente em SINAL_ROT / SINAL_POS aqui embaixo e rode de novo. Nao precisa mexer em
mais nada. (As convencoes de sinal do Bedrock/GeckoLib para rotacao e posicao nao puderam ser conferidas no
jogo ao gerar estes arquivos.)
"""
import bisect
import json
import math
import os
import random
import sys

import numpy as np
from PIL import Image

# ------------------------------------------------------------------------------------------------
# CONFIG (os mesmos parametros do script do Blender, versao aprovada: placas finas)
# ------------------------------------------------------------------------------------------------
SEED = 7
ESPESSURA = 0.5            # 1.0 = placa grossa (primeira versao); 0.5 = a versao fina aprovada
LARGURA = 0.78
ESCALA_FILAMENTOS = 0.6    # espessura dos filamentos (0.6 = padrao)
COMPRIMENTO_ESPINHOS = 0.85
QUANTIDADE_GOTAS = 1.0
FPS = 24
FRAME_FIM = 90             # a animacao dura (90-1)/24 = 3,71 s

# Sinais das conversoes Blender -> arquivo do GeckoLib (veja o aviso no topo).
SINAL_ROT = (1, 1, 1)      # (x, y, z): multiplica os angulos escritos no arquivo
SINAL_POS = (-1, 1, 1)     # (x, y, z): o X de "position" nas animacoes Bedrock e invertido

NOME_ANIM = "animation.armadura_sangue.formar"
RAIZ = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(RAIZ, "src", "main", "resources", "assets", "ordemmod")

# Paleta de sangue: 8 texels na linha y=36 da textura (x = indice)
PALETA = ["#3b0509", "#4c070d", "#2a0306", "#5a0a10", "#7d0d14", "#931119", "#b5151d", "#d0222b"]
LINHA_PALETA = 36


# ------------------------------------------------------------------------------------------------
# Matematica de rotacao / conversao de eixos
# ------------------------------------------------------------------------------------------------
def rx(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[1, 0, 0], [0, c, -s], [0, s, c]])


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, 0, s], [0, 1, 0], [-s, 0, c]])


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return np.array([[c, -s, 0], [s, c, 0], [0, 0, 1]])


def euler_blender(v):
    """Euler XYZ do Blender (radianos) -> matriz."""
    return rz(v[2]) @ ry(v[1]) @ rx(v[0])


# Blender (x, y, z) com z para cima e a frente em -y  ->  Bedrock (x, y, z) com y para cima e a frente em -z
P = np.array([[1, 0, 0], [0, 0, 1], [0, 1, 0]], dtype=float)


def arquivo_de_matriz(Rb):
    """Matriz de rotacao (ja no sistema do Bedrock) -> [x, y, z] em graus, como vai no arquivo."""
    ay = -math.asin(max(-1.0, min(1.0, Rb[2, 0])))
    ax = math.atan2(Rb[2, 1], Rb[2, 2])
    az = math.atan2(Rb[1, 0], Rb[0, 0])
    return np.array([-math.degrees(ax) * SINAL_ROT[0],
                     -math.degrees(ay) * SINAL_ROT[1],
                     math.degrees(az) * SINAL_ROT[2]])


def rot_arquivo(R_blender):
    return arquivo_de_matriz(P @ R_blender @ P.T)


def rot_arquivo_euler(euler_rad):
    return rot_arquivo(euler_blender(euler_rad))


def pos_arquivo(offset_blender):
    o = P @ np.asarray(offset_blender, float)
    return np.array([o[0] * SINAL_POS[0], o[1] * SINAL_POS[1], o[2] * SINAL_POS[2]])


def r3(v):
    return [round(float(x), 3) for x in v]


# ------------------------------------------------------------------------------------------------
# Canais de keyframe (mesma ideia das curvas do Blender)
# ------------------------------------------------------------------------------------------------
def suave(tipo, t):
    if tipo == "LINEAR":
        return t
    if tipo == "QUAD":
        return 2 * t * t if t < 0.5 else 1 - ((-2 * t + 2) ** 2) / 2
    if tipo == "CUBIC":
        return 4 * t ** 3 if t < 0.5 else 1 - ((-2 * t + 2) ** 3) / 2
    if tipo == "BACK":
        c1 = 1.70158
        c3 = c1 + 1
        return 1 + c3 * (t - 1) ** 3 + c1 * (t - 1) ** 2
    return -(math.cos(math.pi * t) - 1) / 2   # SINE


class Canal:
    def __init__(self):
        self.k = []
        self._f = None

    def add(self, frame, valor, tipo="SINE"):
        self.k.append((float(frame), np.array(valor, dtype=float), tipo))
        self._f = None
        return self

    def __call__(self, f):
        if self._f is None:
            self.k.sort(key=lambda a: a[0])
            self._f = [a[0] for a in self.k]
        if f <= self._f[0]:
            return self.k[0][1].copy()
        if f >= self._f[-1]:
            return self.k[-1][1].copy()
        i = bisect.bisect_right(self._f, f) - 1
        f0, v0, tipo = self.k[i]
        f1, v1, _ = self.k[i + 1]
        if tipo == "CONST":
            return v0.copy()
        t = (f - f0) / (f1 - f0)
        return v0 + (v1 - v0) * suave(tipo, t)


def comprimir(amostras, tol):
    """amostras: [(frame, vetor)] -> remove os pontos que a interpolacao linear ja reproduz."""
    if len(amostras) <= 2:
        return amostras
    mantidos = [amostras[0]]
    ini = 0
    i = 2
    while i < len(amostras):
        f0, v0 = amostras[ini]
        f1, v1 = amostras[i]
        ok = True
        for j in range(ini + 1, i):
            fj, vj = amostras[j]
            t = (fj - f0) / (f1 - f0)
            if np.max(np.abs(v0 + (v1 - v0) * t - vj)) > tol:
                ok = False
                break
        if not ok:
            mantidos.append(amostras[i - 1])
            ini = i - 1
        i += 1
    mantidos.append(amostras[-1])
    return mantidos


def tempo(frame):
    return str(round((frame - 1) / FPS, 4))


def serie(fn, tol, inicial=None):
    """Amostra fn(frame) em todos os quadros e comprime. Devolve None se nunca sai do valor inicial."""
    amostras = [(f, np.asarray(fn(f), float)) for f in range(1, FRAME_FIM + 1)]
    if inicial is not None and all(np.max(np.abs(v - inicial)) < 1e-4 for _, v in amostras):
        return None
    return comprimir(amostras, tol)


# ------------------------------------------------------------------------------------------------
# Acumuladores do modelo e da animacao
# ------------------------------------------------------------------------------------------------
BONES = []
ANIM_OSSOS = {}
RAIO_X = []     # para o --preview: (nome, pai, pivo, cubos, rot_repouso)
RNG = random.Random(SEED)


def add_bone(nome, pai, pivo, cubos=None, rot=None):
    b = {"name": nome}
    if pai:
        b["parent"] = pai
    b["pivot"] = r3(pivo)
    if rot is not None and np.max(np.abs(rot)) > 1e-4:
        b["rotation"] = r3(rot)
    if cubos:
        b["cubes"] = cubos
    BONES.append(b)
    RAIO_X.append((nome, pai, np.array(pivo, float), cubos or [],
                   np.array(rot, float) if rot is not None else np.zeros(3)))


def add_anim(nome, canal, serie_, formato):
    if serie_ is None:
        return
    ossos = ANIM_OSSOS.setdefault(nome, {})
    ossos[canal] = {tempo(f): formato(v) for f, v in serie_}


def cubo_cor(origem, tamanho, faces):
    """Cubo de cor lisa: cada face aponta para UM texel da paleta (indice)."""
    uv = {}
    for nome in ("north", "east", "south", "west", "up", "down"):
        i = faces[nome] if isinstance(faces, dict) else faces
        uv[nome] = {"uv": [i, LINHA_PALETA], "uv_size": [1, 1]}
    return {"origin": r3(origem), "size": r3(tamanho), "uv": uv}


# ------------------------------------------------------------------------------------------------
# Steve (base)
# ------------------------------------------------------------------------------------------------
PIVO = {"arm": (np.array([6.0, 22.0, 0.0]), "left_arm"),    # o braco que o Blender chamava de ARM_R
        "tor": (np.array([0.0, 12.0, 0.0]), "body")}


def criar_steve():
    add_bone("root", None, [0, 0, 0])
    add_bone("body", "root", [0, 12, 0], [{"origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16]}])
    add_bone("head", "body", [0, 24, 0], [{"origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0]}])
    add_bone("left_arm", "body", [6, 22, 0], [{"origin": [4, 12, -2], "size": [4, 12, 4], "uv": [32, 48]}])
    add_bone("right_arm", "body", [-6, 22, 0], [{"origin": [-8, 12, -2], "size": [4, 12, 4], "uv": [40, 16]}])
    add_bone("left_leg", "root", [2, 12, 0], [{"origin": [0, 0, -2], "size": [4, 12, 4], "uv": [16, 48]}])
    add_bone("right_leg", "root", [-2, 12, 0], [{"origin": [-4, 0, -2], "size": [4, 12, 4], "uv": [0, 16]}])


# ------------------------------------------------------------------------------------------------
# Animacao do corpo (mesmos keyframes do Blender; angulos em graus, Euler XYZ do Blender)
# ------------------------------------------------------------------------------------------------
def d2r(v):
    return np.radians(np.array(v, float))


CORPO = {}
CORPO["torso"] = Canal()
for f, a, b in ((1, 0, 0), (7, 0, 0), (15, 8.5, -1), (30, 10, -2.5), (60, 10.5, -2.5), (63, 12.5, -2.5),
                (70, 9.5, -2.2), (90, 9.5, -2.2)):
    CORPO["torso"].add(f, d2r((a, b, 0)))
CORPO["head"] = Canal()
for f, a, c in ((1, 0, 0), (9, 0, 0), (16, 10, 0), (30, 9, 8), (60, 8, 11), (63, 13, 11), (71, 8.5, 12), (90, 8.5, 12)):
    CORPO["head"].add(f, d2r((a, 0, c)))
CORPO["armR"] = Canal()          # o braco que recebe a armadura (bone left_arm no Bedrock)
CORPO["armR"].add(1, d2r((0, 0, 0))).add(8, d2r((0, 0, 0))).add(12, d2r((0, -2.5, 0))).add(16, d2r((-1, -5, 0)), "CUBIC")
CORPO["armR"].add(30, d2r((-10, -64, 0))).add(60, d2r((-10, -65.5, 0))).add(63, d2r((-11, -68, 0)))
CORPO["armR"].add(70, d2r((-10, -64, 0))).add(90, d2r((-10, -64, 0)))
CORPO["armL"] = Canal()          # o outro braco (right_arm)
for f, a, b in ((1, 0, 0), (16, -4, 2), (30, -6, 4), (90, -6, 4)):
    CORPO["armL"].add(f, d2r((a, b, 0)))
CORPO["root"] = Canal()
CORPO["root"].add(1, (0, 0, 0)).add(30, (-0.24, 0, 0)).add(90, (-0.24, 0, 0))   # px


def fk(f):
    """Posicao dos pivos no mundo (px, eixos do Blender) no quadro f: (offset da raiz, R do tronco, R do braco)."""
    Rt = euler_blender(CORPO["torso"](f))
    Ra = euler_blender(CORPO["armR"](f))
    return CORPO["root"](f), Rt, Ra


def mundo(f, tipo, local):
    off, Rt, Ra = fk(f)
    local = np.asarray(local, float)
    if tipo == "arm":
        return off + np.array([0, 0, 12.0]) + Rt @ (np.array([6.0, 0, 10.0]) + Ra @ local)
    return off + np.array([0, 0, 12.0]) + Rt @ local


def animar_corpo():
    nomes = {"torso": "body", "head": "head", "armR": "left_arm", "armL": "right_arm"}
    for chave, osso in nomes.items():
        s = serie(lambda f, c=chave: rot_arquivo_euler(CORPO[c](f)), 0.15, np.zeros(3))
        add_anim(osso, "rotation", s, r3)
    s = serie(lambda f: pos_arquivo(CORPO["root"](f)), 0.01, np.zeros(3))
    add_anim("root", "position", s, r3)


# ------------------------------------------------------------------------------------------------
# Placas
# ------------------------------------------------------------------------------------------------
PLACAS_MUNDO = []   # (tipo, centro local) para as gotas de impacto


def placa(nome, tipo, centro, tam, f0, dur, rot_z=0.0, extra=(0, 0, 0), underlay=False):
    pivo, pai = PIVO[tipo]
    tam = list(map(float, tam))
    c = list(map(float, centro))
    # --- afinar (o que o _slim do Blender fazia): o eixo mais fino encolhe para o lado de dentro
    idx = min(range(3), key=lambda i: tam[i])
    ns = [t * (0.85 if underlay else LARGURA) for t in tam]
    ns[idx] = tam[idx] * (0.6 if underlay else ESPESSURA)
    if tipo == "arm":
        ref = [0.0, 0.0, min(max(c[2], -10.0), 2.0)]
    else:
        ref = [min(max(c[0], -4.0), 4.0), 0.0, min(max(c[2], 0.0), 12.0)]
    sgn = 1.0 if c[idx] - ref[idx] >= 0 else -1.0
    c[idx] -= sgn * (tam[idx] - ns[idx]) / 2.0
    tam = ns
    c = np.array(c)
    # --- movimento de entrada
    rest = np.radians([extra[0], extra[1], extra[2] + rot_z])
    saida = np.array([c[0], c[1], 0.0])
    saida = saida / np.linalg.norm(saida) if np.linalg.norm(saida) > 0.01 else np.array([1.0, 0, 0])
    off = saida * RNG.uniform(5, 9) + np.array([RNG.uniform(-2, 2), RNG.uniform(-2, 2), RNG.uniform(-2, 3)])
    if underlay:
        off = off * 0.0
    s_loc = c + off
    if underlay:
        s_rot = rest.copy()
    else:
        s_rot = rest + np.radians([RNG.uniform(-35, 35), RNG.uniform(-35, 35), RNG.uniform(-35, 35)])
    mid = c + off * 0.15
    mid_rot = rest + np.radians([RNG.uniform(-6, 6), RNG.uniform(-6, 6), RNG.uniform(-6, 6)])
    pulso = max(f0 + dur + 2, 64) + RNG.randint(0, 4)

    loc = Canal().add(1, s_loc, "CONST").add(f0, s_loc, "QUAD").add(f0 + dur * 0.62, mid, "BACK").add(f0 + dur, c, "CUBIC")
    rot = Canal().add(1, s_rot, "CONST").add(f0, s_rot, "QUAD").add(f0 + dur * 0.62, mid_rot, "BACK").add(f0 + dur, rest, "CUBIC")
    esc = Canal().add(1, (0, 0, 0), "CONST").add(f0, (0.04,) * 3, "QUAD").add(f0 + dur * 0.62, (1.16,) * 3, "BACK")
    esc.add(f0 + dur, (1, 1, 1), "CUBIC").add(pulso, (1, 1, 1)).add(pulso + 3, (0.93,) * 3).add(pulso + 7, (1.025,) * 3)
    esc.add(pulso + 11, (1, 1, 1))

    centro_bed = pivo + P @ c
    tam_bed = P @ np.array(tam)
    tam_bed = np.abs(tam_bed)
    origem = centro_bed - tam_bed / 2.0
    faces = {n: RNG.choice([0, 1, 2, 3]) for n in ("north", "east", "south", "west", "down")}
    faces["up"] = RNG.choice([3, 4, 5])
    if underlay:
        faces = 2
    nome = nome.lower()
    rest_arq = rot_arquivo_euler(rest)
    add_bone(nome, pai, centro_bed, [cubo_cor(origem, tam_bed, faces)], rest_arq)
    add_anim(nome, "position", serie(lambda f: pos_arquivo(loc(f) - c), 0.04, np.zeros(3)), r3)
    add_anim(nome, "rotation", serie(lambda f: rot_arquivo_euler(rot(f)) - rest_arq, 0.5, np.zeros(3)), r3)
    add_anim(nome, "scale", serie(lambda f: esc(f), 0.01, np.ones(3)), r3)
    if not underlay:
        PLACAS_MUNDO.append((tipo, c))


def criar_placas():
    for n, t, c, s, f0, du in (("UL_ARM", "arm", (0, 0, -4), (4.8, 4.8, 12.6), 24, 16),
                              ("UL_SHOULDER", "arm", (2.2, 0, 3.0), (7.2, 6.2, 5.0), 36, 14),
                              ("UL_CHEST", "tor", (2.0, -2.3, 8.5), (8.5, 1.6, 7.5), 41, 16),
                              ("UL_BACK", "tor", (2.2, 2.3, 9.0), (8.0, 1.6, 7.0), 47, 14),
                              ("UL_NECK", "tor", (2.5, 0, 12.2), (5.5, 4.4, 2.0), 52, 10)):
        placa(n, t, c, s, f0, du, underlay=True)

    # antebraco / mao: camadas de placas ao redor do braco
    camadas = [(-9.0, 23), (-6.4, 27), (-3.9, 31), (-1.3, 34), (1.0, 37)]
    for li, (zc, f0) in enumerate(camadas):
        h = 2.7 if li < 3 else 2.4
        lados = [((2.9, 0, zc), (1.8, 4.6, h), "XP"), ((-2.9, 0, zc), (1.8, 4.6, h), "XN"),
                 ((0, -2.9, zc), (4.6, 1.8, h), "YN"), ((0, 2.9, zc), (4.6, 1.8, h), "YP")]
        for c, s, tag in lados:
            m = RNG.uniform(0.9, 1.25)
            cc = (c[0] + RNG.uniform(-0.5, 0.5), c[1] + RNG.uniform(-0.5, 0.5), c[2] + RNG.uniform(-0.4, 0.4))
            ss = (s[0] * (m if tag.startswith("Y") else 1), s[1] * (m if tag.startswith("X") else 1),
                  s[2] * RNG.uniform(0.9, 1.2))
            placa("PLATE_ARM_L%d_%s" % (li, tag), "arm", cc, ss, f0 + RNG.randint(0, 4), RNG.randint(10, 14),
                  rot_z=RNG.uniform(-8, 8))
    for n, c, s, f0, du, rz in (("PLATE_FOREARM_OUTER", (3.5, 0.3, -6.5), (2.6, 4.8, 4.2), 29, 13, 5),
                                ("PLATE_ARM_OUTER", (3.6, -0.4, -2.0), (2.8, 5.0, 4.0), 36, 13, -6),
                                ("PLATE_ELBOW", (3.4, 0.0, -4.6), (3.0, 5.2, 3.4), 31, 13, 8)):
        placa(n, "arm", c, s, f0, du, rot_z=rz)
    # ombro
    for n, c, s, f0, du, rz, er in (
            ("PLATE_SHOULDER_1", (2.2, 0, 3.6), (6.5, 6.5, 2.6), 36, 14, 0, (0, 0, 0)),
            ("PLATE_SHOULDER_2", (3.9, 0.3, 5.2), (5.0, 5.5, 2.2), 40, 14, 15, (0, -10, 0)),
            ("PLATE_SHOULDER_3", (4.9, -0.5, 2.0), (3.0, 6.0, 4.0), 41, 13, -6, (0, 0, 0)),
            ("PLATE_SHOULDER_BACK", (0.5, 2.8, 3.8), (4.5, 2.2, 2.4), 44, 12, 6, (0, 0, 0)),
            ("PLATE_SHOULDER_FRONT", (0.5, -2.8, 3.6), (4.5, 2.2, 2.4), 44, 12, -5, (0, 0, 0)),
            ("PLATE_SHOULDER_TIP_A", (5.0, -1.0, 6.5), (2.5, 2.5, 3.0), 48, 11, 10, (0, -12, 0)),
            ("PLATE_SHOULDER_TIP_B", (2.6, 1.5, 6.2), (2.6, 2.4, 2.6), 50, 11, -12, (0, 8, 0))):
        placa(n, "arm", c, s, f0, du, rot_z=rz, extra=er)
    # peito / costas / pescoco
    for n, c, s, f0, du, rz in (
            ("PLATE_CHEST_1", (3.2, -2.7, 9.3), (4.5, 1.8, 3.5), 41, 14, 0),
            ("PLATE_CHEST_2", (0.8, -2.8, 10.6), (4.2, 2.0, 3.0), 47, 13, 4),
            ("PLATE_CHEST_3", (3.6, -2.8, 6.2), (3.5, 1.8, 3.4), 51, 12, -5),
            ("PLATE_CHEST_4", (-1.6, -2.6, 9.0), (3.0, 1.6, 3.0), 55, 11, 6),
            ("PLATE_CHEST_5", (1.2, -2.6, 6.8), (3.0, 1.6, 2.8), 57, 10, -4),
            ("PLATE_COLLAR_1", (2.0, -1.0, 12.8), (4.0, 3.5, 1.8), 52, 11, 0),
            ("PLATE_COLLAR_2", (3.2, 0.5, 12.4), (3.0, 3.0, 1.6), 55, 10, 8),
            ("PLATE_NECK", (0.0, -1.2, 12.6), (3.0, 3.0, 1.4), 59, 9, -6),
            ("PLATE_BACK_1", (3.0, 2.8, 9.5), (4.5, 1.8, 3.8), 48, 13, 3),
            ("PLATE_BACK_2", (1.0, 2.8, 10.8), (3.4, 1.6, 3.0), 53, 12, -4),
            ("PLATE_BACK_3", (3.8, 2.6, 6.8), (3.0, 1.6, 3.0), 56, 11, 5)):
        placa(n, "tor", c, s, f0, du, rot_z=rz)


# ------------------------------------------------------------------------------------------------
# Espinhos (piramide de 3 cubos empilhados)
# ------------------------------------------------------------------------------------------------
ESPINHOS_MUNDO = []


def matriz_para_direcao(d):
    z = np.asarray(d, float)
    z = z / np.linalg.norm(z)
    up = np.array([0.0, 0.0, 1.0])
    if np.linalg.norm(np.cross(up, z)) < 1e-6:
        up = np.array([0.0, 1.0, 0.0])
    x = np.cross(up, z)
    x /= np.linalg.norm(x)
    y = np.cross(z, x)
    return np.column_stack([x, y, z])


def espinho(nome, tipo, pos, direcao, comp, tam, f0, dur):
    pivo, pai = PIVO[tipo]
    comp = comp * COMPRIMENTO_ESPINHOS
    R = matriz_para_direcao(direcao)
    rest_arq = rot_arquivo(R)
    base = pivo + P @ np.array(pos, float)
    w = max(tam * 1.02, 0.9)
    cubos = []
    alt = comp / 3.0
    for i, fator in enumerate((1.0, 0.66, 0.34)):
        wi = max(w * fator, 0.5)
        cubos.append(cubo_cor((base[0] - wi / 2, base[1] + i * alt, base[2] - wi / 2), (wi, alt, wi),
                              [2, 4, 5][i] if i < 3 else 5))
    pulso = max(f0 + dur + 2, 66) + RNG.randint(0, 3)
    esc = Canal().add(1, (0, 0, 0), "CONST").add(f0, (0.5, 0.5, 0.03), "QUAD").add(f0 + dur * 0.7, (1.12, 1.12, 1.4), "BACK")
    esc.add(f0 + dur, (1, 1, 1), "CUBIC").add(pulso, (1, 1, 1)).add(pulso + 3, (1.04, 1.04, 0.9)).add(pulso + 8, (1, 1, 1))
    nome = nome.lower()
    add_bone(nome, pai, base, cubos, rest_arq)
    # escala do Blender (x, y, z local) -> Bedrock (x, z, y)
    add_anim(nome, "scale", serie(lambda f: np.array([esc(f)[0], esc(f)[2], esc(f)[1]]), 0.01, np.ones(3)), r3)
    ESPINHOS_MUNDO.append((tipo, np.array(pos, float)))


def criar_espinhos():
    k = RNG
    for n, t, p, d, ln, sz, f0 in (
            ("SPIKE_ELBOW_OUT", "arm", (3.8, 0, -4.6), (1, 0.2, 0.1), 5.0, 1.6, 38),
            ("SPIKE_ELBOW_BACK", "arm", (2.2, 2.6, -4.5), (0.3, 1, 0.2), 3.5, 1.2, 40),
            ("SPIKE_FOREARM", "arm", (3.0, -1, -8), (1, -0.3, -0.2), 3.2, 1.2, 33),
            ("SPIKE_UPPERARM", "arm", (3.4, 0.8, -0.5), (1, 0.4, 0.3), 4.0, 1.4, 42),
            ("SPIKE_SHOULDER_TOP_A", "arm", (5.0, -1, 7.5), (0.4, -0.3, 1), 6.0, 2.0, 50),
            ("SPIKE_SHOULDER_TOP_B", "arm", (3.0, 1.5, 7.2), (-0.1, 0.5, 1), 4.5, 1.6, 52),
            ("SPIKE_SHOULDER_OUT", "arm", (6.2, 0.2, 3.8), (1, 0, 0.4), 5.0, 1.8, 48),
            ("SPIKE_SHOULDER_REAR_A", "arm", (4.8, 2.6, 5.0), (0.5, 1, 0.4), 3.5, 1.3, 54),
            ("SPIKE_SHOULDER_REAR_B", "arm", (1.0, 3.6, 4.4), (0, 1, 0.5), 3.5, 1.3, 50),
            ("SPIKE_CHEST_A", "tor", (3.5, -3.6, 10), (0.4, -1, 0.4), 3.5, 1.3, 56),
            ("SPIKE_CHEST_B", "tor", (4.3, -2.8, 7), (1, -0.4, 0.1), 3.0, 1.2, 58),
            ("SPIKE_COLLAR", "tor", (3.4, -0.5, 13.4), (0.4, 0, 1), 3.0, 1.3, 58),
            ("SPIKE_BACK_A", "tor", (4.0, 3.5, 9.5), (0.5, 1, 0.5), 4.0, 1.5, 55),
            ("SPIKE_BACK_B", "tor", (1.4, 3.6, 11.2), (0, 1, 0.6), 3.0, 1.1, 58)):
        espinho(n, t, p, d, ln, sz, f0, k.randint(8, 11))


# ------------------------------------------------------------------------------------------------
# Filamentos (cadeia de cubos; cada segmento cresce e depois e absorvido, em ordem)
# ------------------------------------------------------------------------------------------------
def catmull(pts, n):
    pts = [np.array(p, float) for p in pts]
    ext = [pts[0] * 2 - pts[1]] + pts + [pts[-1] * 2 - pts[-2]]
    out = []
    por = max(1, n // (len(pts) - 1))
    for i in range(1, len(ext) - 2):
        p0, p1, p2, p3 = ext[i - 1], ext[i], ext[i + 1], ext[i + 2]
        for j in range(por):
            t = j / por
            out.append(0.5 * ((2 * p1) + (-p0 + p2) * t + (2 * p0 - 5 * p1 + 4 * p2 - p3) * t * t
                              + (-p0 + 3 * p1 - 3 * p2 + p3) * t ** 3))
    out.append(pts[-1])
    return out


CONT_FIL = [0]


def filamento(tipo, pts, f0, grow, f_abs, abs_dur, espessura):
    pivo, pai = PIVO[tipo]
    idx = CONT_FIL[0]
    CONT_FIL[0] += 1
    th = round(espessura * (ESCALA_FILAMENTOS / 0.6), 2)
    amostras = catmull(pts, 6)
    seg = len(amostras) - 1
    for i in range(seg):
        A = pivo + P @ amostras[i]
        B = pivo + P @ amostras[i + 1]
        d = B - A
        eixo = int(np.argmax(np.abs(d)))
        tam = [th, th, th]
        tam[eixo] = abs(d[eixo]) + th
        centro = (A + B) / 2.0
        origem = centro - np.array(tam) / 2.0
        p = (i + 0.5) / seg
        fg = f0 + grow * p
        fa = f_abs + abs_dur * p
        esc = Canal().add(1, (0, 0, 0), "CONST").add(fg, (0, 0, 0), "LINEAR").add(fg + 2, (1, 1, 1), "LINEAR")
        esc.add(fa, (1, 1, 1), "LINEAR").add(fa + 3, (0, 0, 0), "LINEAR")
        nome = "fil_%02d_%d" % (idx, i)
        add_bone(nome, pai, centro, [cubo_cor(origem, tam, [6, 7][i % 2])])
        add_anim(nome, "scale", serie(lambda f: esc(f), 0.01, np.ones(3)), r3)


def criar_filamentos():
    R = RNG
    for i in range(14):
        f0 = 22 + i * 1.2
        a0 = R.uniform(0, 6.28)
        z0 = R.uniform(-11, -6)
        z1 = R.uniform(-3, 2)
        pts = []
        n = 6
        for j in range(n):
            t = j / (n - 1)
            r = 9.0 * (1 - t) ** 1.5 + 3.4 * t + R.uniform(-0.3, 0.3)
            a = a0 + t * R.uniform(3.0, 5.0)
            pts.append((r * math.cos(a), r * math.sin(a), z0 + (z1 - z0) * t + R.uniform(-0.8, 0.8)))
        filamento("arm", pts, f0, R.randint(9, 13), 38 + i * 1.5, 9, R.uniform(0.8, 1.2))
    for i in range(8):
        f0 = 36 + i * 2
        sx = R.uniform(7.5, 9.5)
        pts = [(sx, R.uniform(-1.5, 1.5), 11.5 + R.uniform(-1, 1)),
               (sx - 2, R.uniform(-3.8, -2.2), 11.0 + R.uniform(-1, 1)),
               (5 - R.uniform(0, 2), -3.6 + R.uniform(-0.4, 0.4), 9.8 + R.uniform(-1.5, 1.5)),
               (2 - R.uniform(0, 2), -3.4 + R.uniform(-0.4, 0.4), 8.0 + R.uniform(-2, 2)),
               (-0.5 - R.uniform(0, 2), -3.2, 7.0 + R.uniform(-2, 2))]
        filamento("tor", pts, f0, R.randint(10, 14), 54 + i * 1.2, 9, R.uniform(0.8, 1.2))
    for i in range(4):
        f0 = 44 + i * 3
        pts = [(8.2, R.uniform(-1, 1), 11.0), (6.5, 3.4, 10.5 + R.uniform(-1, 1)),
               (3.5, 3.7, 9.5 + R.uniform(-2, 1)), (1.0, 3.5, 8.0 + R.uniform(-2, 2))]
        filamento("tor", pts, f0, R.randint(10, 13), 58 + i, 9, R.uniform(0.8, 1.2))
    for i in range(3):
        f0 = 50 + i * 3
        pts = [(7.5, R.uniform(-1, 1), 12.0), (5.0, -2.5 + R.uniform(-1, 1), 13.0 + R.uniform(0, 1)),
               (2.6, -2.6, 13.4 + R.uniform(0, 1.2)), (0.6, -2.2, 13.0 + R.uniform(0, 1))]
        filamento("tor", pts, f0, R.randint(9, 12), 62 + i, 8, R.uniform(0.8, 1.2))


# ------------------------------------------------------------------------------------------------
# Gotas (posicoes no mundo, igual ao Blender)
# ------------------------------------------------------------------------------------------------
CONT_GOTA = [0]


def gota(prefixo, tam, chaves, tipo):
    """chaves: [(frame, posicao_mundo_px_Blender)]. A gota nasce, viaja e some."""
    idx = CONT_GOTA[0]
    CONT_GOTA[0] += 1
    f_ini, f_fim = chaves[0][0], chaves[-1][0]
    pos = Canal()
    pos.add(1, chaves[0][1], "CONST")
    for f, p in chaves:
        pos.add(f, p, tipo)
    esc = Canal().add(1, (0, 0, 0), "CONST").add(f_ini, (0.05,) * 3, "QUAD").add(f_ini + 2, (1, 1, 1), "QUAD")
    esc.add(max(f_ini + 3, f_fim - 3), (1, 1, 1), "QUAD").add(f_fim, (0, 0, 0), "QUAD")
    tam_y = tam * RNG.uniform(0.8, 1.4)
    nome = "%s_%02d" % (prefixo, idx)
    add_bone(nome, "root", [0, 0, 0], [cubo_cor((-tam / 2, -tam_y / 2, -tam / 2), (tam, tam_y, tam), 7)])
    add_anim(nome, "position", serie(lambda f: pos_arquivo(pos(f)), 0.04, np.zeros(3)), r3)
    add_anim(nome, "scale", serie(lambda f: esc(f), 0.01, np.ones(3)), r3)


def criar_gotas():
    R = RNG
    Q = QUANTIDADE_GOTAS
    W = mundo
    # 1) sementes aderindo ao antebraco
    for i in range(int(14 * Q)):
        fs = 20 + R.uniform(0, 10)
        a = R.uniform(0, 6.28)
        z = R.uniform(-10, -3)
        ks = [(fs, W(fs, "arm", (4.2 * math.cos(a), 4.2 * math.sin(a), z))),
              (fs + 6, W(fs + 6, "arm", (3.0 * math.cos(a), 3.0 * math.sin(a), z))),
              (fs + 13, W(fs + 13, "arm", (2.4 * math.cos(a), 2.4 * math.sin(a), z - 0.8)))]
        gota("drop_seed", R.uniform(0.5, 0.9), ks, "QUAD")
    # 2) puxadas contra a gravidade para dentro da armadura
    for i in range(int(24 * Q)):
        fs = 24 + R.uniform(0, 34)
        if R.random() < 0.6:
            tp = "arm"
            a = R.uniform(0, 6.28)
            z = R.uniform(-10, 5)
            alvo = (3.2 * math.cos(a), 3.2 * math.sin(a), z)
            fora = np.array([math.cos(a), math.sin(a), 0.2])
        else:
            tp = "tor"
            alvo = (R.uniform(-1, 6), R.choice((-3.2, 3.2)), R.uniform(5, 12.5))
            fora = np.array([0.3, 1 if alvo[1] > 0 else -1, 0.3])
        ini = (alvo[0] + fora[0] * R.uniform(9, 15), alvo[1] + fora[1] * R.uniform(9, 15), alvo[2] + fora[2] * 9)
        meio = ((ini[0] + alvo[0]) / 2, (ini[1] + alvo[1]) / 2, max(ini[2], alvo[2]) + R.uniform(2, 5))
        dur = R.randint(10, 15)
        ks = [(fs, W(fs, tp, ini)), (fs + dur // 2, W(fs + dur // 2, tp, meio)), (fs + dur, W(fs + dur, tp, alvo))]
        gota("drop_pull", R.uniform(0.6, 1.3), ks, "QUAD")
    # 3) lancadas no impacto final
    alvos = [mundo(62, t, c) for t, c in PLACAS_MUNDO + ESPINHOS_MUNDO]
    centro = np.array([0.12, 0, 1.28]) * 16.0
    g = -5.5 * 16.0
    for i in range(int(26 * Q)):
        o = R.choice(alvos)
        f0 = R.randint(61, 66)
        dur = R.randint(12, 18)
        v = o - centro
        v[2] = 0
        v = v / np.linalg.norm(v) if np.linalg.norm(v) > 0.01 else np.array([1.0, 0, 0])
        vel = (v * R.uniform(0.5, 1.5) + np.array([R.uniform(-0.4, 0.4), R.uniform(-0.4, 0.4), R.uniform(0.5, 1.6)])) * 16.0
        ks = []
        for s in range(0, dur + 1, 2):
            t = s / 24.0
            ks.append((f0 + s, o + vel * t + np.array([0, 0, 0.5 * g * t * t])))
        gota("drop_burst", R.uniform(0.8, 1.7), ks, "LINEAR")
    # 4) gotejar lento depois de estabilizar
    for i in range(int(6 * Q)):
        fs = R.randint(66, 74)
        p = R.choice(((3.0, R.uniform(-1.5, 1.5), -9.8), (3.4, 0.5, -4.8), (2.6, R.uniform(-2, 2), -10.2)))
        p0 = W(fs, "arm", p)
        ks = [(fs, p0), (fs + 8, p0 + np.array([0, 0, -0.8])), (fs + 18, p0 + np.array([0, 0, -4.2]))]
        gota("drop_drip", R.uniform(0.9, 1.4), ks, "QUAD")


# ------------------------------------------------------------------------------------------------
# Textura 64x64: Steve de cores lisas (substitua pela skin que quiser, mesmo layout) + paleta de sangue
# ------------------------------------------------------------------------------------------------
def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def gerar_textura(caminho):
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    px = img.load()
    PELE, PELE2 = (190, 133, 104, 255), (150, 100, 78, 255)
    CABELO = (70, 44, 24, 255)
    CAMISA, CAMISA2 = (0, 160, 168, 255), (0, 130, 138, 255)
    CALCA, SAPATO = (62, 70, 160, 255), (105, 105, 110, 255)
    BRANCO, OLHO = (255, 255, 255, 255), (78, 60, 150, 255)
    BOCA = (120, 70, 60, 255)

    def regiao(x0, y0, w, h, fn):
        for y in range(h):
            for x in range(w):
                px[x0 + x, y0 + y] = fn(x, y)

    def caixa(u, v, w, h, d, fn):
        regiao(u + d, v, w, d, lambda x, y: fn("up", x, y, w, d))
        regiao(u + d + w, v, w, d, lambda x, y: fn("down", x, y, w, d))
        regiao(u, v + d, d, h, lambda x, y: fn("west", x, y, d, h))
        regiao(u + d, v + d, w, h, lambda x, y: fn("north", x, y, w, h))
        regiao(u + d + w, v + d, d, h, lambda x, y: fn("east", x, y, d, h))
        regiao(u + 2 * d + w, v + d, w, h, lambda x, y: fn("south", x, y, w, h))

    def cabeca(face, x, y, w, h):
        if face == "up" or face == "south":
            return CABELO
        if face in ("west", "east"):
            return CABELO if y < 2 or (face == "east" and False) else PELE
        if face == "down":
            return PELE2
        if y < 2:
            return CABELO
        if y == 2 and (x < 1 or x > 6):
            return CABELO
        if y == 3 and (x in (1, 2, 5, 6)):
            return CABELO
        if y == 4 and x in (1, 6):
            return BRANCO
        if y == 4 and x in (2, 5):
            return OLHO
        if y == 5 and x in (3, 4):
            return PELE2
        if y == 6 and x in (2, 3, 4, 5):
            return BOCA
        return PELE

    def tronco(face, x, y, w, h):
        return CAMISA2 if face in ("down", "up") else CAMISA

    def braco(face, x, y, w, h):
        if face == "up":
            return CAMISA
        if face == "down":
            return PELE2
        return CAMISA if y < 4 else PELE

    def perna(face, x, y, w, h):
        if face in ("up", "down"):
            return CALCA
        return SAPATO if y >= 10 else CALCA

    caixa(0, 0, 8, 8, 8, cabeca)
    caixa(16, 16, 8, 12, 4, tronco)
    caixa(40, 16, 4, 12, 4, braco)
    caixa(32, 48, 4, 12, 4, braco)
    caixa(0, 16, 4, 12, 4, perna)
    caixa(16, 48, 4, 12, 4, perna)
    for i, h in enumerate(PALETA):
        px[i, LINHA_PALETA] = hexrgb(h)
    img.save(caminho)


# ------------------------------------------------------------------------------------------------
# Saida
# ------------------------------------------------------------------------------------------------
def montar():
    criar_steve()
    animar_corpo()
    criar_placas()
    criar_espinhos()
    criar_filamentos()
    criar_gotas()
    geo = {
        "format_version": "1.12.0",
        "minecraft:geometry": [{
            "description": {
                "identifier": "geometry.armadura_sangue",
                "texture_width": 64, "texture_height": 64,
                "visible_bounds_width": 5, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1, 0],
            },
            "bones": BONES,
        }],
    }
    comprimento = round((FRAME_FIM - 1) / FPS, 4)
    anim = {"format_version": "1.8.0",
            "animations": {NOME_ANIM: {"animation_length": comprimento, "bones": ANIM_OSSOS}}}
    return geo, anim


def preview(caminho):
    """Desenho aproximado do ESTADO FINAL (so confere o modelo; o jogo e quem manda na aparencia)."""
    from PIL import ImageDraw
    pos = {}
    for nome, pai, pivo, cubos, rot in RAIO_X:
        pos[nome] = (pai, pivo, cubos, rot)
    final = {}
    for nome, canais in ANIM_OSSOS.items():
        final[nome] = {c: np.array(v[max(v, key=float)], float) for c, v in canais.items()}

    def Rarq(a):
        ax, ay, az = a[0] * SINAL_ROT[0], a[1] * SINAL_ROT[1], a[2] * SINAL_ROT[2]
        return rz(math.radians(az)) @ ry(math.radians(-ay * SINAL_ROT[1] * SINAL_ROT[1])) @ rx(math.radians(-ax))

    def Rarquivo(a):
        # inverso de arquivo_de_matriz: file = (-deg ax, -deg ay, deg az) * SINAL
        ax = -a[0] * SINAL_ROT[0]
        ay = -a[1] * SINAL_ROT[1]
        az = a[2] * SINAL_ROT[2]
        return rz(math.radians(az)) @ ry(math.radians(ay)) @ rx(math.radians(ax))

    cache = {}

    def mat(nome):
        if nome in cache:
            return cache[nome]
        pai, pivo, _, rot = pos[nome]
        ani = final.get(nome, {})
        R = Rarquivo(rot + ani.get("rotation", np.zeros(3)))
        S = ani.get("scale", np.ones(3))
        T = ani.get("position", np.zeros(3)) * np.array(SINAL_POS)
        if pai is None:
            M = np.eye(4)
        else:
            M = mat(pai)
        L = np.eye(4)
        L[:3, :3] = R @ np.diag(S)
        L[:3, 3] = pivo + T - R @ np.diag(S) @ pivo
        cache[nome] = M @ L
        return cache[nome]

    tris = []
    for nome in pos:
        M = mat(nome)
        for c in pos[nome][2]:
            o = np.array(c["origin"], float)
            s = np.array(c["size"], float)
            cor = (200, 40, 40) if nome.startswith(("fil", "drop")) else (110, 14, 20)
            if nome in ("body", "head", "left_arm", "right_arm", "left_leg", "right_leg"):
                cor = {"body": (0, 160, 168), "head": (190, 133, 104), "left_arm": (190, 133, 104),
                       "right_arm": (190, 133, 104)}.get(nome, (62, 70, 160))
            vs = [o + s * np.array([i, j, k]) for i in (0, 1) for j in (0, 1) for k in (0, 1)]
            vs = [(M @ np.append(v, 1))[:3] for v in vs]
            for quad in ((0, 1, 3, 2), (4, 5, 7, 6), (0, 1, 5, 4), (2, 3, 7, 6), (0, 2, 6, 4), (1, 3, 7, 5)):
                tris.append(([vs[q] for q in quad], cor))
    az, el = math.radians(-35), math.radians(18)
    Rv = rx(el) @ ry(az)
    W, H, esc = 720, 760, 17.0
    img = Image.new("RGB", (W, H), (14, 6, 8))
    d = ImageDraw.Draw(img)
    proj = []
    for quad, cor in tris:
        q = [Rv @ v for v in quad]
        z = sum(v[2] for v in q) / 4
        n = np.cross(q[1] - q[0], q[3] - q[0])
        luz = 0.55 + 0.45 * abs(n[2]) / (np.linalg.norm(n) + 1e-9)
        pts = [(W / 2 + v[0] * esc, H - 60 - (v[1] - 0) * esc) for v in q]
        proj.append((z, pts, tuple(int(c * luz) for c in cor)))
    for z, pts, cor in sorted(proj, key=lambda a: -a[0]):
        d.polygon(pts, fill=cor, outline=(20, 4, 6))
    img.save(caminho)


if __name__ == "__main__":
    geo, anim = montar()
    n_chaves = sum(len(v) for o in ANIM_OSSOS.values() for v in o.values())
    print("ossos: %d | ossos animados: %d | keyframes: %d" % (len(BONES), len(ANIM_OSSOS), n_chaves))
    if "--preview" in sys.argv:
        destino = os.path.join(os.environ.get("TMPDIR", "/tmp"), "armadura_preview.png")
        preview(destino)
        print("preview em", destino)
        sys.exit(0)
    for sub, dados in (("geckolib/models/entity/armadura_sangue.geo.json", geo),
                       ("geckolib/animations/entity/armadura_sangue.animation.json", anim)):
        caminho = os.path.join(ASSETS, *sub.split("/"))
        os.makedirs(os.path.dirname(caminho), exist_ok=True)
        with open(caminho, "w", encoding="utf-8") as fh:
            json.dump(dados, fh, ensure_ascii=False, separators=(",", ":"))
        print("gravado:", os.path.relpath(caminho, RAIZ), "(%d KB)" % (os.path.getsize(caminho) // 1024))
    tex = os.path.join(ASSETS, "textures", "entity", "armadura_sangue.png")
    os.makedirs(os.path.dirname(tex), exist_ok=True)
    gerar_textura(tex)
    print("gravado:", os.path.relpath(tex, RAIZ))
