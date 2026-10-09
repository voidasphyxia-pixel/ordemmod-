#!/usr/bin/env python3
"""
Gera os ícones de poderes em pixel art (16x16) da ficha do OrdemMod.

Uso (na pasta do projeto):   python3 tools/gerar_icones.py
Precisa do Pillow:           pip install pillow

Cada ícone = um DESENHO (função abaixo, ex.: espada) + uma COR (paleta vermelha, azul...).
A tabela ICONES no fim do arquivo liga o id de cada poder a um desenho e uma cor.
Para trocar o visual de um poder, mude só a linha dele em ICONES e rode o script de novo.
Os PNGs vão para src/main/resources/assets/ordemmod/textures/gui/poderes/<id>.png
(o jogo procura o arquivo pelo id do poder; sem arquivo, usa generico.png).
"""
import math
import os
import sys

from PIL import Image

T = 16
SAIDA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                     "assets", "ordemmod", "textures", "gui", "poderes")

# ------------------------------------------------------------------ cores
CONTORNO = (22, 18, 28, 255)
AÇO = [(224, 232, 238, 255), (150, 168, 180, 255), (84, 104, 116, 255)]   # claro, médio, escuro
MADEIRA = [(160, 104, 52, 255), (98, 62, 30, 255)]
OURO = [(250, 224, 110, 255), (222, 178, 36, 255), (152, 110, 16, 255)]
PAPEL = [(244, 232, 196, 255), (206, 188, 140, 255)]
BRANCO = (255, 255, 255, 255)
PELE = [(238, 190, 150, 255), (200, 140, 104, 255)]


def rampa(claro, medio, escuro):
    return [claro + (255,), medio + (255,), escuro + (255,)]


PALETAS = {
    "vermelho": rampa((255, 120, 120), (210, 48, 48), (120, 24, 30)),
    "azul": rampa((120, 180, 255), (48, 108, 208), (26, 56, 122)),
    "dourado": rampa((255, 232, 130), (230, 190, 40), (154, 118, 16)),
    "verde": rampa((150, 230, 150), (64, 166, 80), (30, 92, 44)),
    "roxo": rampa((204, 150, 255), (142, 72, 176), (74, 28, 106)),
    "laranja": rampa((255, 184, 112), (230, 126, 34), (138, 70, 16)),
}


class Tela:
    def __init__(self):
        self.p = [[None] * T for _ in range(T)]

    def pt(self, x, y, c):
        x, y = int(x), int(y)
        if 0 <= x < T and 0 <= y < T:
            self.p[y][x] = c

    def ret(self, x0, y0, x1, y1, c):
        for y in range(min(y0, y1), max(y0, y1) + 1):
            for x in range(min(x0, x1), max(x0, x1) + 1):
                self.pt(x, y, c)

    def linha(self, x0, y0, x1, y1, c, grossa=False):
        passos = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(passos + 1):
            x = round(x0 + (x1 - x0) * i / passos)
            y = round(y0 + (y1 - y0) * i / passos)
            self.pt(x, y, c)
            if grossa:
                self.pt(x + 1, y, c)

    def elipse(self, cx, cy, rx, ry, c, cheia=True):
        for y in range(T):
            for x in range(T):
                d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
                if (cheia and d <= 1.0) or (not cheia and 0.55 <= d <= 1.0):
                    self.pt(x, y, c)

    def poli(self, pontos, c):
        """Preenche um polígono (varredura par-ímpar, testando o centro de cada pixel)."""
        n = len(pontos)
        for y in range(T):
            yy = y + 0.5
            xs = []
            for i in range(n):
                (xa, ya), (xb, yb) = pontos[i], pontos[(i + 1) % n]
                if (ya <= yy < yb) or (yb <= yy < ya):
                    xs.append(xa + (yy - ya) * (xb - xa) / (yb - ya))
            xs.sort()
            for i in range(0, len(xs) - 1, 2):
                for x in range(T):
                    if xs[i] <= x + 0.5 < xs[i + 1]:
                        self.pt(x, y, c)

    def espelhar(self):
        for y in range(T):
            self.p[y].reverse()

    def contorno(self):
        novo = [linha[:] for linha in self.p]
        for y in range(T):
            for x in range(T):
                if self.p[y][x] is None:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        nx, ny = x + dx, y + dy
                        if 0 <= nx < T and 0 <= ny < T and self.p[ny][nx] is not None:
                            novo[y][x] = CONTORNO
                            break
        self.p = novo

    def imagem(self):
        img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
        for y in range(T):
            for x in range(T):
                if self.p[y][x] is not None:
                    img.putpixel((x, y), self.p[y][x])
        return img


