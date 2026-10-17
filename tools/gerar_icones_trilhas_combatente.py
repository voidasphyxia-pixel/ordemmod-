#!/usr/bin/env python3
"""
Gera os ícones das habilidades das trilhas de COMBATENTE com a borda estilizada "ladrão"
(ferro escuro desgastado com filete de bronze, sem enfeites).

Uso (na pasta do projeto):
    python3 tools/gerar_icones_trilhas_combatente.py caminho/para/"Trilhas combatente"

A pasta de origem tem uma subpasta por trilha com as artes (.jpg 1024x1024).
Cada arte é ligada ao id da habilidade na tabela ICONES abaixo; o resultado (128x128, cantos
arredondados, fundo transparente) vai para
src/main/resources/assets/ordemmod/textures/gui/poderes/<id>.png
Precisa do Pillow: pip install pillow
"""
import os
import sys
import unicodedata

from PIL import Image, ImageChops, ImageDraw, ImageFilter

SAIDA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                     "assets", "ordemmod", "textures", "gui", "poderes")

# (pasta da trilha, nome do arquivo da arte sem extensão) -> id da habilidade
ICONES = {
    ("Aniquilador", "A Favorita"): "a_favorita",
    ("Aniquilador", "Técnica Secreta"): "tecnica_secreta",
    ("Aniquilador", "Técnica Sublime"): "tecnica_sublime",
    ("Aniquilador", "Máquina de matar"): "maquina_de_guerra",
    ("Comandante de campo", "Inspirar Confiança"): "inspirar_confianca",
    ("Comandante de campo", "Estrategista"): "estrategista",
    ("Comandante de campo", "Brecha na guarda"): "brecha_na_guarda",
    ("Comandante de campo", "Oficial Comandante"): "oficial_comandante",
    ("Guerreiro", "Técnica Letal"): "tecnica_letal",
    ("Guerreiro", "Revidar"): "revidar",
    ("Guerreiro", "Força opressora"): "forca_opressora",
    ("Guerreiro", "Potência Maxima"): "potencia_maxima",
    ("Operações especiais", "Iniciativa Aprimorada"): "iniciativa_aprimorada",
    ("Operações especiais", "Ataque extra"): "ataque_extra",
    ("Operações especiais", "Surto de adrenalina"): "surto_de_adrenalina",
    ("Operações especiais", "Sempre Alerta"): "sempre_alerta",
    ("Tropa de Choque", "Casca Grossa"): "casca_grossa",
    ("Tropa de Choque", "Cai dentro"): "cai_dentro",
    ("Tropa de Choque", "Duro de matar"): "duro_de_matar",
    ("Tropa de Choque", "Inquebrável"): "inquebravel",
}

FINAL = 128          # tamanho do PNG entregue
S = 4                # supersampling
N = FINAL * S        # 512: tela de trabalho
RAIO = 18 * S        # cantos arredondados
BORDA = 11 * S       # largura da moldura

CONTORNO = (14, 11, 18, 255)
FERRO_CLARO = (92, 94, 106, 255)
FERRO_MEDIO = (54, 55, 66, 255)
FERRO_ESCURO = (28, 28, 36, 255)
BRONZE_CLARO = (226, 178, 84, 255)
BRONZE = (168, 120, 48, 255)
BRONZE_ESCURO = (96, 62, 22, 255)
LAMINA = (206, 212, 220, 255)
LAMINA_SOMBRA = (120, 128, 142, 255)


def norm(s):
    s = unicodedata.normalize("NFC", s)
    return s.casefold()


def achar(raiz, pasta, nome):
    """Acha o .jpg ignorando maiúsculas e acentos soltos (o zip tem nomes meio bagunçados)."""
    alvo_pasta, alvo_nome = norm(pasta), norm(nome) + ".jpg"
    for d in os.listdir(raiz):
        if norm(d) == alvo_pasta:
            for f in os.listdir(os.path.join(raiz, d)):
                if norm(f) == alvo_nome:
                    return os.path.join(raiz, d, f)
    raise FileNotFoundError(f"{pasta}/{nome}.jpg")


