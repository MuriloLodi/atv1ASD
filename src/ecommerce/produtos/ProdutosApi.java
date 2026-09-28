package ecommerce.produtos;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import ecommerce.comum.Http;
import ecommerce.comum.Json;
import ecommerce.comum.Portas;
import ecommerce.comum.Valores;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ProdutosApi {

    private final Map<Integer, Produto> produtos = new TreeMap<>();

    public ProdutosApi() {
        adicionar(new Produto(1, "Notebook Lenovo IdeaPad", "Ryzen 5, 8GB RAM, SSD 256GB", 3299.90, 5));
        adicionar(new Produto(2, "Mouse sem fio Logitech", "Mouse optico 1000 DPI", 89.90, 30));
        adicionar(new Produto(3, "Teclado Mecanico Redragon", "Switch blue, ABNT2", 229.90, 12));
        adicionar(new Produto(4, "Monitor LG 24\"", "Full HD, IPS, 75Hz", 799.00, 8));
        adicionar(new Produto(5, "Headset HyperX Cloud", "Som surround 7.1", 349.90, 10));
        adicionar(new Produto(6, "Webcam Full HD", "1080p com microfone", 159.90, 3));
    }

    private void adicionar(Produto produto) {
        produtos.put(produto.getId(), produto);
    }

    public void iniciar(int porta) throws IOException {
        HttpServer servidor = Http.criarServidor(porta);
        servidor.createContext("/produtos", this::tratarProdutos);
        servidor.createContext("/estoque/baixa", this::tratarBaixa);
        servidor.start();
        System.out.println("[Produtos] rodando na porta " + porta);
    }

    private void tratarProdutos(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("GET")) {
            Http.responderErro(troca, 405, "Metodo nao permitido");
            return;
        }
        String caminho = troca.getRequestURI().getPath();
        String[] partes = caminho.split("/");

        if (partes.length <= 2) {
            List<Map<String, Object>> lista = new ArrayList<>();
            synchronized (produtos) {
                produtos.values().forEach(p -> lista.add(p.paraMapa()));
            }
            Http.responder(troca, 200, lista);
            return;
        }

        try {
            Produto produto = produtos.get(Integer.parseInt(partes[2]));
            if (produto == null) {
                Http.responderErro(troca, 404, "Produto nao encontrado");
                return;
            }
            Http.responder(troca, 200, produto.paraMapa());
        } catch (NumberFormatException e) {
            Http.responderErro(troca, 400, "Id invalido");
        }
    }

    @SuppressWarnings("unchecked")
    private void tratarBaixa(HttpExchange troca) throws IOException {
        if (!troca.getRequestMethod().equals("POST")) {
            Http.responderErro(troca, 405, "Metodo nao permitido");
            return;
        }
        Map<String, Object> corpo = Json.lerObjeto(Http.lerCorpo(troca));
        List<Map<String, Object>> itens = (List<Map<String, Object>>) corpo.get("itens");
        if (itens == null || itens.isEmpty()) {
            Http.responderErro(troca, 400, "Nenhum item informado");
            return;
        }

        synchronized (produtos) {
            for (Map<String, Object> item : itens) {
                Produto produto = produtos.get(Valores.inteiro(item, "id"));
                int quantidade = Valores.inteiro(item, "quantidade");
                if (produto == null) {
                    Http.responderErro(troca, 404, "Produto " + item.get("id") + " nao encontrado");
                    return;
                }
                if (produto.getEstoque() < quantidade) {
                    Http.responderErro(troca, 409, "Estoque insuficiente para " + produto.getNome());
                    return;
                }
            }

            List<Map<String, Object>> atualizados = new ArrayList<>();
            for (Map<String, Object> item : itens) {
                Produto produto = produtos.get(Valores.inteiro(item, "id"));
                produto.baixarEstoque(Valores.inteiro(item, "quantidade"));
                atualizados.add(produto.paraMapa());
                System.out.println("[Produtos] baixa de estoque: " + produto.getNome()
                        + " -> restam " + produto.getEstoque());
            }
            Http.responder(troca, 200, Map.of("mensagem", "Estoque atualizado", "produtos", atualizados));
        }
    }

    public static void main(String[] args) throws IOException {
        new ProdutosApi().iniciar(Portas.PRODUTOS);
    }
}