# ------------------------------------------------------------------ desenhos (r = rampa de cor: claro, médio, escuro)
def espada(t, r):
    t.linha(13, 1, 6, 8, AÇO[1], True)
    t.linha(12, 1, 5, 8, AÇO[0])
    t.linha(13, 2, 7, 8, AÇO[2])
    t.pt(13, 1, BRANCO)
    t.linha(4, 7, 8, 11, r[1], True)
    t.linha(5, 10, 3, 12, MADEIRA[0], True)
    t.pt(2, 13, r[0])
    t.pt(3, 13, r[2])


def espadas_cruzadas(t, r):
    espada(t, r)
    a = Tela()
    espada(a, r)
    a.espelhar()
    for y in range(T):
        for x in range(T):
            if a.p[y][x] is not None:
                t.p[y][x] = a.p[y][x]
    t.ret(7, 7, 8, 8, r[0])


def machado(t, r):
    t.linha(3, 14, 10, 5, MADEIRA[0], True)
    t.linha(4, 14, 11, 5, MADEIRA[1])
    t.poli([(8, 2), (14, 1), (14, 8), (10, 8), (9, 6)], AÇO[1])
    t.linha(13, 2, 13, 7, AÇO[0])
    t.linha(9, 3, 9, 5, AÇO[2])
    t.pt(3, 15, r[1])


def martelo(t, r):
    t.ret(3, 2, 12, 6, AÇO[1])
    t.ret(3, 2, 12, 3, AÇO[0])
    t.ret(3, 6, 12, 6, AÇO[2])
    t.ret(3, 3, 4, 5, r[1])
    t.ret(11, 3, 12, 5, r[1])
    t.ret(7, 7, 8, 14, MADEIRA[0])
    t.ret(8, 7, 8, 14, MADEIRA[1])
    t.ret(7, 13, 8, 14, r[1])


def explosao(t, r):
    pts = []
    for i in range(16):
        ang = math.radians(-90 + i * 22.5)
        raio = 7.4 if i % 2 == 0 else 4.2
        pts.append((8 + raio * math.cos(ang), 8 + raio * math.sin(ang)))
    t.poli(pts, r[1])
    pts2 = [(8 + (0.55 * (px - 8)), 8 + (0.55 * (py - 8))) for px, py in pts]
    t.poli(pts2, r[0])
    t.ret(7, 7, 8, 8, BRANCO)


def armadura(t, r):
    t.ret(2, 3, 5, 6, AÇO[1])
    t.ret(10, 3, 13, 6, AÇO[1])
    t.ret(4, 3, 11, 13, AÇO[1])
    t.ret(7, 3, 8, 4, None)
    t.ret(4, 7, 11, 7, r[1])
    t.ret(4, 8, 11, 8, r[2])
    t.ret(7, 5, 8, 12, AÇO[0])
    t.ret(4, 13, 11, 13, AÇO[2])
    t.ret(2, 3, 3, 3, AÇO[0])
    t.ret(12, 3, 13, 3, AÇO[0])


def capacete(t, r):
    t.ret(4, 4, 11, 12, AÇO[1])
    t.ret(5, 3, 10, 3, AÇO[1])
    t.ret(6, 2, 9, 2, AÇO[1])
    t.ret(5, 3, 7, 4, AÇO[0])
    t.ret(5, 7, 10, 8, CONTORNO)
    t.ret(4, 11, 5, 13, AÇO[2])
    t.ret(10, 11, 11, 13, AÇO[2])
    t.ret(7, 9, 8, 13, AÇO[2])
    t.ret(7, 0, 8, 2, r[1])
    t.pt(7, 0, r[0])


def punho(t, r):
    t.ret(4, 6, 11, 12, r[1])
    for x0, topo in ((4, 5), (6, 4), (8, 4), (10, 5)):
        t.ret(x0, topo, x0 + 1, 6, r[0])
    for x in (6, 8, 10):
        t.ret(x, 5, x, 7, r[2])
    t.ret(3, 8, 4, 10, r[0])
    t.ret(5, 12, 10, 14, AÇO[2])
    t.ret(5, 12, 10, 12, AÇO[1])
    t.ret(5, 8, 10, 8, r[2])


