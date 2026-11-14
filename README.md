# E-Fecaf Global — Laboratório de Monitoramento

Loja virtual (Spring Boot + PostgreSQL com 50 mil produtos) monitorada com
Prometheus, Blackbox, Grafana e Alertmanager (Telegram), com testes de carga no Locust.
Só precisa do **Docker**. Tudo roda em containers.

Os resultados dos testes estão em [`resultados/`](resultados/RESULTADOS.md), o relatório em
[`relatorio/`](relatorio/) e a apresentação em [`apresentacao/`](apresentacao/).

---

## Etapa 1 — Conferir o Docker

```bash
docker --version
docker compose version
```

No servidor Ubuntu, o Docker já está instalado. Acesse tudo pelo navegador do PC usando o IP do
servidor (ex.: `http://192.168.15.100:3002`). Se não abrir: `sudo ufw allow 3002,8080,8089,9090,9093/tcp`.

## Etapa 2 — Estrutura

```
efecaf-monitoramento/
├── docker-compose.yml          ← liga todos os serviços
├── app/                        ← a loja (Spring Boot + Actuator)
├── postgres/init.sql           ← cria as tabelas e os 50 mil produtos
├── prometheus/prometheus.yml   ← o que coletar e de onde
├── prometheus/alertas.yml      ← as 7 regras de alerta
├── alertmanager/alertmanager.yml ← envio para o Telegram
├── blackbox/blackbox.yml       ← teste "o site está no ar?"
├── grafana/                    ← fonte de dados e dashboard prontos
├── locust/locustfile.py        ← clientes simulados e pico de Black Friday
```

Fluxo: a loja expõe métricas em `/actuator/prometheus` → o Prometheus coleta a cada 5s →
o Blackbox testa o site "de fora" → o Grafana desenha os gráficos → o Prometheus avalia os
alertas e avisa o Alertmanager → o Alertmanager manda no Telegram. O Locust simula os clientes.

**Gargalos colocados de propósito** (é o que os testes vão revelar):
1. Pool de só **5 conexões** com o banco para a loja inteira.
2. O **checkout segura a conexão** por 300 ms enquanto "fala" com o gateway de pagamento.
3. A **busca não tem índice**: lê os 50 mil produtos a cada consulta.

## Etapa 3 — Subir o ambiente

```bash
cd efecaf-monitoramento
docker compose up -d --build
```

A primeira vez leva de 5 a 10 minutos (baixa imagens e compila o Java).

```bash
docker compose ps
docker compose logs -f app
```

Espere aparecer `Started LojaApplication` e aperte Ctrl+C (a loja continua rodando).
Se der erro no build, as últimas linhas do log mostram a causa.

## Etapa 4 — Endereços

| Ferramenta | Endereço | O que aparece |
|---|---|---|
| Loja | http://192.168.15.100:8080/api/produtos | Produtos em JSON |
| Produto | http://192.168.15.100:8080/api/produtos/10 | Um produto |
| Busca | http://192.168.15.100:8080/api/busca?q=notebook | Resultados |
| Métricas brutas | http://192.168.15.100:8080/actuator/prometheus | Texto com métricas |
| Prometheus | http://192.168.15.100:9090 | Tela de consulta |
| Alertmanager | http://192.168.15.100:9093 | Lista de alertas |
| Grafana | http://192.168.15.100:3002 | Login `admin` / `admin123` |
| Locust | http://192.168.15.100:8089 | Tela do teste de carga |

## Etapa 5 — Métricas e dashboard

1. Prometheus → **Status → Targets**: `efecaf-app`, `blackbox` e `prometheus` em **UP**. 📸
2. Consultas para testar na aba Graph:

| Mede | Consulta |
|---|---|
| Req/s | `sum(rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[1m]))` |
| p95 | `histogram_quantile(0.95, sum by (le) (rate(http_server_requests_seconds_bucket{uri!~"/actuator.*"}[1m])))` |
| Site no ar | `probe_success` |
| Conexões em uso | `hikaricp_connections_active` |

3. Locust: deixe **Shape class** vazio, Users **20**, Ramp up **5**, START.
4. Grafana → Dashboards → **E-Fecaf Global → E-Fecaf Global - Saúde da Plataforma**. 📸
5. Locust: STOP.

## Etapa 6 — Alertas no Telegram

1. No Telegram, fale com **@BotFather** → `/newbot` → escolha nome e usuário terminado em `bot`.
   Copie o token. Abra o seu bot e mande `/start`.
2. Abra no navegador `https://api.telegram.org/botSEU_TOKEN/getUpdates` e copie o número
   depois de `"chat":{"id":`.
3. Grave o token no arquivo secreto (troque `SEU_TOKEN`):
   ```bash
   printf 'SEU_TOKEN' > alertmanager/secrets/telegram_token
   ```
4. Em `alertmanager/alertmanager.yml`, troque `123456789` pelo seu chat_id.
5. `docker compose restart alertmanager`

**O token não vai para o GitHub:** o arquivo `alertmanager/secrets/telegram_token` está no
`.gitignore`. Mesmo assim, não mostre o token em print nem no vídeo.

As 7 regras (`prometheus/alertas.yml`):

