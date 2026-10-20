#!/usr/bin/env python3
"""
Gera as texturas das CORRENTES da tela inicial (titulo/CorrentesFundo.java): uma corrente de elos retangulares (como a do
render de referencia: blocos com textura de pixels cinza-azulada e um toque vermelho nas faces de baixo), renderizada em 3D
por ray casting ortografico, em 24 quadros de GIRO em torno do proprio eixo.

Saida: src/main/resources/assets/ordemmod/textures/gui/titulo/
  correntes_nitida.png    elos nitidos (camada da frente)
  correntes_media.png     metade da resolucao + leve desfoque (camada do meio)
  correntes_fundo.png     metade da resolucao + desfoque forte (camada do fundo)
  *.png.mcmeta            "blur": true (filtro suave ao ampliar)

Cada PNG e uma grade COLS x LINHAS de celulas; a celula f (0..23) e a corrente com o giro f * 90/24 graus. Cada celula traz
PAD pixels de sobra de cada lado (conteudo repetido do outro lado do periodo) para o filtro suave nao deixar emenda entre
ladrilhos. O ladrilho util tem LADRILHO_W = 2 passos (2 elos) de largura. Padrao: com 90 graus a mais a corrente fica igual, so
andando 1 passo para o lado (o jogo usa isso para girar sem fim; veja CorrentesFundo.java). Os numeros abaixo TEM de bater com as
constantes do topo de CorrentesFundo.java (o script imprime o que o Java precisa).

Uso (na pasta do projeto):   python3 tools/gerar_correntes.py [--rapido]     (--rapido = so 1 quadro de teste, em /tmp)
Precisa de numpy e Pillow (+ scipy so para o desfoque).
"""
import math
import os
import sys

import numpy as np
from PIL import Image

SAIDA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "ordemmod",
                     "textures", "gui", "titulo")

# ------------------------------------------------------------------ geometria do elo (em pixels do atlas NITIDO)
T = 30                      # espessura do arame (secao quadrada T x T)
LO = 240                    # comprimento externo do elo
HO = 112                    # altura externa do elo
FOLGA = 8                   # folga entre elos
PASSO = LO - 2 * T - FOLGA  # distancia entre elos consecutivos (172)
LADRILHO_W = 2 * PASSO      # 344: dois elos (um de frente, um de lado) formam o ladrilho que se repete
FH = HO + 8                 # altura util do quadro (120)
PAD = 4                     # sobra lateral (px do atlas nitido) para o filtro suave
QUADROS = 24                # quadros do giro de 0 a 90 graus
COLS, LINHAS = 4, 6
TEXEL = T / 6.0             # tamanho de um "pixel" da textura de pedra/metal


