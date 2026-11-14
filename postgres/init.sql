-- Banco da loja E-Fecaf Global: roda automaticamente na primeira vez que o Postgres sobe.

CREATE TABLE produto (
    id         BIGSERIAL PRIMARY KEY,
    nome       VARCHAR(200)   NOT NULL,
    descricao  TEXT           NOT NULL,
    categoria  VARCHAR(50)    NOT NULL,
    preco      NUMERIC(10, 2) NOT NULL,
    estoque    INTEGER        NOT NULL
);
-- ATENÇÃO: de propósito NÃO existe índice em "nome"/"descricao".
-- A correção está em otimizacao/indices.sql (Etapa 8.2 do README).

CREATE TABLE pedido (
    id          BIGSERIAL PRIMARY KEY,
    produto_id  BIGINT         NOT NULL REFERENCES produto (id),
    quantidade  INTEGER        NOT NULL,
    valor       NUMERIC(12, 2) NOT NULL,
    criado_em   TIMESTAMP      NOT NULL DEFAULT now()
);

-- 50 mil produtos gerados automaticamente
INSERT INTO produto (nome, descricao, categoria, preco, estoque)
SELECT
    tipos[1 + (g % 10)] || ' ' || marcas[1 + ((g / 10) % 8)] || ' Modelo ' || g,
    'Produto ' || g || ' da linha ' || marcas[1 + ((g / 10) % 8)] ||
        ' na categoria ' || tipos[1 + (g % 10)] ||
        '. Ideal para a Black Friday E-Fecaf Global, com garantia de 12 meses e frete para todo o Brasil.',
    tipos[1 + (g % 10)],
    round((19.90 + random() * 4980)::numeric, 2),
    1000000
FROM generate_series(1, 50000) AS g,
     (SELECT ARRAY['notebook','smartphone','monitor','teclado','mouse',
                   'fone','cadeira','tv','geladeira','tablet'] AS tipos,
             ARRAY['Fecaf','Orion','Vega','Atlas','Nexus','Prisma','Zenite','Aurora'] AS marcas) AS listas;

ANALYZE produto;
