package ecommerce.entrega;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Json;
import ecommerce.comum.Portas;
import ecommerce.comum.Valores;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class EntregaApi {

    private final AtomicInteger sequencia = new AtomicInteger(0);

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/entregas", this::agendar);
        servidor.start();
        System.out.println("[Entrega] rodando na porta " + porta);
    }

    @SuppressWarnings("unchecked")
    private void agendar(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("POST")) {
            Http.responderErro(troca, 405, "Metodo nao permitido");
            return;
        }
        Map<String, Object> corpo = Json.lerObjeto(Http.lerCorpo(troca));
        if (!(corpo.get("endereco") instanceof Map<?, ?>)) {
            Http.responderErro(troca, 400, "Endereco de entrega nao informado");
            return;
        }
        Map<String, Object> endereco = (Map<String, Object>) corpo.get("endereco");
        String uf = Valores.texto(endereco, "uf");

        int prazo = uf.equals("PR") ? 3 : 7;
        String transportadora = uf.equals("PR") ? "Transportadora Oeste Express" : "Correios SEDEX";

        Map<String, Object> entrega = new LinkedHashMap<>();
        entrega.put("pedido", corpo.get("pedido"));
        entrega.put("codigoRastreio", String.format("BR%09dTL", sequencia.incrementAndGet()));
        entrega.put("transportadora", transportadora);
        entrega.put("prazoDias", prazo);
        entrega.put("previsao", LocalDate.now().plusDays(prazo).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        entrega.put("endereco", endereco);
        entrega.put("status", "AGUARDANDO_COLETA");

        System.out.println("[Entrega] pedido " + corpo.get("pedido") + " -> rastreio " + entrega.get("codigoRastreio"));
        Http.responder(troca, 201, entrega);
    }

    public static void main(String[] args) throws IOException {
        new EntregaApi().iniciar(Portas.ENTREGA);
    }
}
