package com.mercado.validade_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class GiroDTO {
    private int vendido30d;
    private int vendido90d;
    private double velocidade30; // unidades/dia nos ultimos 30 dias
    private double velocidade90; // unidades/dia nos ultimos 90 dias
}
