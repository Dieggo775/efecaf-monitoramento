"""
Simulação de clientes da E-Fecaf Global.

- Sem "Shape class": você escolhe na tela quantos usuários quer (testes 1 e 3).
- Shape "PicoBlackFriday": a carga sobe e desce sozinha em 5 minutos (teste 2).
"""
import random

from locust import FastHttpUser, LoadTestShape, between, task

TOTAL_PRODUTOS = 50_000
TERMOS_BUSCA = ["notebook", "smartphone", "monitor", "tv", "fone", "cadeira",
                "geladeira", "tablet", "fecaf", "orion", "aurora", "teclado gamer"]


class Cliente(FastHttpUser):
    """Um cliente navegando: vê a vitrine, busca, abre produtos e às vezes compra."""

    wait_time = between(1, 3)

    @task(5)
    def vitrine(self):
        self.client.get(f"/api/produtos?pagina={random.randint(0, 50)}", name="/api/produtos")

    @task(4)
    def ver_produto(self):
        self.client.get(f"/api/produtos/{random.randint(1, TOTAL_PRODUTOS)}", name="/api/produtos/[id]")

    @task(3)
    def buscar(self):
        self.client.get(f"/api/busca?q={random.choice(TERMOS_BUSCA)}", name="/api/busca")

    @task(2)
    def carrinho(self):
        self.client.post("/api/carrinho",
                         json={"produtoId": random.randint(1, TOTAL_PRODUTOS), "quantidade": 1},
                         name="/api/carrinho")

    @task(1)
    def checkout(self):
        self.client.post("/api/checkout",
                         json={"produtoId": random.randint(1, TOTAL_PRODUTOS),
                               "quantidade": random.randint(1, 3)},
                         name="/api/checkout")


class PicoBlackFriday(LoadTestShape):
    """
    Black Friday em 5 minutos:
      0:00-1:00   50 usuários   (manhã tranquila)
      1:00-2:00  200 usuários   (começa a promoção)
      2:00-3:30  800 usuários   (PICO: meia-noite da Black Friday)
      3:30-5:00  100 usuários   (pós-pico)
    """

    estagios = [
        {"ate": 60, "usuarios": 50, "taxa": 10},
        {"ate": 120, "usuarios": 200, "taxa": 20},
        {"ate": 210, "usuarios": 800, "taxa": 50},
        {"ate": 300, "usuarios": 100, "taxa": 50},
    ]

    def tick(self):
        t = self.get_run_time()
        for e in self.estagios:
            if t < e["ate"]:
                return e["usuarios"], e["taxa"]
        return None  # fim do teste