def escudo(t, r):
    t.ret(3, 2, 12, 8, r[1])
    for y, (a, b) in enumerate([(4, 11), (5, 10), (6, 9), (7, 8)], start=9):
        t.ret(a, y, b, y, r[1])
    t.ret(7, 3, 8, 11, r[0])
    t.ret(4, 5, 11, 6, r[0])
    t.ret(3, 2, 12, 2, OURO[1])
    t.ret(11, 3, 12, 8, r[2])
    t.pt(7, 12, r[2])


def escudo_quebrado(t, r):
    escudo(t, r)
    for x, y in ((8, 2), (7, 3), (7, 4), (8, 5), (9, 6), (8, 7), (7, 8), (7, 9), (8, 10), (8, 11)):
        t.pt(x, y, CONTORNO)
    for x, y in ((9, 2), (8, 3), (8, 4), (9, 5), (10, 6), (9, 7), (8, 8), (8, 9), (9, 10)):
        t.pt(x, y, r[2])


def olho(t, r):
    for y, (a, b) in zip(range(4, 11), [(6, 9), (4, 11), (3, 12), (2, 13), (3, 12), (4, 11), (6, 9)]):
        t.ret(a, y, b, y, BRANCO)
    t.ret(6, 5, 9, 9, r[1])
    t.ret(7, 6, 8, 8, CONTORNO)
    t.pt(6, 6, BRANCO)
    t.ret(2, 7, 3, 7, AÇO[2])
    t.ret(12, 7, 13, 7, AÇO[2])


def olho_alerta(t, r):
    olho(t, r)
    t.ret(7, 0, 8, 1, r[1])
    t.linha(3, 2, 4, 3, r[1])
    t.linha(12, 2, 11, 3, r[1])


def torre(t, r):
    t.ret(4, 5, 11, 14, AÇO[1])
    t.ret(4, 3, 5, 4, AÇO[1])
    t.ret(7, 3, 8, 4, AÇO[1])
    t.ret(10, 3, 11, 4, AÇO[1])
    t.ret(4, 5, 5, 14, AÇO[0])
    t.ret(10, 5, 11, 14, AÇO[2])
    t.ret(7, 10, 8, 14, CONTORNO)
    t.ret(7, 7, 8, 8, r[1])
    t.ret(4, 5, 11, 5, r[2])


def livro(t, r):
    t.ret(3, 2, 12, 13, r[1])
    t.ret(3, 2, 4, 13, r[2])
    t.ret(5, 2, 12, 2, r[0])
    t.ret(5, 4, 10, 5, OURO[1])
    t.ret(5, 8, 10, 8, r[2])
    t.ret(5, 10, 9, 10, r[2])
    t.ret(4, 14, 12, 14, PAPEL[0])
    t.ret(12, 3, 13, 13, PAPEL[0])
    t.ret(13, 4, 13, 12, PAPEL[1])


def mira(t, r):
    t.elipse(7.5, 7.5, 5.3, 5.3, r[1], cheia=False)
    for a, b, c, d in ((8, 0, 8, 4), (8, 11, 8, 15), (0, 8, 4, 8), (11, 8, 15, 8)):
        t.linha(a, b, c, d, r[0])
    t.ret(7, 7, 8, 8, BRANCO)


def municao(t, r):
    for x0 in (1, 6, 11):
        t.ret(x0, 8, x0 + 2, 14, OURO[1])
        t.ret(x0, 8, x0, 14, OURO[0])
        t.ret(x0 + 2, 8, x0 + 2, 14, OURO[2])
        t.ret(x0, 5, x0 + 2, 7, r[1])
        t.ret(x0 + 1, 3, x0 + 1, 4, r[1])
        t.pt(x0, 5, r[0])
        t.pt(x0, 7, r[2])
        t.ret(x0, 14, x0 + 2, 14, OURO[2])


def pistola(t, r):
    t.ret(2, 4, 13, 7, AÇO[1])
    t.ret(2, 4, 13, 4, AÇO[0])
    t.ret(2, 7, 13, 7, AÇO[2])
    t.ret(14, 5, 15, 6, AÇO[2])
    t.ret(3, 8, 6, 14, MADEIRA[0])
    t.ret(6, 8, 6, 14, MADEIRA[1])
    t.ret(7, 8, 9, 8, AÇO[2])
    t.pt(7, 9, AÇO[2])
    t.pt(3, 3, AÇO[2])
    t.pt(12, 3, AÇO[2])
    t.pt(10, 5, r[1])
    t.pt(11, 5, r[1])


