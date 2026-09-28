package com.missio.fluencia_leitora.common.texto;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * PAL-08: separa um texto corrido em palavras (tokens), reutilizável pelo
 * cadastro de listas {@code TEXTO_CURTO} (feature {@code banco-palavras}) e,
 * depois, pela feature {@code avaliacao} (entrada ad hoc de texto). Função
 * pura: sem estado, sem I/O.
 */
public final class TokenizadorTexto {

    private static final Pattern PONTUACAO_NA_BORDA = Pattern.compile("^[^\\p{L}-]+|[^\\p{L}-]+$");

    private TokenizadorTexto() {
    }

    /**
     * Separa {@code texto} por espaços em branco, remove a pontuação do
     * início e do fim de cada token (hífen interno não é afetado) e
     * descarta os tokens que ficarem vazios. A grafia original de cada
     * token (maiúsculas, acentos) é preservada.
     */
    public static List<String> tokenizar(String texto) {
        List<String> tokens = new ArrayList<>();
        if (texto == null) {
            return tokens;
        }

        for (String bruto : texto.trim().split("\\s+")) {
            String token = PONTUACAO_NA_BORDA.matcher(bruto).replaceAll("");
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }
}
