package ecommerce.pagamento;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Json;
import ecommerce.comum.Portas;
import ecommerce.comum.Valores;

import java.io.IOException;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PagamentoApi {

    private static final double LIMITE_CARTAO = 5000.00;

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/pagamentos", this::pagar);
        servidor.start();
        System.out.println("[Pagamento] rodando na porta " + porta);
    }

    private void pagar(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("POST")) {
            Http.responderErro(troca, 405, "Metodo nao permitido");
            return;
        }
        Map<String, Object> corpo = Json.lerObjeto(Http.lerCorpo(troca));
        String numero = Valores.apenasDigitos(Valores.texto(corpo, "numeroCartao"));
        String titular = Valores.texto(corpo, "titular");
        String validade = Valores.texto(corpo, "validade");
        String cvv = Valores.apenasDigitos(Valores.texto(corpo, "cvv"));
        double valor = Valores.decimal(corpo, "valor");

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("transacao", UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        resultado.put("valor", valor);

        String motivo = validar(numero, titular, validade, cvv, valor);
        if (motivo == null) {
            resultado.put("status", "APROVADO");
            resultado.put("mensagem", "Pagamento aprovado");
            resultado.put("cartao", "**** **** **** " + numero.substring(numero.length() - 4));
        } else {
            resultado.put("status", "RECUSADO");
            resultado.put("mensagem", motivo);
        }

        System.out.println("[Pagamento] transacao " + resultado.get("transacao") + " de R$ "
                + valor + " -> " + resultado.get("status"));
        Http.responder(troca, 200, resultado);
    }

    private String validar(String numero, String titular, String validade, String cvv, double valor) {
        if (numero.length() < 13 || numero.length() > 19) return "Numero do cartao invalido";
        if (titular.isBlank()) return "Nome do titular nao informado";
        if (cvv.length() < 3 || cvv.length() > 4) return "CVV invalido";
        if (cartaoVencido(validade)) return "Cartao vencido ou validade invalida";
        if (numero.endsWith("0000")) return "Transacao nao autorizada pela operadora";
        if (valor > LIMITE_CARTAO) return "Limite do cartao insuficiente";
        return null;
    }

    private boolean cartaoVencido(String validade) {
        try {
            YearMonth mesAno = YearMonth.parse(validade, DateTimeFormatter.ofPattern("MM/yy"));
            return mesAno.isBefore(YearMonth.now());
        } catch (DateTimeParseException e) {
            return true;
        }
    }

    public static void main(String[] args) throws IOException {
        new PagamentoApi().iniciar(Portas.PAGAMENTO);
    }
}
