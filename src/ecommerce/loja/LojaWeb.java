package ecommerce.loja;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Json;
import ecommerce.comum.Portas;
import ecommerce.comum.ServicoIndisponivelException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class LojaWeb {

    private static final Path PASTA_WEB = Path.of("web");
    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8");

    private final OrquestradorCompra orquestrador = new OrquestradorCompra();

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/", this::arquivos);
        servidor.createContext("/api/produtos", troca -> repassar(troca, Portas.url(Portas.PRODUTOS) + "/produtos"));
        servidor.createContext("/api/cep/", this::cep);
        servidor.createContext("/api/emails", troca -> repassar(troca, Portas.url(Portas.EMAIL) + "/emails"));
        servidor.createContext("/api/pedidos", this::pedidos);
        servidor.start();
        System.out.println("[Loja] rodando em " + Portas.url(porta));
    }

    private void cep(HttpExchange troca) throws IOException {
        String cep = troca.getRequestURI().getPath().substring("/api/cep/".length());
        repassar(troca, Portas.url(Portas.CEP) + "/cep/" + cep);
    }

    private void repassar(HttpExchange troca, String url) throws IOException {
        try {
            Http.Resposta resposta = Http.get(url);
            Http.responder(troca, resposta.status(), resposta.corpo());
        } catch (ServicoIndisponivelException e) {
            Http.responderErro(troca, 503, e.getMessage());
        }
    }

    private void pedidos(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("POST")) {
            Http.responderErro(troca, 405, "Metodo nao permitido");
            return;
        }
        try {
            Map<String, Object> dados = Json.lerObjeto(Http.lerCorpo(troca));
            Map<String, Object> resultado = orquestrador.finalizar(dados);
            Http.responder(troca, 200, resultado);
        } catch (OrquestradorCompra.CompraException e) {
            Http.responderErro(troca, e.getStatus(), e.getMessage());
        } catch (IllegalArgumentException | ClassCastException e) {
            Http.responderErro(troca, 400, "Requisicao invalida: " + e.getMessage());
        }
    }

    private void arquivos(HttpExchange troca) throws IOException {
        String caminho = troca.getRequestURI().getPath();
        if (caminho.equals("/")) {
            caminho = "/index.html";
        }
        Path arquivo = PASTA_WEB.resolve(caminho.substring(1)).normalize();
        if (!arquivo.startsWith(PASTA_WEB) || !Files.isRegularFile(arquivo)) {
            Http.responderErro(troca, 404, "Pagina nao encontrada");
            return;
        }
        String nome = arquivo.getFileName().toString();
        String extensao = nome.substring(nome.lastIndexOf('.') + 1);
        troca.getResponseHeaders().set("Content-Type", TIPOS.getOrDefault(extensao, "application/octet-stream"));
        Http.enviar(troca, 200, Files.readAllBytes(arquivo));
    }

    public static void main(String[] args) throws IOException {
        new LojaWeb().iniciar(Portas.LOJA);
    }
}
