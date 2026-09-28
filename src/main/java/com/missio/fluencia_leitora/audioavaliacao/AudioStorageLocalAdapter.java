package com.missio.fluencia_leitora.audioavaliacao;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Único adapter real de {@link AudioStoragePort} por enquanto - grava/lê
 * arquivos num diretório local configurável (AD-003, `.specs/STATE.md`).
 */
@Component
public class AudioStorageLocalAdapter implements AudioStoragePort {

    private static final Set<String> MIME_TYPES_PERMITIDOS =
            Set.of("audio/webm", "audio/ogg", "audio/mp4", "audio/mpeg", "audio/wav");

    private static final Map<String, String> EXTENSAO_POR_MIME_TYPE = Map.of(
            "audio/webm", "webm",
            "audio/ogg", "ogg",
            "audio/mp4", "mp4",
            "audio/mpeg", "mp3",
            "audio/wav", "wav");

    private final Path diretorioBase;
    private final long tamanhoMaximoBytes;

    public AudioStorageLocalAdapter(
            @Value("${APP_AUDIO_STORAGE_DIR:data/audios}") String diretorioBase,
            @Value("${APP_AUDIO_TAMANHO_MAXIMO_BYTES:26214400}") long tamanhoMaximoBytes) {
        this.diretorioBase = Path.of(diretorioBase).toAbsolutePath().normalize();
        this.tamanhoMaximoBytes = tamanhoMaximoBytes;
    }

    @PostConstruct
    void inicializar() {
        try {
            Files.createDirectories(diretorioBase);
        } catch (IOException e) {
            throw new AudioArmazenamentoException(
                    "Não foi possível criar o diretório de armazenamento de áudio: " + diretorioBase, e);
        }
    }

    @Override
    public String armazenar(byte[] conteudo, String mimeType) {
        validarMimeType(mimeType);
        validarTamanho(conteudo);

        String nomeArquivo = UUID.randomUUID() + "." + EXTENSAO_POR_MIME_TYPE.get(mimeType);
        Path destino = diretorioBase.resolve(nomeArquivo);

        try {
            Files.write(destino, conteudo);
        } catch (IOException e) {
            excluirSeExistir(destino);
            throw new AudioArmazenamentoException("Falha ao gravar o áudio em " + destino, e);
        }

        return nomeArquivo;
    }

    @Override
    public byte[] recuperar(String referencia) {
        Path arquivo = diretorioBase.resolve(referencia).normalize();

        if (!arquivo.startsWith(diretorioBase) || !Files.isRegularFile(arquivo)) {
            throw new AudioNaoEncontradoException("Áudio não encontrado: " + referencia);
        }

        try {
            return Files.readAllBytes(arquivo);
        } catch (IOException e) {
            throw new AudioArmazenamentoException("Falha ao ler o áudio em " + arquivo, e);
        }
    }

    private void validarMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank() || !MIME_TYPES_PERMITIDOS.contains(mimeType)) {
            throw new AudioFormatoInvalidoException("mimeType não permitido: " + mimeType);
        }
    }

    private void validarTamanho(byte[] conteudo) {
        int tamanho = conteudo == null ? 0 : conteudo.length;
        if (tamanho == 0 || tamanho > tamanhoMaximoBytes) {
            throw new AudioTamanhoInvalidoException(
                    "Tamanho do áudio fora da faixa aceitável [1, " + tamanhoMaximoBytes + "]: " + tamanho);
        }
    }

    private void excluirSeExistir(Path arquivo) {
        try {
            Files.deleteIfExists(arquivo);
        } catch (IOException ignorada) {
            // Melhor esforço - a exceção original de escrita já vai propagar.
        }
    }
}
