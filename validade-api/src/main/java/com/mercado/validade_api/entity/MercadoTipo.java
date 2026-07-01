package com.mercado.validade_api.entity;

/**
 * Lançamentos manuais da conta do mercado. CREDITO_MANUAL soma ao saldo (+);
 * os usos (avaria da loja, troca de preço, outro) consomem o crédito (-).
 */
public enum MercadoTipo {
    CREDITO_MANUAL(1),
    AVARIA_LOJA(-1),
    TROCA_PRECO(-1),
    REBAIXA_PARCERIA(-1), // cobertura da rebaixa parceria que o mercado decidiu bancar (desconta bonificação)
    REBAIXA_VALIDADE(-1), // rebaixa do semáforo de validade que o mercado decidiu bancar (não é rebaixa parceria)
    OUTRO(-1);

    private final int sinal;

    MercadoTipo(int sinal) {
        this.sinal = sinal;
    }

    public int getSinal() {
        return sinal;
    }
}
