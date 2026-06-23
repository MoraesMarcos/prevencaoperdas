package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.AcompanhamentoResponseDTO;
import com.mercado.validade_api.service.AcompanhamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/acompanhamento")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // MVP: liberar app/web. Em produção, restringir a origem.
public class AcompanhamentoController {

    private final AcompanhamentoService acompanhamentoService;

    @GetMapping
    public List<AcompanhamentoResponseDTO> listar(
            @RequestParam(defaultValue = "false") boolean incluirNovos) {
        return acompanhamentoService.listar(incluirNovos);
    }
}
