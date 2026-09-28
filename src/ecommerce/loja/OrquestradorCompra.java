package ecommerce.loja;

import ecommerce.comum.Http;
import ecommerce.comum.Portas;
import ecommerce.comum.ServicoIndisponivelException;
import ecommerce.comum.Valores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class OrquestradorCompra {

    private static final String PRODUTOS = Portas.url(Portas.PRODUTOS);
    private static final String EMAIL = Portas.url(Portas.EMAIL);
    private static final String PAGAMENTO = Portas.url(Portas.PAGAMENTO);
    private static final String FISCAL = Portas.url(Portas.FISCAL);
    private static final String ENTREGA = Portas.url(Portas.ENTREGA);

    private final AtomicInteger sequencia = new AtomicInteger(0);

    public static class CompraException extends RuntimeException {
        private final int status;

        public CompraException(int status, String mensagem) {
            super(mensagem);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> finalizar(Map<String, Object> dados) {
        Map<String, Object> cliente = (Map<String, Object>) dados.get("cliente");
        Map<String, Object> endereco = (Map<String, Object>) dados.get("endereco");
        Map<String, Object> cartao = (Map<String, Object>) dados.get("pagamento");
        List<Map<String, Object>> itensPedido = (List<Map<String, Object>>) dados.get("itens");

        if (cliente == null || endereco == null || cartao == null) {
            throw new CompraException(400, "Dados do cliente, endereco e pagamento sao obrigatorios");
        }
        if (itensPedido == null || itensPedido.isEmpty()) {
            throw new CompraException(400, "O carrinho esta vazio");
        }

        String email = Valores.texto(cliente, "email");
        String nome = Valores.texto(cliente, "nome");

        try {
            List<Map<String, Object>> itens = montarItens(itensPedido);
            double total = Valores.arredondar(itens.stream()
                    .mapToDouble(i -> (double) i.get("subtotal"))
                    .sum());

            String pedido = String.format("PED-%04d", sequencia.incrementAndGet());
            List<Map<String, Object>> etapas = new ArrayList<>();
            Map<String, Object> resultado = new LinkedHashMap<>();
            resultado.put("pedido", pedido);
            resultado.put("etapas", etapas);
            resultado.put("itens", itens);
            resultado.put("total", total);
            etapa(etapas, 5, "Compra confirmada pelo usuario", true);

            enviarEmail(email, "Pedido " + pedido + " recebido",
                    "Ola " + nome + ", recebemos o seu pedido " + pedido + ".\n"
                            + descreverItens(itens) + "Total: R$ " + formatar(total));
            etapa(etapas, 6, "E-mail de confirmacao da compra enviado", true);

            Map<String, Object> dadosPagamento = new LinkedHashMap<>(cartao);
            dadosPagamento.put("valor", total);
            Map<String, Object> pagamento = Http.post(PAGAMENTO + "/pagamentos", dadosPagamento).objeto();
            boolean aprovado = "APROVADO".equals(pagamento.get("status"));
            resultado.put("pagamento", pagamento);
            etapa(etapas, 7, "Pagamento " + pagamento.get("status") + ": " + pagamento.get("mensagem"), aprovado);

            enviarEmail(email, "Pagamento do pedido " + pedido + (aprovado ? " aprovado" : " recusado"),
                    "Transacao " + pagamento.get("transacao") + ": " + pagamento.get("mensagem"));
            etapa(etapas, 8, "E-mail com resultado do pagamento enviado", true);

            if (!aprovado) {
                resultado.put("sucesso", false);
                resultado.put("mensagem", "Pagamento recusado: " + pagamento.get("mensagem"));
                return resultado;
            }

            Map<String, Object> dadosNota = new LinkedHashMap<>();
            dadosNota.put("pedido", pedido);
            dadosNota.put("cliente", nome);
            dadosNota.put("cpf", cliente.get("cpf"));
            dadosNota.put("itens", itens);
            dadosNota.put("total", total);
            Http.Resposta respostaNota = Http.post(FISCAL + "/notas", dadosNota);
            if (!respostaNota.ok()) {
                throw new CompraException(respostaNota.status(), "Erro ao emitir nota: " + respostaNota.objeto().get("erro"));
            }
            Map<String, Object> nota = respostaNota.objeto();
            resultado.put("notaFiscal", nota);

            Http.Resposta respostaEstoque = Http.post(PRODUTOS + "/estoque/baixa", Map.of("itens", itensPedido));
            if (!respostaEstoque.ok()) {
                throw new CompraException(respostaEstoque.status(), "Erro ao baixar estoque: " + respostaEstoque.objeto().get("erro"));
            }
            etapa(etapas, 9, "Nota fiscal " + nota.get("numero") + " gerada e estoque atualizado", true);

            enviarEmail(email, "Nota fiscal do pedido " + pedido,
                    "NF-e numero " + nota.get("numero") + " serie " + nota.get("serie") + "\n"
                            + "Chave de acesso: " + nota.get("chaveAcesso") + "\n"
                            + "Valor total: R$ " + formatar(total));
            etapa(etapas, 10, "E-mail com a nota fiscal enviado", true);

            Map<String, Object> entrega = Http.post(ENTREGA + "/entregas",
                    Map.of("pedido", pedido, "endereco", endereco)).objeto();
            resultado.put("entrega", entrega);
            etapa(etapas, 11, "Produtos disponibilizados para entrega (" + entrega.get("transportadora") + ")", true);

            enviarEmail(email, "Pedido " + pedido + " a caminho",
                    "Codigo de rastreio: " + entrega.get("codigoRastreio") + "\n"
                            + "Transportadora: " + entrega.get("transportadora") + "\n"
                            + "Previsao de entrega: " + entrega.get("previsao"));
            etapa(etapas, 12, "E-mail com dados da entrega enviado", true);

            resultado.put("sucesso", true);
            resultado.put("mensagem", "Compra finalizada com sucesso");
            return resultado;

        } catch (ServicoIndisponivelException e) {
            throw new CompraException(503, e.getMessage());
        }
    }

    private List<Map<String, Object>> montarItens(List<Map<String, Object>> itensPedido) {
        List<Map<String, Object>> itens = new ArrayList<>();
        for (Map<String, Object> item : itensPedido) {
            int id = Valores.inteiro(item, "id");
            int quantidade = Valores.inteiro(item, "quantidade");
            if (quantidade <= 0) {
                throw new CompraException(400, "Quantidade invalida para o produto " + id);
            }

            Http.Resposta resposta = Http.get(PRODUTOS + "/produtos/" + id);
            if (!resposta.ok()) {
                throw new CompraException(404, "Produto " + id + " nao encontrado");
            }
            Map<String, Object> produto = resposta.objeto();
            if (Valores.inteiro(produto, "estoque") < quantidade) {
                throw new CompraException(409, "Estoque insuficiente para " + produto.get("nome"));
            }

            double preco = Valores.decimal(produto, "preco");
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("id", id);
            linha.put("nome", produto.get("nome"));
            linha.put("quantidade", quantidade);
            linha.put("precoUnitario", preco);
            linha.put("subtotal", Valores.arredondar(preco * quantidade));
            itens.add(linha);
        }
        return itens;
    }

    private void enviarEmail(String para, String assunto, String mensagem) {
        Http.Resposta resposta = Http.post(EMAIL + "/emails",
                Map.of("para", para, "assunto", assunto, "mensagem", mensagem));
        if (!resposta.ok()) {
            throw new CompraException(resposta.status(), "Erro ao enviar e-mail: " + resposta.objeto().get("erro"));
        }
    }

    private void etapa(List<Map<String, Object>> etapas, int numero, String descricao, boolean ok) {
        Map<String, Object> etapa = new LinkedHashMap<>();
        etapa.put("numero", numero);
        etapa.put("descricao", descricao);
        etapa.put("ok", ok);
        etapas.add(etapa);
        System.out.println("[Loja] etapa " + numero + ": " + descricao);
    }

    private String descreverItens(List<Map<String, Object>> itens) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> item : itens) {
            sb.append(" - ").append(item.get("quantidade")).append("x ").append(item.get("nome"))
                    .append(" = R$ ").append(formatar((double) item.get("subtotal"))).append("\n");
        }
        return sb.toString();
    }

    private String formatar(double valor) {
        return String.format("%.2f", valor);
    }
}
