package com.exemplo.ordemmod;

/**
 * PROVISORIO (etapa 6a): so escreve no log. Na 6b isto volta a abrir a tela de criacao
 * (TelaMarcado quando comMarcado, senao TelaCriacao), como no Forge. So existe no cliente.
 */
public final class ClienteCriacao {
    private ClienteCriacao() {
    }

    public static void abrir(boolean comMarcado) {
        System.out.println("[Ordem cliente] servidor pediu a tela de criacao (com Marcado: " + comMarcado
                + ") - a tela ainda nao foi portada (etapa 6b)");
    }
}
