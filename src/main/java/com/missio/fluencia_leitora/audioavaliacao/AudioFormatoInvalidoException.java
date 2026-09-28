package com.missio.fluencia_leitora.audioavaliacao;

/** AUD-03: {@code mimeType} nulo, vazio ou fora da lista permitida. */
public class AudioFormatoInvalidoException extends AudioStorageException {

    public AudioFormatoInvalidoException(String message) {
        super(message);
    }
}
