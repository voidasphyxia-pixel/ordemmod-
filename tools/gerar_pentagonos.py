#!/usr/bin/env python3
"""
Gera as texturas SUAVIZADAS (antialiasing) dos pentagonos de habilidades da HUD (HabilidadesHud).

Uso (na pasta do projeto):   python3 tools/gerar_pentagonos.py
Precisa do Pillow:           pip install pillow

Saida: src/main/resources/assets/ordemmod/textures/gui/hud_habilidades/
  moldura.png    anel do pentagono (branco; o jogo pinta com a cor da borda)
  brilho.png     halo suave (branco; o jogo pinta de dourado)
  <id>.png       fundo escuro + icone do poder recortado no pentagono interno (um por arquivo de poderes/)
  *.png.mcmeta   "blur": true, para o jogo filtrar de forma suave ao reduzir
Rode de novo sempre que um icone de poderes/ mudar ou for criado.

Geometria (igual a HabilidadesHud.java): pentagono regular com a ponta para cima, raio 1; largura 2*cos18,
altura 1+cos36. A borda e proporcional ao raio (BORDA_REL), entao o slot pequeno e o grande ficam parecidos.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets",
                    "ordemmod", "textures", "gui")
PODERES = os.path.join(RAIZ, "poderes")
SAIDA = os.path.join(RAIZ, "hud_habilidades")

LARG = 192                      # largura da textura do pentagono (px); o jogo reduz com filtro suave
SS = 4                          # supersampling para as bordas
BORDA_REL = 1.6 / 17.0          # espessura da borda / raio (o slot grande tem raio 17 e borda 1,6)
FUNDO = (9, 9, 16, 0xE0)        # mesmo COR_FUNDO do HabilidadesHud
ZOOM_ICONE = 1.18               # amplia o icone para cortar a moldura propria dos PNGs de 128 px

C18 = math.cos(math.radians(18))
C36 = math.cos(math.radians(36))
ANG = [-90, -18, 54, 126, 198]
ALT = round(LARG * (1 + C36) / (2 * C18))   # altura da textura (= largura * 0,951)


def poligono(cx, cy, r):
    return [(cx + r * math.cos(math.radians(a)), cy + r * math.sin(math.radians(a))) for a in ANG]


def mascara(w, h, cx, cy, r):
    """Mascara L do pentagono de raio r (em px da tela grande w x h), com supersampling."""
    m = Image.new("L", (w * SS, h * SS), 0)
    ImageDraw.Draw(m).polygon([(x * SS, y * SS) for x, y in poligono(cx, cy, r)], fill=255)
    return m.resize((w, h), Image.LANCZOS)


def geometria():
    escala = LARG / (2 * C18)     # px por unidade de raio
    return escala, LARG / 2, escala   # escala, cx, cy (circuncentro: 1 raio abaixo da ponta)


def salvar(img, nome):
    os.makedirs(SAIDA, exist_ok=True)
    caminho = os.path.join(SAIDA, nome)
    img.save(caminho)
    with open(caminho + ".mcmeta", "w", encoding="utf-8") as f:
        f.write('{\n  "texture": {\n    "blur": true\n  }\n}\n')


def moldura():
    esc, cx, cy = geometria()
    ext = mascara(LARG, ALT, cx, cy, esc)
    inte = mascara(LARG, ALT, cx, cy, esc * (1 - BORDA_REL))
    anel = ImageChops.subtract(ext, inte)
    img = Image.new("RGBA", (LARG, ALT), (255, 255, 255, 0))
    img.putalpha(anel)
    salvar(img, "moldura.png")


def brilho():
    lado = 256
    img = Image.new("L", (lado, lado), 0)
    r = lado * 0.36
    ImageDraw.Draw(img).polygon(poligono(lado / 2, lado / 2, r), fill=255)
    img = img.filter(ImageFilter.GaussianBlur(lado * 0.05))
    out = Image.new("RGBA", (lado, lado), (255, 255, 255, 0))
    out.putalpha(img)
    salvar(out, "brilho.png")


def icone(arquivo):
    esc, cx, cy = geometria()
    ri = esc * (1 - BORDA_REL)
    # o icone e um quadrado com a largura do pentagono interno, centrado na caixa dele (cobre a forma inteira)
    lado = round(2 * ri * C18 * ZOOM_ICONE)
    centro_y = cy - ri + ri * (1 + C36) / 2
    base = Image.new("RGBA", (LARG, ALT), (0, 0, 0, 0))
    base.paste(Image.new("RGBA", (LARG, ALT), FUNDO))
    ic = Image.open(os.path.join(PODERES, arquivo)).convert("RGBA").resize((lado, lado), Image.LANCZOS)
    camada = Image.new("RGBA", (LARG, ALT), (0, 0, 0, 0))
    camada.paste(ic, (round(cx - lado / 2), round(centro_y - lado / 2)))
    base.alpha_composite(camada)
    mask = mascara(LARG, ALT, cx, cy, ri)
    base.putalpha(ImageChops.multiply(base.getchannel("A"), mask))
    salvar(base, arquivo)


def main():
    moldura()
    brilho()
    n = 0
    for arq in sorted(os.listdir(PODERES)):
        if arq.endswith(".png"):
            icone(arq)
            n += 1
    print(f"ok: moldura, brilho e {n} icones em {os.path.normpath(SAIDA)}")


if __name__ == "__main__":
    main()
