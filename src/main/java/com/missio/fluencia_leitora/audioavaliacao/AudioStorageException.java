package com.missio.fluencia_leitora.audioavaliacao;

/**
 * Base comum das falhas de {@link AudioStoragePort}, para quem quiser um
 * catch único. Não é {@code common.error.BusinessException} de propósito -
 * esta feature não tem controller; quando a feature {@code avaliacao}
 * existir, seu controller decide como mapear cada subtipo para HTTP
 * (design.md, Components).
 */
public abstract class AudioStorageException extends RuntimeException {

    protected AudioStorageException(String message) {
        super(message);
    }

    protected AudioStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
