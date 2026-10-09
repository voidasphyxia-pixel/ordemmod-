package com.exemplo.ordemmod.marcado;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * O roteiro da cena do Marcado, lido de assets/ordemmod/marcado/marcado.json (dentro do jar, então
 * cliente E servidor leem o mesmo arquivo). Java puro, sem nada do Minecraft: dá para testar à parte.
 * Os textos NÃO ficam aqui: o json só tem chaves, e as falas ficam em lang/pt_br.json.
 */
public final class RoteiroMarcado {
    public static final String CAMINHO = "/assets/ordemmod/marcado/marcado.json";

    public static final class Opcao {
        public String texto;      // chave de tradução da opção
        public String reacao;     // chave de tradução da fala da entidade depois da escolha (pode ser null)
        public String efeito;     // id de efeito disparado ao escolher (pode ser null)
        public Map<String, Integer> tags = new LinkedHashMap<>(); // pontos ocultos; nunca aparecem na tela
    }

    public static final class Passo {
        public String tipo;       // fala | pergunta | silencio | revelacao | fim
        public String id;         // só pergunta: "p01"...
        public String texto;      // chave de tradução (fala ou enunciado da pergunta)
        public float duracao = 0; // segundos extras de pausa depois da fala (ou duração do silêncio)
        public String efeito;     // efeito ao começar o passo (pode ser null)
        public String se;         // só roda se a resposta bater: "p01=0", "p01=0|2" (pode ser null = sempre)
        public String som;        // som único ao começar o passo (pode ser null), ex.: "tum"
        public List<Opcao> opcoes = new ArrayList<>();
    }

    public final List<Passo> passos = new ArrayList<>();

    public int totalPerguntas() {
        int n = 0;
        for (Passo p : passos) {
            if ("pergunta".equals(p.tipo)) {
                n++;
            }
        }
        return n;
    }

    /** As perguntas na ordem em que aparecem. */
    public List<Passo> perguntas() {
        List<Passo> l = new ArrayList<>();
        for (Passo p : passos) {
            if ("pergunta".equals(p.tipo)) {
                l.add(p);
            }
        }
        return l;
    }

    /** Soma as tags das opções escolhidas. Devolve null se a escolha for inválida (o servidor não confia no cliente). */
    public Map<String, Integer> somarTags(int[] escolhas) {
        List<Passo> perguntas = perguntas();
        if (escolhas == null || escolhas.length != perguntas.size()) {
            return null;
        }
        Map<String, Integer> tags = new LinkedHashMap<>();
        for (int i = 0; i < escolhas.length; i++) {
            List<Opcao> ops = perguntas.get(i).opcoes;
            if (escolhas[i] < 0 || escolhas[i] >= ops.size()) {
                return null;
            }
            ops.get(escolhas[i]).tags.forEach((k, v) -> tags.merge(k, v, Integer::sum));
        }
        return tags;
    }

    private static RoteiroMarcado cache;

    public static synchronized RoteiroMarcado carregar() {
        if (cache == null) {
            try (InputStream in = RoteiroMarcado.class.getResourceAsStream(CAMINHO)) {
                if (in == null) {
                    throw new IllegalStateException("Faltou " + CAMINHO);
                }
                cache = ler(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Não consegui ler o roteiro do Marcado", e);
            }
        }
        return cache;
    }

    static RoteiroMarcado ler(JsonObject raiz) {
        RoteiroMarcado r = new RoteiroMarcado();
        for (JsonElement e : raiz.getAsJsonArray("passos")) {
            JsonObject o = e.getAsJsonObject();
            Passo p = new Passo();
            p.tipo = str(o, "tipo");
            p.id = str(o, "id");
            p.texto = str(o, "texto");
            p.efeito = str(o, "efeito");
            p.se = str(o, "se");
            p.som = str(o, "som");
            p.duracao = o.has("duracao") ? o.get("duracao").getAsFloat() : 0f;
            if (o.has("opcoes")) {
                for (JsonElement oe : o.getAsJsonArray("opcoes")) {
                    JsonObject oo = oe.getAsJsonObject();
                    Opcao op = new Opcao();
                    op.texto = str(oo, "texto");
                    op.reacao = str(oo, "reacao");
                    op.efeito = str(oo, "efeito");
                    if (oo.has("tags")) {
                        for (Map.Entry<String, JsonElement> t : oo.getAsJsonObject("tags").entrySet()) {
                            op.tags.put(t.getKey(), t.getValue().getAsInt());
                        }
                    }
                    p.opcoes.add(op);
                }
            }
            r.passos.add(p);
        }
        return r;
    }

    private static String str(JsonObject o, String chave) {
        return o.has(chave) && !o.get(chave).isJsonNull() ? o.get(chave).getAsString() : null;
    }
}
