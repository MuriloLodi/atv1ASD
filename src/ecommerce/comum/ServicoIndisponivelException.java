package ecommerce.comum;

public class ServicoIndisponivelException extends RuntimeException {

    public ServicoIndisponivelException(String url) {
        super("Nao foi possivel conectar em " + url);
    }
}
