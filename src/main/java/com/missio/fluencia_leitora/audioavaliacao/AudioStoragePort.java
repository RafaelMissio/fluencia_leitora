package com.missio.fluencia_leitora.audioavaliacao;

/**
 * Guarda e recupera bytes de áudio, independente de onde ficam gravados
 * (AD-003, `.specs/STATE.md`: hoje disco local, podendo trocar para S3
 * depois sem mudar quem chama). Sem endpoint HTTP nesta feature - chamada
 * Java direta pela feature `avaliacao` quando existir (design.md,
 * Architecture Overview).
 */
public interface AudioStoragePort {

    /**
     * Valida formato e tamanho e grava {@code conteudo} em disco.
     *
     * @return uma referência opaca que identifica o arquivo de forma única,
     *         para usar depois em {@link #recuperar(String)}
     * @throws AudioFormatoInvalidoException se {@code mimeType} não estiver na lista permitida
     * @throws AudioTamanhoInvalidoException se {@code conteudo} estiver vazio ou acima do limite
     * @throws AudioArmazenamentoException se a escrita em disco falhar
     */
    String armazenar(byte[] conteudo, String mimeType);

    /**
     * Devolve os bytes gravados por uma chamada anterior a {@link #armazenar}.
     *
     * @throws AudioNaoEncontradoException se {@code referencia} não existir
     */
    byte[] recuperar(String referencia);
}