# ------------------------------------------------------------------ textura (pedra/metal pixelado)
def textura_base(seed=5, n=64):
    """Textura n x n de pixels cinza-azulados com manchas, como a do render de referencia (media ~ (88,85,93))."""
    rng = np.random.default_rng(seed)
    ruido = rng.normal(0, 1, (n, n))
    grosso = np.kron(rng.normal(0, 1, (n // 8, n // 8)), np.ones((8, 8)))
    medio = np.kron(rng.normal(0, 1, (n // 4, n // 4)), np.ones((4, 4)))
    v = 0.55 * ruido + 0.50 * medio + 0.45 * grosso
    v = (v - v.mean()) / v.std()
    base = np.array([88.0, 85.0, 93.0])
    tex = base[None, None, :] + v[..., None] * np.array([30.0, 28.0, 31.0])
    # riscos claros esparsos (brilho de metal)
    riscos = rng.random((n, n)) > 0.985
    tex[riscos] += 35
    return np.clip(tex, 12, 235) / 255.0


TEX = textura_base()
TN = TEX.shape[0]


# ------------------------------------------------------------------ ray casting ortografico
def intervalo(a, b, lo, hi):
    """Valores de zw com lo <= a*zw + b <= hi, como (zmin, zmax); vazio => zmin > zmax."""
    pequeno = np.abs(a) < 1e-9
    az = np.where(pequeno, 1.0, a)
    z1 = (lo - b) / az
    z2 = (hi - b) / az
    zmin = np.minimum(z1, z2)
    zmax = np.maximum(z1, z2)
    dentro = (b >= lo) & (b <= hi)
    zmin = np.where(pequeno, np.where(dentro, -1e9, 1e9), zmin)
    zmax = np.where(pequeno, np.where(dentro, 1e9, -1e9), zmax)
    return zmin, zmax


def caixas_do_elo():
    """As 4 caixas do elo no sistema local (x ao longo da corrente, y altura, z profundidade): (x0,x1,y0,y1,z0,z1)."""
    d = T / 2.0
    h = HO / 2.0
    return [
        (0, LO, h - T, h, -d, d),          # barra de cima
        (0, LO, -h, -h + T, -d, d),        # barra de baixo
        (0, T, -h + T, h - T, -d, d),      # extremidade esquerda
        (LO - T, LO, -h + T, h - T, -d, d)  # extremidade direita
    ]


def luz(normal_mundo):
    """Cor de iluminacao (r,g,b) para a normal no mundo (nx, ny, nz): luz fria vinda de cima + luz vermelha de baixo."""
    nx, ny, nz = normal_mundo
    chave = np.array([-0.35, 0.75, 0.55])
    chave = chave / np.linalg.norm(chave)
    rubra = np.array([0.10, -0.97, 0.15])
    rubra = rubra / np.linalg.norm(rubra)
    n = np.stack([nx, ny, nz], -1)
    d1 = np.clip(n @ chave, 0, 1)[..., None]
    d2 = np.clip(n @ rubra, 0, 1)[..., None]
    # 20/10/2026: clareada e esfriada para chegar perto do render de referencia (brilho medio ~89, cor media ~(88,85,93), so ~8% de
    # pixels avermelhados). Antes: ambiente 0,2 e luz vermelha forte (0.62,0.12,0.10) deixavam a corrente ~metade do brilho e 2x mais vermelha.
    return 1.42 * (0.46 + d1 * np.array([0.62, 0.64, 0.70])) + d2 * np.array([0.34, 0.035, 0.03])


def renderiza_quadro(giro_graus, ss=2, x_ini=-PAD, largura=LADRILHO_W + 2 * PAD):
    """Um quadro: largura x FH pixels (RGBA float 0..1) da corrente com o giro dado, cobrindo x em [x_ini, x_ini + largura)."""
    w, h = largura * ss, FH * ss
    xs = x_ini + (np.arange(w) + 0.5) / ss
    ys = (FH / 2.0) - (np.arange(h) + 0.5) / ss            # y do mundo (para cima positivo), centro do quadro = 0
    X, Y = np.meshgrid(xs, ys)
    melhor_z = np.full(X.shape, -1e9)
    cor = np.zeros(X.shape + (3,))
    acerto = np.zeros(X.shape, bool)
    for i in range(-3, 6):
        phi = math.radians(giro_graus + 90.0 * (i % 2))
        c, s = math.cos(phi), math.sin(phi)
        x0 = i * PASSO
        for (bx0, bx1, by0, by1, bz0, bz1) in caixas_do_elo():
            xl = X - x0
            dentro_x = (xl >= bx0) & (xl <= bx1)
            if not dentro_x.any():
                continue
            # local: y = Y*c + zw*s ; z = -Y*s + zw*c
            ymin, ymax = intervalo(np.full(X.shape, s), Y * c, by0, by1)
            zmin, zmax = intervalo(np.full(X.shape, c), -Y * s, bz0, bz1)
            lo = np.maximum(ymin, zmin)
            hi = np.minimum(ymax, zmax)
            bate = dentro_x & (lo <= hi) & (hi > melhor_z)
            if not bate.any():
                continue
            zw = hi
            yl = Y * c + zw * s
            zl = -Y * s + zw * c
            # face atingida: a mais proxima entre y0,y1,z0,z1 (no ponto de entrada)
            dist = np.stack([np.abs(yl - by0), np.abs(yl - by1), np.abs(zl - bz0), np.abs(zl - bz1)], -1)
            f = dist.argmin(-1)
            # normal local (0, ny, nz) -> mundo: ny' = ny*c - nz*s ; nz' = ny*s + nz*c
            ny = np.where(f == 0, -1.0, np.where(f == 1, 1.0, 0.0))
            nz = np.where(f == 2, -1.0, np.where(f == 3, 1.0, 0.0))
            nyw = ny * c - nz * s
            nzw = ny * s + nz * c
            nxw = np.zeros_like(nyw)
            # coordenadas de textura (pixeladas): u = x local; v = distancia ate o centro da caixa, na direcao "do anel" (z nas
            # faces de y, y nas faces de z). Assim a textura nao depende do elo nem de qual face e: a corrente a 90 graus
            # fica IDENTICA a de 0 graus deslocada 1 passo, e o ciclo do jogo fecha sem "pulo" no ruido.
            cy_ = (by0 + by1) / 2.0
            cz_ = (bz0 + bz1) / 2.0
            u = xl
            vv = np.where(f < 2, np.abs(zl - cz_), np.abs(yl - cy_))
            tu = np.floor(u / TEXEL).astype(int) % TN
            tv = np.floor(vv / TEXEL).astype(int) % TN
            albedo = TEX[tv, tu]
            # escurece perto das quinas da face (da volume): distancia ate a borda da face em texels
            if True:
                du = np.minimum(xl - bx0, bx1 - xl)
                dv = np.where(f < 2, np.minimum(zl - bz0, bz1 - zl), np.minimum(yl - by0, by1 - yl))
                borda = np.clip(np.minimum(du, dv) / (TEXEL * 1.2), 0, 1)
                albedo = albedo * (0.72 + 0.28 * borda[..., None])
            sombra = luz((nxw, nyw, nzw))
            pix = np.clip(albedo * sombra, 0, 1)
            cor = np.where(bate[..., None], pix, cor)
            melhor_z = np.where(bate, hi, melhor_z)
            acerto |= bate
    rgba = np.zeros(X.shape + (4,))
    rgba[..., :3] = cor * acerto[..., None]
    rgba[..., 3] = acerto
    # reduz o supersampling por media (alfa pre-multiplicado)
    pm = rgba.copy()
    pm[..., :3] *= pm[..., 3:4]
    pm = pm.reshape(FH, ss, largura, ss, 4).mean((1, 3))
    a = pm[..., 3:4]
    out = np.zeros_like(pm)
    out[..., :3] = np.where(a > 1e-6, pm[..., :3] / np.maximum(a, 1e-6), 0)
    out[..., 3:4] = a
    return out


def para_imagem(rgba):
    return Image.fromarray((np.clip(rgba, 0, 1) * 255 + 0.5).astype(np.uint8), "RGBA")


def desfoca(img, sigma):
    from scipy.ndimage import gaussian_filter
    a = np.asarray(img).astype(float) / 255.0
    pm = a.copy()
    pm[..., :3] *= pm[..., 3:4]
    for c in range(4):
        pm[..., c] = gaussian_filter(pm[..., c], sigma, mode="nearest")
    out = np.zeros_like(pm)
    al = pm[..., 3:4]
    out[..., :3] = np.where(al > 1e-6, pm[..., :3] / np.maximum(al, 1e-6), 0)
    out[..., 3:4] = al
    return para_imagem(out)


def sangra_cor(img):
    """Copia a cor dos pixels vizinhos para dentro da area transparente (RGB), sem mexer no alfa. Com o filtro suave do jogo, o
    RGB preto dos pixels transparentes escureceria as bordas; assim a franja some."""
    from scipy.ndimage import distance_transform_edt
    a = np.asarray(img).copy()
    vazio = a[..., 3] < 3
    if not vazio.any() or vazio.all():
        return img
    _, (iy, ix) = distance_transform_edt(vazio, return_indices=True)
    a[..., :3] = a[iy, ix, :3]
    return Image.fromarray(a, "RGBA")


def atlas(nome, quadros, escala, sigma):
    cel_w = int(round((LADRILHO_W + 2 * PAD) * escala))
    cel_h = int(round(FH * escala))
    folha = Image.new("RGBA", (cel_w * COLS, cel_h * LINHAS), (0, 0, 0, 0))
    for f in range(QUADROS):
        q = quadros[f]
        if escala != 1.0:
            q = q.resize((cel_w, cel_h), Image.LANCZOS)
        if sigma > 0:
            q = desfoca(q, sigma)
        folha.paste(sangra_cor(q), ((f % COLS) * cel_w, (f // COLS) * cel_h))
        print(f"  {nome}: quadro {f + 1}/{QUADROS}", end="\r")
    destino = os.path.join(SAIDA, nome + ".png")
    folha.save(destino, optimize=True)
    with open(destino + ".mcmeta", "w", encoding="utf-8") as fh:
        fh.write('{\n  "texture": {\n    "blur": true\n  }\n}\n')
    print(f"ok: {os.path.normpath(destino)}  celula {cel_w}x{cel_h}, folha {folha.width}x{folha.height}, "
          f"{os.path.getsize(destino) // 1024} KB")
    return cel_w, cel_h, folha.width, folha.height


def main():
    if "--rapido" in sys.argv:
        q = para_imagem(renderiza_quadro(float(sys.argv[sys.argv.index("--rapido") + 1]) if len(sys.argv) > 2 else 20.0))
        fundo = Image.new("RGBA", q.size, (5, 5, 7, 255))
        fundo.alpha_composite(q)
        fundo.save("/tmp/corrente_teste.png")
        print("teste em /tmp/corrente_teste.png", q.size)
        return
    os.makedirs(SAIDA, exist_ok=True)
    print(f"passo={PASSO} ladrilho={LADRILHO_W} quadro_h={FH} pad={PAD}")
    # PAD e proporcional: nas folhas pequenas o pad vira 2 px (escala 0,5)
    quadros = []
    for k in range(QUADROS):                      # os 24 quadros sao renderizados uma vez e servem para as 3 folhas
        quadros.append(para_imagem(renderiza_quadro(k * 90.0 / QUADROS, ss=3)))
        print(f"  renderizado {k + 1}/{QUADROS}", end="\r")
    n = atlas("correntes_nitida", quadros, 1.0, 0.0)
    m = atlas("correntes_media", quadros, 0.5, 1.0)
    f = atlas("correntes_fundo", quadros, 0.5, 3.2)
    print("\nCONSTANTES para CorrentesFundo.java:")
    print(f"  nitida: celula {n[0]}x{n[1]}, folha {n[2]}x{n[3]}")
    print(f"  media : celula {m[0]}x{m[1]}, folha {m[2]}x{m[3]}")
    print(f"  fundo : celula {f[0]}x{f[1]}, folha {f[2]}x{f[3]}")


if __name__ == "__main__":
    main()
