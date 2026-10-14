package com.exemplo.ordemmod;

/**
 * PROVISORIO (etapa 6a): so escreve no log. Na 6b isto volta a abrir a TelaTeste (o dado) por cima da tela atual,
 * como no Forge. So existe no cliente.
 */
public final class ClienteTeste {
    private ClienteTeste() {
    }

    public static void abrir(PacoteTeste dados) {
        System.out.println("[Ordem cliente] teste recebido: " + Pericia.values()[Math.max(0,
                Math.min(dados.pericia(), Pericia.values().length - 1))].getNome() + " " + dados.total() + "/" + dados.dt()
                + (dados.sucesso() ? " sucesso" : " falha") + (dados.critico() ? " CRITICO" : "")
                + " - a tela do dado ainda nao foi portada (etapa 6b)");
    }
}
