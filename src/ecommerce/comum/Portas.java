package ecommerce.comum;

public final class Portas {

    public static final int LOJA = 8080;
    public static final int PRODUTOS = 8081;
    public static final int CEP = 8082;
    public static final int FISCAL = 8083;
    public static final int EMAIL = 8084;
    public static final int PAGAMENTO = 8085;
    public static final int ENTREGA = 8086;

    private Portas() {
    }

    public static String url(int porta) {
        return "http://localhost:" + porta;
    }
}
