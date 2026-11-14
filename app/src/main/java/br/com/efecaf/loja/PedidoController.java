package br.com.efecaf.loja;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class PedidoController {

    public record ItemRequest(long produtoId, int quantidade) {
    }

    private final JdbcTemplate jdbc;
    private final long latenciaPagamentoMs;

    public PedidoController(JdbcTemplate jdbc,
                            @Value("${loja.pagamento.latencia-ms}") long latenciaPagamentoMs) {
        this.jdbc = jdbc;
        this.latenciaPagamentoMs = latenciaPagamentoMs;
    }

    /** Adicionar ao carrinho: confere se o produto existe e tem estoque. */
    @PostMapping("/carrinho")
    public Map<String, Object> adicionarAoCarrinho(@RequestBody ItemRequest item) {
        List<Integer> estoque = jdbc.queryForList(
                "SELECT estoque FROM produto WHERE id = ?", Integer.class, item.produtoId());
        if (estoque.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
        }
        boolean disponivel = estoque.get(0) >= item.quantidade();
        return Map.of("produtoId", item.produtoId(), "quantidade", item.quantidade(), "disponivel", disponivel);
    }

    /**
     * Finalizar compra. GARGALO PROPOSITAL: a transação (e a conexão do banco)
     * fica aberta enquanto a loja "conversa" com o gateway de pagamento.
     * Com o pool de 5 conexões, poucos checkouts simultâneos já travam a loja toda.
     */
    @PostMapping("/checkout")
    @Transactional
    public Map<String, Object> checkout(@RequestBody ItemRequest item) throws InterruptedException {
        int qtd = Math.max(item.quantidade(), 1);
        List<BigDecimal> preco = jdbc.queryForList(
                "SELECT preco FROM produto WHERE id = ?", BigDecimal.class, item.produtoId());
        if (preco.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
        }

        int atualizados = jdbc.update(
                "UPDATE produto SET estoque = estoque - ? WHERE id = ? AND estoque >= ?",
                qtd, item.produtoId(), qtd);
        if (atualizados == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sem estoque");
        }

        // Simula a chamada ao gateway de pagamento (segurando a conexão do banco).
        Thread.sleep(latenciaPagamentoMs);

        BigDecimal valor = preco.get(0).multiply(BigDecimal.valueOf(qtd));
        Long pedidoId = jdbc.queryForObject(
                "INSERT INTO pedido (produto_id, quantidade, valor) VALUES (?, ?, ?) RETURNING id",
                Long.class, item.produtoId(), qtd, valor);

        return Map.of("pedidoId", pedidoId, "valor", valor, "status", "APROVADO");
    }
}
