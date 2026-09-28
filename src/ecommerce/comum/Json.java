package ecommerce.comum;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Json {

    private final String texto;
    private int pos;

    private Json(String texto) {
        this.texto = texto;
    }

    public static Object ler(String texto) {
        if (texto == null || texto.isBlank()) {
            return new LinkedHashMap<String, Object>();
        }
        Json json = new Json(texto);
        Object valor = json.lerValor();
        json.pularEspacos();
        if (json.pos < texto.length()) {
            throw new IllegalArgumentException("JSON invalido na posicao " + json.pos);
        }
        return valor;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> lerObjeto(String texto) {
        return (Map<String, Object>) ler(texto);
    }

    public static String escrever(Object valor) {
        StringBuilder sb = new StringBuilder();
        escrever(valor, sb);
        return sb.toString();
    }

    private static void escrever(Object valor, StringBuilder sb) {
        if (valor == null) {
            sb.append("null");
        } else if (valor instanceof String s) {
            escreverTexto(s, sb);
        } else if (valor instanceof Number || valor instanceof Boolean) {
            sb.append(valor);
        } else if (valor instanceof Map<?, ?> mapa) {
            sb.append('{');
            boolean primeiro = true;
            for (Map.Entry<?, ?> e : mapa.entrySet()) {
                if (!primeiro) sb.append(',');
                escreverTexto(String.valueOf(e.getKey()), sb);
                sb.append(':');
                escrever(e.getValue(), sb);
                primeiro = false;
            }
            sb.append('}');
        } else if (valor instanceof List<?> lista) {
            sb.append('[');
            for (int i = 0; i < lista.size(); i++) {
                if (i > 0) sb.append(',');
                escrever(lista.get(i), sb);
            }
            sb.append(']');
        } else {
            escreverTexto(valor.toString(), sb);
        }
    }

    private static void escreverTexto(String s, StringBuilder sb) {
        sb.append('"');
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
    }

    private Object lerValor() {
        pularEspacos();
        char c = texto.charAt(pos);
        if (c == '{') return lerMapa();
        if (c == '[') return lerLista();
        if (c == '"') return lerTexto();
        if (texto.startsWith("true", pos)) { pos += 4; return true; }
        if (texto.startsWith("false", pos)) { pos += 5; return false; }
        if (texto.startsWith("null", pos)) { pos += 4; return null; }
        return lerNumero();
    }

    private Map<String, Object> lerMapa() {
        Map<String, Object> mapa = new LinkedHashMap<>();
        pos++;
        pularEspacos();
        if (texto.charAt(pos) == '}') { pos++; return mapa; }
        while (true) {
            pularEspacos();
            String chave = lerTexto();
            pularEspacos();
            esperar(':');
            mapa.put(chave, lerValor());
            pularEspacos();
            if (texto.charAt(pos) == ',') { pos++; continue; }
            esperar('}');
            return mapa;
        }
    }

    private List<Object> lerLista() {
        List<Object> lista = new ArrayList<>();
        pos++;
        pularEspacos();
        if (texto.charAt(pos) == ']') { pos++; return lista; }
        while (true) {
            lista.add(lerValor());
            pularEspacos();
            if (texto.charAt(pos) == ',') { pos++; continue; }
            esperar(']');
            return lista;
        }
    }

    private String lerTexto() {
        esperar('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = texto.charAt(pos++);
            if (c == '"') return sb.toString();
            if (c == '\\') {
                char e = texto.charAt(pos++);
                switch (e) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case 'b' -> sb.append('\b');
                    case 'f' -> sb.append('\f');
                    case 'u' -> {
                        sb.append((char) Integer.parseInt(texto.substring(pos, pos + 4), 16));
                        pos += 4;
                    }
                    default -> sb.append(e);
                }
            } else {
                sb.append(c);
            }
        }
    }

    private Number lerNumero() {
        int inicio = pos;
        while (pos < texto.length() && "+-0123456789.eE".indexOf(texto.charAt(pos)) >= 0) {
            pos++;
        }
        String numero = texto.substring(inicio, pos);
        if (numero.isEmpty()) {
            throw new IllegalArgumentException("JSON invalido na posicao " + inicio);
        }
        if (numero.contains(".") || numero.contains("e") || numero.contains("E")) {
            return Double.parseDouble(numero);
        }
        return Long.parseLong(numero);
    }

    private void esperar(char c) {
        if (pos >= texto.length() || texto.charAt(pos) != c) {
            throw new IllegalArgumentException("Esperado '" + c + "' na posicao " + pos);
        }
        pos++;
    }

    private void pularEspacos() {
        while (pos < texto.length() && Character.isWhitespace(texto.charAt(pos))) {
            pos++;
        }
    }
}
