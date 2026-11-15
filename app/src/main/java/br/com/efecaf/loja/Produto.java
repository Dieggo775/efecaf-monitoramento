package br.com.efecaf.loja;

import java.math.BigDecimal;

public record Produto(long id, String nome, String categoria, BigDecimal preco, int estoque) {
}
