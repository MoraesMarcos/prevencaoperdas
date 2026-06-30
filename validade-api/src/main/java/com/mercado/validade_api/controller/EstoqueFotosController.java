package com.mercado.validade_api.controller;

import com.mercado.validade_api.dto.EstoqueFotoDTO;
import com.mercado.validade_api.service.EstoqueFotosService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estoque-fotos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class EstoqueFotosController {

    private final EstoqueFotosService estoqueFotosService;

    @GetMapping
    public List<EstoqueFotoDTO> listar() {
        return estoqueFotosService.listar();
    }
}
