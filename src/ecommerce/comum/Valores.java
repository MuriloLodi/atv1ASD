package ecommerce.comum;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public final class Valores {

    private Valores() {
    }

    public static String texto(Map<String, Object> mapa, String campo) {
        Object valor = mapa.get(campo);
        return valor == null ? "" : valor.toString().trim();
    }

    public static int inteiro(Map<String, Object> mapa, String campo) {
        Object valor = mapa.get(campo);
        if (valor instanceof Number n) return n.intValue();
        return Integer.parseInt(texto(mapa, campo));
    }

    public static double decimal(Map<String, Object> mapa, String campo) {
        Object valor = mapa.get(campo);
        if (valor instanceof Number n) return n.doubleValue();
        return Double.parseDouble(texto(mapa, campo));
    }

    public static double arredondar(double valor) {
        return BigDecimal.valueOf(valor).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    public static String apenasDigitos(String valor) {
        return valor == null ? "" : valor.replaceAll("\\D", "");
    }
}
