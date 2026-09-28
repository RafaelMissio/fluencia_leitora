package com.missio.fluencia_leitora.audioavaliacao;

/** AUD-04: conteúdo com tamanho zero ou acima do limite configurado. */
public class AudioTamanhoInvalidoException extends AudioStorageException {

    public AudioTamanhoInvalidoException(String message) {
        super(message);
    }
}