def retangulo_arredondado(caixa, raio, c):
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).rounded_rectangle(caixa, raio, fill=c)
    return m


def textura_ferro():
    """Faixa de ferro com degradê diagonal e riscos de desgaste (determinístico)."""
    img = Image.new("RGBA", (N, N), FERRO_MEDIO)
    d = ImageDraw.Draw(img)
    for i in range(N * 2):
        t = i / (N * 2)
        v = int(40 + 36 * (1 - abs(t - 0.28) * 2.2 if abs(t - 0.28) < 0.45 else 0))
        d.line([(i, 0), (i - N, N)], fill=(v, v + 1, v + 12, 255), width=2)
    for k in range(46):  # riscos finos
        x = (k * 97) % N
        y = (k * 53) % N
        d.line([(x, y), (x + 26 + (k % 5) * 6, y - 10 - (k % 3) * 5)], fill=(104, 106, 118, 140), width=1)
    return img


def moldura():
    """Só a moldura (RGBA, miolo transparente) já com o recorte arredondado."""
    externo = retangulo_arredondado([0, 0, N - 1, N - 1], RAIO, 255)
    interno = retangulo_arredondado([BORDA, BORDA, N - 1 - BORDA, N - 1 - BORDA], RAIO - 6 * S, 255)

    ferro = textura_ferro()
    d = ImageDraw.Draw(ferro)
    # linha de bronze junto da arte e filete claro por fora
    d.rounded_rectangle([BORDA - 3 * S, BORDA - 3 * S, N - 1 - BORDA + 3 * S, N - 1 - BORDA + 3 * S],
                        RAIO - 4 * S, outline=BRONZE, width=2 * S)
    d.rounded_rectangle([BORDA - 4 * S, BORDA - 4 * S, N - 1 - BORDA + 4 * S, N - 1 - BORDA + 4 * S],
                        RAIO - 3 * S, outline=CONTORNO, width=S)
    d.rounded_rectangle([3 * S, 3 * S, N - 1 - 3 * S, N - 1 - 3 * S], RAIO - 3 * S, outline=FERRO_CLARO, width=S)
    d.rounded_rectangle([0, 0, N - 1, N - 1], RAIO, outline=CONTORNO, width=2 * S)

    banda = ImageChops.subtract(externo, interno)
    # adagas/gazua passam um pouco para dentro da arte: usa a máscara do ferro + enfeites (alpha dos enfeites)
    return ferro, banda, externo, interno


def vinheta(arte):
    """Escurece levemente as bordas da arte para ela 'entrar' na moldura."""
    sombra = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    m = Image.new("L", (N, N), 0)
    ImageDraw.Draw(m).rounded_rectangle([BORDA, BORDA, N - 1 - BORDA, N - 1 - BORDA], RAIO - 6 * S, outline=150, width=7 * S)
    m = m.filter(ImageFilter.GaussianBlur(5 * S))
    sombra.putalpha(m)
    return Image.alpha_composite(arte, sombra)


def montar(caminho_arte):
    ferro, banda, externo, interno = moldura()
    arte = Image.open(caminho_arte).convert("RGB").resize((N - 2 * BORDA + 2 * S, N - 2 * BORDA + 2 * S), Image.LANCZOS)
    tela = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    tela.paste(arte, (BORDA - S, BORDA - S))
    tela.putalpha(interno)
    tela = vinheta(tela)

    # moldura por cima (ferro com enfeites); os enfeites que invadem a arte ficam visíveis
    cheio = ImageChops.lighter(banda, Image.new("L", (N, N), 0))
    base = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    base.paste(ferro, (0, 0), banda)
    resultado = Image.alpha_composite(tela, base)

    return resultado.resize((FINAL, FINAL), Image.LANCZOS)


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    raiz = sys.argv[1]
    os.makedirs(SAIDA, exist_ok=True)
    for (pasta, nome), ident in ICONES.items():
        img = montar(achar(raiz, pasta, nome))
        img.save(os.path.join(SAIDA, ident + ".png"), optimize=True)
        print("ok", ident)


if __name__ == "__main__":
    main()
