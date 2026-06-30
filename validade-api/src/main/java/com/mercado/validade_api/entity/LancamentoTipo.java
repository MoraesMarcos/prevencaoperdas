package com.mercado.validade_api.entity;

/**
 * Tipo do lançamento e seu sinal no saldo que o fornecedor deve:
 * TROCA, REBAIXA, AVARIA aumentam (+); NEGOCIACAO e PAGAMENTO reduzem (-).
 */
public enum LancamentoTipo {
    TROCA(1),
    REBAIXA(1),
    AVARIA(1),
    NEGOCIACAO(-1),
    PAGAMENTO(-1),
    BONIFICACAO(-1); // crédito de bonificação aplicado para abater trocas (registro finalizado)

    private final int sinal;

    LancamentoTipo(int sinal) {
        this.sinal = sinal;
    }

    public int getSinal() {
        return sinal;
    }
}
