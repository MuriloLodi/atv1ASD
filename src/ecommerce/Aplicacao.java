package ecommerce;

import ecommerce.cep.CepApi;
import ecommerce.comum.Portas;
import ecommerce.email.EmailApi;
import ecommerce.entrega.EntregaApi;
import ecommerce.fiscal.FiscalApi;
import ecommerce.loja.LojaWeb;
import ecommerce.pagamento.PagamentoApi;
import ecommerce.produtos.ProdutosApi;

import java.io.IOException;

public class Aplicacao {

    public static void main(String[] args) throws IOException {
        new ProdutosApi().iniciar(Portas.PRODUTOS);
        new CepApi().iniciar(Portas.CEP);
        new FiscalApi().iniciar(Portas.FISCAL);
        new EmailApi().iniciar(Portas.EMAIL);
        new PagamentoApi().iniciar(Portas.PAGAMENTO);
        new EntregaApi().iniciar(Portas.ENTREGA);
        new LojaWeb().iniciar(Portas.LOJA);

        System.out.println();
        System.out.println("Todos os servicos iniciados. Acesse " + Portas.url(Portas.LOJA));
    }
}
