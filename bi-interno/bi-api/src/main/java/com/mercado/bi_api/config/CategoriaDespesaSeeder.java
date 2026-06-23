package com.mercado.bi_api.config;

import com.mercado.bi_api.entity.CategoriaDespesa;
import com.mercado.bi_api.entity.CentroCusto;
import com.mercado.bi_api.repository.CategoriaDespesaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Popula as categorias de despesa padrao (lista definida pelo dono do mercado)
 * na primeira inicializacao, com palavras-chave para o motor de sugestao automatica.
 */
@Component
@RequiredArgsConstructor
public class CategoriaDespesaSeeder implements CommandLineRunner {

    private final CategoriaDespesaRepository categoriaDespesaRepository;

    @Override
    public void run(String... args) {
        if (categoriaDespesaRepository.count() > 0) {
            return;
        }

        List<CategoriaDespesa> categorias = List.of(
                // ===== FIXAS =====
                categoria("Aluguel", CentroCusto.FIXA, "aluguel,locacao"),
                categoria("Seguros", CentroCusto.FIXA, "seguro,seguros"),
                categoria("Consórcios", CentroCusto.FIXA, "consorcio"),
                categoria("Conta Água", CentroCusto.FIXA, "agua,saneamento,compesa"),
                categoria("Energia", CentroCusto.FIXA, "energia,celpe,luz,neoenergia"),
                categoria("Internet", CentroCusto.FIXA, "internet,wifi,banda larga"),
                categoria("Telefonia", CentroCusto.FIXA, "telefone,celular,claro,vivo,tim,oi"),
                categoria("Salários Pessoal", CentroCusto.FIXA, "salario,folha de pagamento,pessoal"),
                categoria("Colaboradores Terceiros", CentroCusto.FIXA, "terceirizado,terceiros"),
                categoria("Pró-labore", CentroCusto.FIXA, "pro labore,prolabore"),
                categoria("Encargos INSS", CentroCusto.FIXA, "inss"),
                categoria("Encargos FGTS", CentroCusto.FIXA, "fgts"),
                categoria("Honorários Contabilidade", CentroCusto.FIXA, "contador,contabilidade,honorario contabil"),
                categoria("Consultoria Jurídica", CentroCusto.FIXA, "advogado,juridico,consultoria juridica"),
                categoria("Sistema", CentroCusto.FIXA, "sistema,software,licenca,uniplus"),
                categoria("Manutenção Predial", CentroCusto.FIXA, "manutencao predial,predial,reforma"),
                categoria("Manutenção Equipamentos", CentroCusto.FIXA, "manutencao equipamento,conserto,equipamento"),

                // ===== OPERACIONAIS =====
                categoria("Taxas Cartão", CentroCusto.OPERACIONAL, "taxa cartao,maquininha,adquirente"),
                categoria("Marketing", CentroCusto.OPERACIONAL, "marketing,propaganda,anuncio,panfleto"),
                categoria("Fretes", CentroCusto.OPERACIONAL, "frete,transporte,entrega"),
                categoria("Combustíveis", CentroCusto.OPERACIONAL, "combustivel,gasolina,diesel,posto,alcool,etanol"),
                categoria("Alimentação", CentroCusto.OPERACIONAL, "alimentacao,almoco,lanche,refeicao"),
                categoria("Impostos Federais", CentroCusto.OPERACIONAL, "imposto federal,darf,simples nacional,pis,cofins"),
                categoria("Impostos Estaduais", CentroCusto.OPERACIONAL, "imposto estadual,icms"),
                categoria("Material Consumo", CentroCusto.OPERACIONAL, "material de consumo,material de escritorio,papelaria"),
                categoria("Embalagens", CentroCusto.OPERACIONAL, "embalagem,sacola,saco plastico"),

                // ===== VARIÁVEIS =====
                categoria("Doação", CentroCusto.VARIAVEL, "doacao"),
                categoria("Patrocínios", CentroCusto.VARIAVEL, "patrocinio")
        );

        categoriaDespesaRepository.saveAll(categorias);
    }

    private CategoriaDespesa categoria(String nome, CentroCusto centroCusto, String palavrasChave) {
        return CategoriaDespesa.builder()
                .nome(nome)
                .centroCusto(centroCusto)
                .palavrasChave(palavrasChave)
                .build();
    }
}
