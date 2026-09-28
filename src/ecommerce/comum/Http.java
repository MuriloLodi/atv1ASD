package ecommerce.comum;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Executors;

public class Http {

    private static final HttpClient CLIENTE = HttpClient.newHttpClient();

    public record Resposta(int status, Object corpo) {
        public boolean ok() {
            return status >= 200 && status < 300;
        }

        @SuppressWarnings("unchecked")
        public Map<String, Object> objeto() {
            return (Map<String, Object>) corpo;
        }
    }

    public static HttpServer criarServidor(int porta) throws IOException {
        HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
        servidor.setExecutor(Executors.newFixedThreadPool(8));
        return servidor;
    }

    public static String lerCorpo(HttpExchange troca) throws IOException {
        return new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    public static void responder(HttpExchange troca, int status, Object corpo) throws IOException {
        byte[] bytes = Json.escrever(corpo).getBytes(StandardCharsets.UTF_8);
        troca.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        enviar(troca, status, bytes);
    }

    public static void responderErro(HttpExchange troca, int status, String mensagem) throws IOException {
        responder(troca, status, Map.of("erro", mensagem));
    }

    public static void enviar(HttpExchange troca, int status, byte[] bytes) throws IOException {
        troca.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        try (OutputStream saida = troca.getResponseBody()) {
            saida.write(bytes);
        }
    }

    public static Resposta get(String url) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url)).GET().build();
        return executar(requisicao);
    }

    public static Resposta post(String url, Object corpo) {
        HttpRequest requisicao = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(Json.escrever(corpo)))
                .build();
        return executar(requisicao);
    }

    private static Resposta executar(HttpRequest requisicao) {
        try {
            HttpResponse<String> resposta = CLIENTE.send(requisicao, HttpResponse.BodyHandlers.ofString());
            return new Resposta(resposta.statusCode(), Json.ler(resposta.body()));
        } catch (IOException e) {
            throw new ServicoIndisponivelException(requisicao.uri().toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicoIndisponivelException(requisicao.uri().toString());
        }
    }
}
