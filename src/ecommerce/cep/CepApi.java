package ecommerce.cep;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Portas;
import ecommerce.comum.Valores;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class CepApi {

    private final Map<String, Map<String, Object>> enderecos = new HashMap<>();

    public CepApi() {
        adicionar("85902490", "Rua Cristo Rei", "Vila Becker", "Toledo", "PR");
        adicionar("85900010", "Rua Barao do Rio Branco", "Centro", "Toledo", "PR");
        adicionar("85810000", "Avenida Brasil", "Centro", "Cascavel", "PR");
        adicionar("80010000", "Praca Tiradentes", "Centro", "Curitiba", "PR");
        adicionar("01310100", "Avenida Paulista", "Bela Vista", "Sao Paulo", "SP");
    }

    private void adicionar(String cep, String logradouro, String bairro, String cidade, String uf) {
        Map<String, Object> endereco = new LinkedHashMap<>();
        endereco.put("cep", cep.substring(0, 5) + "-" + cep.substring(5));
        endereco.put("logradouro", logradouro);
        endereco.put("bairro", bairro);
        endereco.put("cidade", cidade);
        endereco.put("uf", uf);
        enderecos.put(cep, endereco);
    }

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/cep", this::consultar);
        servidor.start();
        System.out.println("[CEP] rodando na porta " + porta);
    }

    private void consultar(HttpExchange troca) throws IOException {
        String[] partes = troca.getRequestURI().getPath().split("/");
        if (partes.length < 3) {
            Http.responderErro(troca, 400, "Informe o CEP na URL, ex: /cep/85902490");
            return;
        }
        String cep = Valores.apenasDigitos(partes[2]);
        if (cep.length() != 8) {
            Http.responderErro(troca, 400, "CEP deve ter 8 digitos");
            return;
        }
        Map<String, Object> endereco = enderecos.get(cep);
        if (endereco == null) {
            Http.responderErro(troca, 404, "CEP nao encontrado");
            return;
        }
        System.out.println("[CEP] consulta " + cep + " -> " + endereco.get("logradouro"));
        Http.responder(troca, 200, endereco);
    }

    public static void main(String[] args) throws IOException {
        new CepApi().iniciar(Portas.CEP);
    }
}