| Alerta | Dispara quando | Severidade |
|---|---|---|
| SiteForaDoAr | health check externo falha | 🔴 crítico |
| LatenciaAlta | p95 > 1s | 🟡 aviso |
| LatenciaCritica | p95 > 2s | 🔴 crítico |
| TaxaDeErrosAlta | erros 5xx > 2% | 🔴 crítico |
| PoolDoBancoSaturado | ≥ 90% das conexões em uso | 🔴 crítico |
| CPUAlta | CPU > 80% | 🟡 aviso |
| MemoriaAlta | heap > 85% | 🟡 aviso |

**Teste de queda:**

```bash
docker compose stop app
```

Prometheus → **Alerts**: `SiteForaDoAr` vai de Inactive → Pending → Firing (~30–40 s).
Chega 🚨 no Telegram. 📸

```bash
docker compose start app
```

Em ~1 minuto chega ✅ RESOLVIDO. 📸

## Etapa 7 — Testes de carga

Deixe o Grafana e o Locust abertos em abas separadas. Anote os números de cada teste.

- **Teste 1 (linha de base):** Shape vazio, Users 50, Ramp up 5, rodar 3 min.
  Anote req/s, p95, % de falhas, conexões em uso. STOP.
- **Teste 2 (Black Friday):** New test → Shape **PicoBlackFriday** → START.
  Dura 5 min: 50 → 200 → 800 (pico) → 100 usuários. 📸 Grafana no pico, aba Charts do Locust,
  alertas no Telegram. Baixe o CSV em **Download Data**.
- **Teste 3 (limite):** Shape vazio, Users 1500, Ramp up 20. Anote em quantos usuários os erros
  passam de 5% ou o p95 passa de 3s. 📸 STOP.

## Etapa 8 — Otimizar e provar (antes × depois)

A otimização tem **duas rodadas**. A primeira é o palpite óbvio, e ele **não resolve**. A
segunda usa o que o monitoramento mostrou. Essa é a história do slide de otimização.

### 8.1 — Primeiro palpite: mais conexões e mais CPU

No `docker-compose.yml`, serviço `app`: `DB_POOL_SIZE: "20"` e `cpus: 2.0`. Depois:

```bash
docker compose up -d app
```

Repita o Teste 2 e anote. O pool passa a mostrar 20 em uso e a fila continua, e os erros quase
não mudam. No painel **p95 por endpoint**, `/api/busca` é de longe o mais lento. Rode
`docker stats --no-stream` durante o pico: o **postgres passa de 300% de CPU**. O gargalo
real é a busca sem índice, que lê os 50 mil produtos a cada consulta (cerca de 215 ms de CPU cada).

### 8.2 — Correção certa: índices na busca

```bash
docker compose exec -T postgres psql -U loja -d loja < otimizacao/indices.sql
```

Repita o Teste 2 e anote. A busca cai de cerca de 215 ms para cerca de 2 ms no banco.

| Métrica no pico | Antes (pool 5, 1 CPU) | 8.1 (pool 20, 2 CPUs) | 8.2 (+ índices) |
|---|---|---|---|
| p95 | | | |
| Taxa de erros | | | |
| Req/s | | | |
| Alertas disparados | | | |

Referência, medida em 09/11 com 400 usuários fixos neste servidor:
antes = 45% de erros, p95 de 2,3 s e 105 req/s; 8.1 = 39%, 3,3 s e 103 req/s;
8.2 = **0% de erros, p95 de 0,3 s e 197 req/s**.

**Para repetir tudo do zero** (voltar ao "antes"): devolva `DB_POOL_SIZE: "5"` e `cpus: 1.0`,
rode `docker compose up -d app` e depois
`docker compose exec -T postgres psql -U loja -d loja < otimizacao/remover-indices.sql`.

Outras propostas para os slides: tirar a chamada ao gateway de pagamento de dentro da
transação, cache Redis do catálogo, CDN para imagens, balanceador com mais instâncias,
réplicas de leitura e fila para o checkout.

## Etapa 9 — Checklist de prints

- [ ] Targets UP
- [ ] Dashboard com tráfego normal
- [ ] Alerts Firing + Telegram 🚨 e ✅
- [ ] Grafana no pico da Black Friday
- [ ] Charts do Locust
- [ ] Limite do teste de estresse
- [ ] Dashboard no pico da 8.1 (pool 20) e da 8.2 (índices)
- [ ] Tabela antes × depois

## Etapa 10 — Demonstração

- Para mostrar o dashboard "vivo", rode o PicoBlackFriday no Locust: os alertas disparam
  durante o pico e chegam no Telegram.
- No fim: `docker compose down`.

## Se algo der errado

| Problema | Solução |
|---|---|
| `port is already allocated` | Troque o lado esquerdo da porta no compose (ex.: `"3001:3000"`) |
| A loja fica reiniciando | `docker compose logs app` e veja as últimas linhas |
| Grafana "No data" | Gere tráfego no Locust e espere 30 s; confira os Targets |
| Telegram não recebe | Mandou `/start`? Revise token/chat_id; `docker compose logs alertmanager` |
| Quero recomeçar do zero | `docker compose down -v` (apaga banco e históricos) |
