# E-commerce com Web APIs orquestradas

Projeto Pratico 1 da disciplina de Arquitetura de Sistemas Distribuidos (TSIN36B) - UTFPR Toledo.

Solucao de e-commerce Web onde a **Loja Web** atua como orquestradora e se comunica via HTTP com seis Web APIs independentes. As APIs apenas simulam o comportamento real, com dados em memoria (sem banco de dados).

## Arquitetura

| Servico        | Porta | Endpoints |
|----------------|-------|-----------|
| Loja Web       | 8080  | `GET /` (pagina), `GET /api/produtos`, `GET /api/cep/{cep}`, `POST /api/pedidos`, `GET /api/emails` |
| Produtos API   | 8081  | `GET /produtos`, `GET /produtos/{id}`, `POST /estoque/baixa` |
| CEP API        | 8082  | `GET /cep/{cep}` |
| Fiscal API     | 8083  | `POST /notas` |
| Email API      | 8084  | `POST /emails`, `GET /emails` |
| Pagamento API  | 8085  | `POST /pagamentos` |
| Entrega API    | 8086  | `POST /entregas` |

Cada API e um servidor HTTP proprio (`com.sun.net.httpserver.HttpServer`). As APIs nao se conhecem entre si: somente a Loja Web chama os servicos, na ordem do fluxo de compra (orquestracao), dentro da classe `OrquestradorCompra`.

## Fluxo de compra

1. Loja Web exibe catalogo de produtos (Produtos API)
2. Usuario escolhe produtos para comprar
3. Usuario informa dados de pagamento e endereco de entrega
4. Usuario informa CEP para preenchimento automatico do endereco (CEP API)
5. Usuario confirma a compra
6. Sistema envia e-mail de confirmacao da compra (Email API)
7. Sistema realiza a transacao de pagamento (Pagamento API)
8. Sistema envia e-mail com resultado do pagamento (Email API)
9. Sistema gera nota fiscal e baixa o estoque (Fiscal API e Produtos API)
10. Sistema envia e-mail com a nota fiscal (Email API)
11. Sistema disponibiliza produtos para entrega (Entrega API)
12. Sistema envia e-mail com dados da entrega (Email API)

Se o pagamento for recusado, o fluxo para na etapa 8.

## Como executar

Requisito: Java 17 ou superior (nao usa bibliotecas externas).

Windows:

```
executar.bat
```

Linux/Mac:

```
sh executar.sh
```

Depois acesse http://localhost:8080

Tambem e possivel subir cada servico separadamente, pois cada API tem seu proprio `main` (ex: `java -cp out ecommerce.pagamento.PagamentoApi`).

## Dados para teste

CEPs cadastrados: `85902-490`, `85900-010`, `85810-000`, `80010-000`, `01310-100`.

Regras do pagamento simulado:

- Cartao aprovado: qualquer numero valido, ex: `4111 1111 1111 1111`, validade futura (MM/AA) e CVV com 3 digitos
- Cartao terminado em `0000`: recusado pela operadora
- Compra acima de R$ 5.000,00: recusada por limite
- Validade expirada: recusado

Entregas para o PR tem prazo de 3 dias, demais estados 7 dias.