def pergaminho(t, r):
    t.ret(4, 3, 11, 12, PAPEL[0])
    t.ret(3, 2, 12, 3, PAPEL[1])
    t.ret(3, 12, 12, 13, PAPEL[1])
    for y in (5, 7, 9):
        t.ret(5, y, 9, y, r[2])
    t.ret(9, 10, 10, 11, r[1])
    t.pt(9, 10, r[0])


def engrenagem(t, r):
    for a, b, c, d in ((7, 1, 8, 3), (7, 12, 8, 14), (1, 7, 3, 8), (12, 7, 14, 8),
                       (2, 2, 3, 3), (12, 2, 13, 3), (2, 12, 3, 13), (12, 12, 13, 13)):
        t.ret(a, b, c, d, AÇO[1])
    t.elipse(7.5, 7.5, 5.2, 5.2, AÇO[1])
    t.elipse(7.5, 7.5, 5.2, 5.2, AÇO[0], cheia=False)
    t.elipse(7.5, 7.5, 2.2, 2.2, CONTORNO)
    t.ret(7, 7, 8, 8, r[1])


def bandeira(t, r):
    t.ret(3, 1, 3, 14, MADEIRA[0])
    t.poli([(4, 2), (13, 2), (10, 5), (13, 8), (4, 8)], r[1])
    t.ret(4, 2, 12, 3, r[0])
    t.ret(4, 7, 11, 8, r[2])
    t.ret(6, 4, 7, 5, BRANCO)
    t.ret(2, 14, 5, 14, AÇO[2])


def mapa(t, r):
    t.ret(2, 3, 13, 13, PAPEL[0])
    t.ret(2, 3, 13, 3, PAPEL[1])
    t.ret(2, 13, 13, 13, PAPEL[1])
    t.ret(6, 3, 6, 13, PAPEL[1])
    t.ret(10, 3, 10, 13, PAPEL[1])
    for x, y in ((3, 10), (4, 9), (5, 8), (7, 8), (8, 7), (9, 6)):
        t.pt(x, y, r[2])
    t.linha(10, 4, 12, 6, r[1])
    t.linha(12, 4, 10, 6, r[1])


def coroa(t, r):
    t.ret(3, 9, 12, 12, OURO[1])
    t.ret(3, 9, 12, 9, OURO[0])
    t.ret(3, 12, 12, 12, OURO[2])
    t.ret(3, 5, 4, 8, OURO[1])
    t.ret(7, 3, 8, 8, OURO[1])
    t.ret(11, 5, 12, 8, OURO[1])
    t.pt(3, 5, OURO[0])
    t.pt(7, 3, OURO[0])
    t.pt(11, 5, OURO[0])
    for x in (5, 8, 11):
        t.pt(x, 10, r[1])
        t.pt(x, 11, r[2])


def caveira(t, r):
    for y, (a, b) in zip(range(2, 13), [(5, 10), (4, 11), (3, 12), (3, 12), (3, 12), (3, 12), (3, 12), (4, 11), (5, 10), (5, 10), (5, 10)]):
        t.ret(a, y, b, y, PAPEL[0])
    t.ret(3, 5, 3, 8, PAPEL[1])
    t.ret(5, 6, 6, 8, CONTORNO)
    t.ret(9, 6, 10, 8, CONTORNO)
    t.ret(5, 6, 5, 6, r[1])
    t.ret(9, 6, 9, 6, r[1])
    t.ret(7, 9, 8, 9, CONTORNO)
    for x in (6, 8, 10):
        t.ret(x, 11, x, 12, PAPEL[1])


def contra_ataque(t, r):
    t.ret(11, 2, 12, 10, r[1])
    t.ret(5, 9, 12, 10, r[1])
    t.poli([(2, 9.5), (7, 5), (7, 14)], r[1])
    t.ret(11, 2, 11, 9, r[0])
    t.ret(6, 9, 11, 9, r[0])
    t.ret(11, 10, 12, 10, r[2])
    t.pt(7, 5, r[0])


