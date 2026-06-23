package com.mercado.bi_api.service;

import com.mercado.bi_api.entity.CategoriaDespesa;
import com.mercado.bi_api.repository.CategoriaDespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Optional;

/**
 * Motor de sugestao de categoria por palavras-chave (sem IA externa).
 * Cada CategoriaDespesa tem uma lista de palavras-chave; a primeira categoria
 * cuja palavra-chave aparece na descricao digitada vence.
 */
@Service
@RequiredArgsConstructor
public class CategorizacaoService {

    private final CategoriaDespesaRepository categoriaDespesaRepository;

    public Optional<CategoriaDespesa> sugerir(String descricao) {
        if (descricao == null || descricao.isBlank()) {
            return Optional.empty();
        }
        String descricaoNormalizada = normalizar(descricao);

        return categoriaDespesaRepository.findAll().stream()
                .filter(categoria -> categoria.getPalavrasChave() != null && !categoria.getPalavrasChave().isBlank())
                .filter(categoria -> Arrays.stream(categoria.getPalavrasChave().split(","))
                        .map(String::trim)
                        .filter(palavra -> !palavra.isEmpty())
                        .map(this::normalizar)
                        .anyMatch(descricaoNormalizada::contains))
                .findFirst();
    }

    private String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return semAcento.toLowerCase().trim();
    }
}
