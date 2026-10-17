#!/usr/bin/env python3
"""
Gera os ícones das habilidades das trilhas de COMBATENTE a partir das artes originais.
Sem borda própria: a moldura padrão é desenhada pela ficha (TelaFicha.desenharMoldura).

Uso (na pasta do projeto):
    python3 tools/gerar_icones_trilhas_combatente.py caminho/para/"Trilhas combatente"

A pasta de origem tem uma subpasta por trilha com as artes (.jpg 1024x1024).
Cada arte é ligada ao id da habilidade na tabela ICONES abaixo; o resultado (128x128, quadrado, sem borda) vai para
src/main/resources/assets/ordemmod/textures/gui/poderes/<id>.png
Precisa do Pillow: pip install pillow
"""
import os
import sys
import unicodedata

from PIL import Image

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

FINAL = 128  # tamanho do PNG entregue


def norm(s):
    return unicodedata.normalize("NFC", s).casefold()


def achar(raiz, pasta, nome):
    """Acha o .jpg ignorando maiúsculas (o zip tem nomes meio bagunçados)."""
    for d in os.listdir(raiz):
        if norm(d) == norm(pasta):
            for f in os.listdir(os.path.join(raiz, d)):
                if norm(f) == norm(nome) + ".jpg":
                    return os.path.join(raiz, d, f)
    raise FileNotFoundError(f"{pasta}/{nome}.jpg")


def main():
    if len(sys.argv) != 2:
        print(__doc__)
        sys.exit(1)
    os.makedirs(SAIDA, exist_ok=True)
    for (pasta, nome), ident in ICONES.items():
        img = Image.open(achar(sys.argv[1], pasta, nome)).convert("RGBA").resize((FINAL, FINAL), Image.LANCZOS)
        img.save(os.path.join(SAIDA, ident + ".png"), optimize=True)
        print("ok", ident)


if __name__ == "__main__":
    main()