def bota(t, r):
    t.ret(5, 2, 9, 9, r[1])
    t.ret(5, 2, 5, 9, r[0])
    t.ret(9, 2, 9, 9, r[2])
    t.poli([(5, 9), (10, 9), (14, 11), (14, 14), (4, 14), (4, 9)], r[1])
    t.ret(4, 13, 14, 14, AÇO[2])
    t.ret(5, 5, 9, 5, AÇO[0])
    t.ret(11, 11, 13, 11, r[0])
    t.ret(0, 4, 3, 4, BRANCO)
    t.ret(0, 7, 2, 7, BRANCO)
    t.ret(1, 10, 3, 10, BRANCO)


def seringa(t, r):
    t.ret(4, 6, 11, 9, AÇO[0])
    t.ret(5, 7, 10, 8, r[1])
    t.ret(4, 6, 11, 6, AÇO[1])
    t.ret(4, 9, 11, 9, AÇO[1])
    t.ret(2, 5, 3, 10, AÇO[2])
    t.ret(0, 7, 1, 8, AÇO[1])
    t.ret(12, 7, 15, 8, AÇO[0])
    t.ret(12, 7, 15, 7, AÇO[1])
    t.ret(7, 4, 8, 5, r[0])
    t.ret(7, 10, 8, 11, r[0])


def coracao(t, r):
    rows = {3: [(3, 6), (9, 12)], 4: [(2, 7), (8, 13)], 5: [(2, 13)], 6: [(2, 13)], 7: [(2, 13)],
            8: [(3, 12)], 9: [(4, 11)], 10: [(5, 10)], 11: [(6, 9)], 12: [(7, 8)]}
    for y, faixas in rows.items():
        for a, b in faixas:
            t.ret(a, y, b, y, r[1])
    t.ret(3, 4, 4, 5, r[0])
    t.ret(11, 7, 13, 8, r[2])
    t.ret(10, 9, 11, 9, r[2])
    t.ret(7, 11, 8, 12, r[2])


def raio(t, r):
    t.poli([(10, 0), (3, 9), (7, 9), (5, 15), (13, 6), (9, 6), (12, 0)], r[1])
    t.poli([(10, 1), (5, 8), (8, 8), (7, 12), (11, 6), (8, 6), (11, 1)], r[0])


def diamante(t, r):
    t.poli([(8, 1), (14, 6), (8, 15), (2, 6)], r[1])
    t.ret(3, 6, 13, 6, r[0])
    t.poli([(8, 1), (11, 6), (5, 6)], r[0])
    t.poli([(8, 15), (11, 7), (13, 7)], r[2])
    t.pt(7, 3, BRANCO)


def cerebro(t, r):
    t.elipse(5.5, 6.5, 3.6, 3.4, r[1])
    t.elipse(10.5, 6.5, 3.6, 3.4, r[1])
    t.ret(4, 8, 11, 11, r[1])
    t.ret(7, 3, 8, 11, r[2])
    t.pt(4, 5, r[0])
    t.pt(5, 5, r[0])
    t.pt(11, 5, r[0])
    t.pt(10, 8, r[2])
    t.pt(5, 9, r[2])
    t.ret(6, 12, 9, 13, r[2])


def runa(t, r):
    t.elipse(7.5, 7.5, 6.3, 6.3, r[1], cheia=False)
    pts = [(7.5 + 5.2 * math.cos(math.radians(-90 + 72 * i)), 7.5 + 5.2 * math.sin(math.radians(-90 + 72 * i))) for i in range(5)]
    for i in range(5):
        a, b = pts[i], pts[(i + 2) % 5]
        t.linha(round(a[0]), round(a[1]), round(b[0]), round(b[1]), r[0])
    t.ret(7, 7, 8, 8, BRANCO)


def estrela(t, r):
    pts = []
    for i in range(10):
        ang = math.radians(-90 + i * 36)
        raio = 7.2 if i % 2 == 0 else 3.1
        pts.append((8 + raio * math.cos(ang), 8.6 + raio * math.sin(ang)))
    t.poli(pts, r[1])
    t.poli([(8 + 0.5 * (x - 8), 8.6 + 0.5 * (y - 8.6)) for x, y in pts], r[0])


