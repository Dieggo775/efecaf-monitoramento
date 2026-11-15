package br.com.efecaf.loja;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProdutoController {

    static final RowMapper<Produto> MAPPER = (rs, i) -> new Produto(
            rs.getLong("id"),
            rs.getString("nome"),
            rs.getString("categoria"),
            rs.getBigDecimal("preco"),
            rs.getInt("estoque"));

    private final JdbcTemplate jdbc;

    public ProdutoController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Vitrine: lista paginada do catálogo. */
    @GetMapping("/produtos")
    public List<Produto> listar(@RequestParam(defaultValue = "0") int pagina,
                                @RequestParam(defaultValue = "20") int tamanho) {
        int t = Math.min(Math.max(tamanho, 1), 100);
        int offset = Math.max(pagina, 0) * t;
        return jdbc.query(
                "SELECT id, nome, categoria, preco, estoque FROM produto ORDER BY id LIMIT ? OFFSET ?",
                MAPPER, t, offset);
    }

    /** Página de detalhe do produto. */
    @GetMapping("/produtos/{id}")
    public Produto detalhe(@PathVariable long id) {
        List<Produto> r = jdbc.query(
                "SELECT id, nome, categoria, preco, estoque FROM produto WHERE id = ?", MAPPER, id);
        if (r.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado");
        }
        return r.get(0);
    }

    /**
     * Busca por texto. GARGALO PROPOSITAL: não existe índice na coluna "nome" nem
     * em "descricao", então o banco lê os 50 mil produtos a cada busca (full scan).
     */
    @GetMapping("/busca")
    public List<Produto> buscar(@RequestParam(defaultValue = "") String q) {
        String termo = "%" + q.toLowerCase() + "%";
        return jdbc.query(
                "SELECT id, nome, categoria, preco, estoque FROM produto " +
                "WHERE lower(nome) LIKE ? OR lower(descricao) LIKE ? " +
                "ORDER BY preco LIMIT 20",
                MAPPER, termo, termo);
    }
}
