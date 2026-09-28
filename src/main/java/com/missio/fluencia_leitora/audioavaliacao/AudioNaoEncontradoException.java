package com.missio.fluencia_leitora.audioavaliacao;

/**
 * AUD-06: {@code recuperar} chamado com uma referência que não existe no
 * diretório base, ou que resolve fora dele (design.md, Risks & Concerns).
 */
public class AudioNaoEncontradoException extends AudioStorageException {

    public AudioNaoEncontradoException(String message) {
        super(message);
    }
}
