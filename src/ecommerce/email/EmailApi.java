package ecommerce.email;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Json;
import ecommerce.comum.Portas;
import ecommerce.comum.Valores;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EmailApi {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final List<Map<String, Object>> enviados = new ArrayList<>();

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/emails", this::tratar);
        servidor.start();
        System.out.println("[Email] rodando na porta " + porta);
    }

    private void tratar(HttpExchange troca) throws IOException {
        switch (troca.getRequestMethod()) {
            case "POST" -> enviar(troca);
            case "GET" -> listar(troca);
            default -> Http.responderErro(troca, 405, "Metodo nao permitido");
        }
    }

    private void listar(HttpExchange troca) throws IOException {
        List<Map<String, Object>> copia;
        synchronized (enviados) {
            copia = new ArrayList<>(enviados);
        }
        Http.responder(troca, 200, copia);
    }

    private void enviar(HttpExchange troca) throws IOException {
        Map<String, Object> corpo = Json.lerObjeto(Http.lerCorpo(troca));
        String para = Valores.texto(corpo, "para");
        String assunto = Valores.texto(corpo, "assunto");
        String mensagem = Valores.texto(corpo, "mensagem");

        if (!para.contains("@")) {
            Http.responderErro(troca, 400, "Destinatario invalido");
            return;
        }

        Map<String, Object> email = new LinkedHashMap<>();
        synchronized (enviados) {
            email.put("id", enviados.size() + 1);
            email.put("para", para);
            email.put("assunto", assunto);
            email.put("mensagem", mensagem);
            email.put("enviadoEm", LocalDateTime.now().format(FORMATO));
            enviados.add(email);
        }

        System.out.println();
        System.out.println("[Email] ===============================");
        System.out.println("Para: " + para);
        System.out.println("Assunto: " + assunto);
        System.out.println(mensagem);
        System.out.println("=======================================");
        System.out.println();

        Http.responder(troca, 201, email);
    }

    public static void main(String[] args) throws IOException {
        new EmailApi().iniciar(Portas.EMAIL);
    }
}
