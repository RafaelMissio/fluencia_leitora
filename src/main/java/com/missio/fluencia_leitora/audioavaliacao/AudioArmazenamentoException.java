package com.missio.fluencia_leitora.audioavaliacao;

/** AUD-05: falha de escrita em disco (embrulha a {@link java.io.IOException} original). */
public class AudioArmazenamentoException extends AudioStorageException {

    public AudioArmazenamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
