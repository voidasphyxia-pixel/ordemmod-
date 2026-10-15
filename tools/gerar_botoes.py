#!/usr/bin/env python3
"""Gera os sprites de botoes e sliders do estilo do mod (vidro escuro, borda fina, cantos de sangue no hover).
Saida: src/main/resources/assets/minecraft/textures/gui/sprites/widget/*.png (+ .mcmeta de nine-slice).
No 26.x os botoes do jogo sao sprites do atlas gui; este script substitui o que o EstiloTexturas fazia em memoria no Forge.
Uso: python tools/gerar_botoes.py   (precisa do Pillow)"""
import json, os
from PIL import Image

SAIDA = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "minecraft",
                     "textures", "gui", "sprites", "widget")
BORDA = (0x34, 0x34, 0x3F, 255)
SANGUE = (0xB0, 0x17, 0x2B, 255)
SANGUE_CLARO = (0xE0, 0x26, 0x3E, 255)

def argb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255, (h >> 24) & 255)

def caixa(w, h, fundo, borda, canto=None):
    img = Image.new("RGBA", (w, h))
    px = img.load()
    for y in range(h):
        for x in range(w):
            c = fundo
            bx = x < 1 or x >= w - 1
            by = y < 1 or y >= h - 1
            if bx or by:
                c = borda
            if canto and ((bx and (y < 4 or y >= h - 4)) or (by and (x < 4 or x >= w - 4))):
                c = canto
            px[x, y] = c
    return img

def salvar(nome, img, largura, altura, borda=3):
    img.save(os.path.join(SAIDA, nome + ".png"))
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": largura, "height": altura, "border": borda}}}
    with open(os.path.join(SAIDA, nome + ".png.mcmeta"), "w") as f:
        json.dump(meta, f, indent=2)

os.makedirs(SAIDA, exist_ok=True)
salvar("button", caixa(200, 20, argb(0xC0141419), BORDA), 200, 20)
salvar("button_highlighted", caixa(200, 20, argb(0xE63A0D15), SANGUE, SANGUE_CLARO), 200, 20)
salvar("button_disabled", caixa(200, 20, argb(0x900E0E12), argb(0xFF24242C)), 200, 20)
salvar("slider", caixa(200, 20, argb(0xC0141419), BORDA), 200, 20)
salvar("slider_highlighted", caixa(200, 20, argb(0xE63A0D15), SANGUE, SANGUE_CLARO), 200, 20)
salvar("slider_handle", caixa(8, 20, argb(0xF0701020), SANGUE), 8, 20)
salvar("slider_handle_highlighted", caixa(8, 20, SANGUE, argb(0xFFFFD0D6)), 8, 20)
print("ok:", os.path.abspath(SAIDA))
