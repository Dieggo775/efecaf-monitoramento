-- ETAPA 8.2: índices que acabam com o full scan da busca.
-- Aplicar:  docker compose exec -T postgres psql -U loja -d loja < otimizacao/indices.sql
CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- Índices de trigramas: permitem LIKE '%termo%' sem ler a tabela inteira
CREATE INDEX IF NOT EXISTS idx_produto_nome_trgm      ON produto USING gin (lower(nome) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_produto_descricao_trgm ON produto USING gin (lower(descricao) gin_trgm_ops);

-- Índice por preço: a busca ordena por preço e para nos 20 primeiros
CREATE INDEX IF NOT EXISTS idx_produto_preco ON produto (preco);

ANALYZE produto;
