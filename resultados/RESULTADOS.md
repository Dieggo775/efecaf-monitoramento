# Resultados dos testes — 13/11/2026

Testes rodados no servidor 192.168.15.100, com o mesmo roteiro do README (Locust → loja → Prometheus).
Os horários são de Brasília.

## Tabela antes × depois (Teste 2: PicoBlackFriday, 800 usuários no pico)

| Métrica | Antes (pool 5, 1 CPU) | 8.1 (pool 20, 2 CPUs) | 8.2 (+ índices) |
|---|---|---|---|
| Taxa de erros (teste inteiro, Locust) | **31.21%** | 27.12% | **0.0%** |
| Taxa de erros máxima no pico (servidor) | 54.05% | 49.53% | 0.0% |
| p95 visto pelo cliente (Locust) | **4.8 s** | 5.2 s | **0.31 s** |
| p95 máximo no servidor (Prometheus) | 2.19 s | 2.91 s | 0.32 s |
| Req/s máximas atendidas | **133** | 124 | **393** |
| Requisições atendidas no teste | 21,517 | 20,948 | 47,869 |
| Pool do banco: em uso / fila (máx.) | 5 de 5 / 194 | 20 de 20 / 179 | 20 de 20 / 29 |
| CPU do Postgres no pico | 189.19% | **322.70%** | 175.27% |
| Alertas disparados | LatenciaAlta, LatenciaCritica, PoolDoBancoSaturado, TaxaDeErrosAlta | LatenciaAlta, LatenciaCritica, PoolDoBancoSaturado, TaxaDeErrosAlta | **nenhum** |

**Conclusões para os slides:**
1. Com carga normal (Teste 1, 50 usuários), a loja está saudável: 0% de erros, p95 de 0.34 s e 24 req/s.
2. No pico da Black Friday, a loja falha: 31.21% das requisições dão erro, o pool fica 5/5 com 194 requisições na fila e 4 alertas disparam.
3. **O primeiro palpite (pool 20 + 2 CPUs) quase não ajuda**: os erros vão de 31.21% para 27.12%. O monitoramento mostra o motivo: o Postgres vai a 322.70% de CPU. O gargalo é a busca sem índice (cerca de 215 ms de CPU por busca).
4. **Com os índices, o problema some**: 0% de erros, p95 de 0.31 s, nenhum alerta e 2.9× mais requisições por segundo.

## Teste 3: limite da plataforma (estado "antes")

| Usuários | Req/s | Falhas | p95 (cliente) |
|---|---|---|---|
| 220 | 45.2 | 0.0% | 0.7 s |
| 420 | 72.9 | 6.2% | 2.2 s |
| 620 | 119.6 | 44.6% | 2.7 s |
| 820 | 124.9 | 54.8% | 4.3 s |
| 1020 | 126.6 | 54.3% | 5.6 s |
| 1220 | 127.5 | 53.2% | 6.8 s |
| 1420 | 126.1 | 52.2% | 8.4 s |
| 1500 | 122.0 | 55.5% | 10.0 s |

**Limite encontrado: entre 220 e 420 usuários simultâneos.** Com 420 usuários, os erros passam de
5% (6.2%) e o p95 chega a 2.2 s. A loja não passa de cerca de 130 req/s, por mais usuários que entrem.
Com 1500 usuários, até o health check externo falhou e disparou **SiteForaDoAr**: para o cliente, o site "caiu".

## Links para os prints (abra, espere carregar e use Win+Shift+S)

Os links do Grafana já abrem no horário de cada teste, em tela cheia. Se pedir login: `admin` / `admin123`.

| Print | Horário | Link |
|---|---|---|
| Dashboard com tráfego normal (Teste 1) | 20:04–20:07 | [abrir](http://192.168.15.100:3002/d/efecaf-saude/e-fecaf-global-saude-da-plataforma?from=1794610999062&to=1794611269063&refresh=&kiosk) |
| Grafana no pico da Black Friday, antes (Teste 2) | 20:08–20:13 | [abrir](http://192.168.15.100:3002/d/efecaf-saude/e-fecaf-global-saude-da-plataforma?from=1794611302253&to=1794611697071&refresh=&kiosk) |
| Limite no teste de estresse (Teste 3) | 20:15–20:20 | [abrir](http://192.168.15.100:3002/d/efecaf-saude/e-fecaf-global-saude-da-plataforma?from=1794611701065&to=1794612036495&refresh=&kiosk) |
| Pico depois da 8.1 (pool 20) | 20:22–20:27 | [abrir](http://192.168.15.100:3002/d/efecaf-saude/e-fecaf-global-saude-da-plataforma?from=1794612136178&to=1794612530455&refresh=&kiosk) |
| Pico depois da 8.2 (índices) | 20:29–20:34 | [abrir](http://192.168.15.100:3002/d/efecaf-saude/e-fecaf-global-saude-da-plataforma?from=1794612551003&to=1794612946054&refresh=&kiosk) |
| **Lado a lado: antes → 8.1 → 8.2** (a melhor imagem para o slide) | 20:08–20:34 | [abrir](http://192.168.15.100:3002/d/efecaf-saude/e-fecaf-global-saude-da-plataforma?from=1794611272253&to=1794612946054&refresh=&kiosk) |
| Alertas disparando ao longo dos testes (Prometheus) | 20:04–20:34 | [abrir](http://192.168.15.100:9090/graph?g0.expr=ALERTS%7Balertstate%3D%22firing%22%7D&g0.tab=0&g0.range_input=35m&g0.end_input=2026-11-13+23%3A36%3A46) |

Prints do Locust: abra os arquivos `*_relatorio_locust.html` desta pasta no navegador. Cada um tem
a tabela de estatísticas e os gráficos (equivalentes à aba Charts).

## Arquivos desta pasta

| Arquivo | O que é |
|---|---|
| `teste2_black_friday_antes_estatisticas.csv` | O CSV do Locust pedido no Teste 2 (Download Data) |
| `*_relatorio_locust.html` | Relatório completo de cada teste, com gráficos |
| `*_estatisticas.csv` / `*_falhas.csv` | Estatísticas e erros de cada teste |
| `resultados.json` | Todos os números medidos (usados nesta página) |

