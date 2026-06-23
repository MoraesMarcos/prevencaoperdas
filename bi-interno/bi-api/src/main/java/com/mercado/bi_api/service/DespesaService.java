package com.mercado.bi_api.service;

import com.mercado.bi_api.dto.DespesaRequestDTO;
import com.mercado.bi_api.dto.DespesaResponseDTO;
import com.mercado.bi_api.entity.CategoriaDespesa;
import com.mercado.bi_api.entity.Despesa;
import com.mercado.bi_api.repository.CategoriaDespesaRepository;
import com.mercado.bi_api.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final CategoriaDespesaRepository categoriaDespesaRepository;

    @Transactional
    public DespesaResponseDTO registrar(DespesaRequestDTO dto) {
        CategoriaDespesa categoria = categoriaDespesaRepository.findById(dto.getCategoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Categoria nao encontrada"));

        Despesa despesa = despesaRepository.save(Despesa.builder()
                .descricao(dto.getDescricao())
                .valor(dto.getValor())
                .data(dto.getData())
                .categoria(categoria)
                .recorrente(dto.isRecorrente())
                .criadoPor(dto.getCriadoPor())
                .build());

        return paraDTO(despesa);
    }

    @Transactional(readOnly = true)
    public List<DespesaResponseDTO> listarPorPeriodo(LocalDate inicio, LocalDate fim) {
        return despesaRepository.findByDataBetweenOrderByDataDesc(inicio, fim)
                .stream().map(this::paraDTO).toList();
    }

    @Transactional
    public void remover(UUID id) {
        if (!despesaRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa nao encontrada");
        }
        despesaRepository.deleteById(id);
    }

    private DespesaResponseDTO paraDTO(Despesa despesa) {
        return DespesaResponseDTO.builder()
                .id(despesa.getId())
                .descricao(despesa.getDescricao())
                .valor(despesa.getValor())
                .data(despesa.getData())
                .categoriaNome(despesa.getCategoria().getNome())
                .centroCusto(despesa.getCategoria().getCentroCusto().name())
                .recorrente(despesa.isRecorrente())
                .criadoPor(despesa.getCriadoPor())
                .build();
    }
}