# ------------------------------------------------------------------ id do poder -> (desenho, cor)
DESENHOS = {
    "espada": espada, "espadas_cruzadas": espadas_cruzadas, "machado": machado, "martelo": martelo,
    "explosao": explosao, "armadura": armadura, "capacete": capacete, "punho": punho, "escudo": escudo,
    "escudo_quebrado": escudo_quebrado, "olho": olho, "olho_alerta": olho_alerta, "torre": torre,
    "livro": livro, "mira": mira, "municao": municao, "pistola": pistola, "pergaminho": pergaminho,
    "engrenagem": engrenagem, "bandeira": bandeira, "mapa": mapa, "coroa": coroa, "caveira": caveira,
    "contra_ataque": contra_ataque, "bota": bota, "seringa": seringa, "coracao": coracao, "raio": raio,
    "diamante": diamante, "cerebro": cerebro, "runa": runa, "estrela": estrela,
}

ICONES = {
    # Combatente: poderes de classe
    "ataque_especial": ("espada", "vermelho"),
    "ataque_devastador": ("explosao", "vermelho"),
    "pele_grossa": ("armadura", "laranja"),
    "armamento_pesado": ("machado", "laranja"),
    "protecao_pesada": ("capacete", "azul"),
    "artista_marcial": ("punho", "dourado"),
    "reflexos_defensivos": ("escudo", "azul"),
    "sentido_tatico": ("olho", "verde"),
    "tanque_de_guerra": ("torre", "azul"),
    "golpe_pesado": ("martelo", "vermelho"),
    "treinamento_pericia": ("livro", "dourado"),
    "tiro_certeiro": ("mira", "verde"),
    "mira_treinada": ("mira", "vermelho"),
    "rajada": ("municao", "dourado"),
    "fogo_de_cobertura": ("pistola", "verde"),
    # Combatente: trilhas
    "a_favorita": ("espada", "dourado"),
    "tecnica_secreta": ("pergaminho", "vermelho"),
    "tecnica_sublime": ("pergaminho", "dourado"),
    "maquina_de_guerra": ("engrenagem", "vermelho"),
    "inspirar_confianca": ("bandeira", "dourado"),
    "estrategista": ("mapa", "verde"),
    "brecha_na_guarda": ("escudo_quebrado", "vermelho"),
    "oficial_comandante": ("coroa", "dourado"),
    "tecnica_letal": ("caveira", "vermelho"),
    "revidar": ("contra_ataque", "azul"),
    "forca_opressora": ("punho", "vermelho"),
    "potencia_maxima": ("explosao", "laranja"),
    "iniciativa_aprimorada": ("bota", "verde"),
    "ataque_extra": ("espadas_cruzadas", "azul"),
    "surto_de_adrenalina": ("seringa", "verde"),
    "sempre_alerta": ("olho_alerta", "azul"),
    "casca_grossa": ("coracao", "vermelho"),
    "cai_dentro": ("raio", "laranja"),
    "duro_de_matar": ("armadura", "azul"),
    "inquebravel": ("diamante", "azul"),
    # Especialista e Ocultista (exemplos)
    "golpe_certeiro": ("mira", "dourado"),
    "olho_treinado": ("olho", "dourado"),
    "golpe_mortal": ("caveira", "roxo"),
    "escudo_mental": ("cerebro", "azul"),
    "sentir_outro_lado": ("runa", "roxo"),
    "mente_blindada": ("cerebro", "roxo"),
    # quando um poder não tem ícone
    "generico": ("estrela", "dourado"),
}


def gerar(desenho, paleta):
    t = Tela()
    DESENHOS[desenho](t, PALETAS[paleta])
    t.contorno()
    return t.imagem()


def main():
    os.makedirs(SAIDA, exist_ok=True)
    for pid, (desenho, paleta) in ICONES.items():
        gerar(desenho, paleta).save(os.path.join(SAIDA, pid + ".png"))
    print(len(ICONES), "ícones em", os.path.normpath(SAIDA))
    if "--folha" in sys.argv:  # folha de conferência ampliada (para olhar todos de uma vez)
        cols = 8
        linhas = (len(ICONES) + cols - 1) // cols
        esc = 6
        folha = Image.new("RGBA", (cols * (T + 2) * esc, linhas * (T + 2) * esc), (30, 30, 36, 255))
        for i, (pid, (d, p)) in enumerate(ICONES.items()):
            ic = gerar(d, p).resize((T * esc, T * esc), Image.NEAREST)
            folha.paste(ic, ((i % cols) * (T + 2) * esc + esc, (i // cols) * (T + 2) * esc + esc), ic)
        folha.save("/tmp/folha_icones.png")


if __name__ == "__main__":
    main()
