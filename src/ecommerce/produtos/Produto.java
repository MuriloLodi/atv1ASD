package ecommerce.produtos;

import java.util.LinkedHashMap;
import java.util.Map;

public class Produto {

    private final int id;
    private final String nome;
    private final String descricao;
    private final double preco;
    private int estoque;

    public Produto(int id, String nome, String descricao, double preco, int estoque) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.preco = preco;
        this.estoque = estoque;
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public double getPreco() {
        return preco;
    }

    public int getEstoque() {
        return estoque;
    }

    public void baixarEstoque(int quantidade) {
        estoque -= quantidade;
    }

    public Map<String, Object> paraMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        mapa.put("id", id);
        mapa.put("nome", nome);
        mapa.put("descricao", descricao);
        mapa.put("preco", preco);
        mapa.put("estoque", estoque);
        return mapa;
    }
}
