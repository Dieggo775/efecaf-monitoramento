-- Volta ao estado "antes" (sem índices), para repetir os testes do zero.
-- Aplicar:  docker compose exec -T postgres psql -U loja -d loja < otimizacao/remover-indices.sql
DROP INDEX IF EXISTS idx_produto_nome_trgm;
DROP INDEX IF EXISTS idx_produto_descricao_trgm;
DROP INDEX IF EXISTS idx_produto_preco;
ANALYZE produto;
