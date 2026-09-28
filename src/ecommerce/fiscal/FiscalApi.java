package ecommerce.fiscal;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Json;
import ecommerce.comum.Portas;
import ecommerce.comum.Valores;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class FiscalApi {

    private static final double ALIQUOTA_ICMS = 0.18;
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AtomicInteger proximoNumero = new AtomicInteger(1000);
    private final Random random = new Random();

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/notas", this::emitir);
        servidor.start();
        System.out.println("[Fiscal] rodando na porta " + porta);
    }

    private void emitir(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("POST")) {
            Http.responderErro(troca, 405, "Metodo nao permitido");
            return;
        }
        Map<String, Object> corpo = Json.lerObjeto(Http.lerCorpo(troca));
        String cpf = Valores.apenasDigitos(Valores.texto(corpo, "cpf"));
        if (cpf.length() != 11) {
            Http.responderErro(troca, 400, "CPF invalido");
            return;
        }
        if (!(corpo.get("itens") instanceof List<?> itens) || itens.isEmpty()) {
            Http.responderErro(troca, 400, "Nota sem itens");
            return;
        }

        double total = Valores.decimal(corpo, "total");
        int numero = proximoNumero.incrementAndGet();

        Map<String, Object> nota = new LinkedHashMap<>();
        nota.put("numero", numero);
        nota.put("serie", 1);
        nota.put("chaveAcesso", gerarChave(numero));
        nota.put("emissao", LocalDateTime.now().format(FORMATO));
        nota.put("pedido", corpo.get("pedido"));
        nota.put("cliente", corpo.get("cliente"));
        nota.put("cpf", cpf);
        nota.put("itens", itens);
        nota.put("total", total);
        nota.put("icms", Valores.arredondar(total * ALIQUOTA_ICMS));

        System.out.println("[Fiscal] NF-e " + numero + " emitida para o pedido " + corpo.get("pedido"));
        Http.responder(troca, 201, nota);
    }

    private String gerarChave(int numero) {
        StringBuilder chave = new StringBuilder("41");
        chave.append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMM")));
        chave.append(String.format("%09d", numero));
        while (chave.length() < 44) {
            chave.append(random.nextInt(10));
        }
        return chave.toString();
    }

    public static void main(String[] args) throws IOException {
        new FiscalApi().iniciar(Portas.FISCAL);
    }
}
