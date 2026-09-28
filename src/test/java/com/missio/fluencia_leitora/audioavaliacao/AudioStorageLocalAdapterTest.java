package com.missio.fluencia_leitora.audioavaliacao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AUD-01..AUD-08 + edge cases (spec.md): testes unitários puros com
 * {@code @TempDir} do JUnit - sem Spring context nem Docker (design.md,
 * Tips), mesmo padrão de {@code JwtServiceTest} (instancia a classe direto,
 * chama o método {@code @PostConstruct} manualmente).
 */
class AudioStorageLocalAdapterTest {

    private static final long TAMANHO_MAXIMO_BYTES = 1024;

    private AudioStorageLocalAdapter criarAdapter(Path diretorio) {
        AudioStorageLocalAdapter adapter = new AudioStorageLocalAdapter(diretorio.toString(), TAMANHO_MAXIMO_BYTES);
        adapter.inicializar();
        return adapter;
    }

    private long contarArquivos(Path diretorio) throws IOException {
        try (var stream = Files.list(diretorio)) {
            return stream.count();
        }
    }

    // --- armazenar (AUD-01, AUD-03, AUD-04, AUD-05, AUD-07, AUD-08) ---

    @Test
    void armazenarComBytesValidosGravaArquivoEDevolveReferenciaNaoNula(@TempDir Path tempDir) throws IOException {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);

        String referencia = adapter.armazenar(new byte[] {1, 2, 3}, "audio/webm");

        assertNotNull(referencia);
        assertEquals(1, contarArquivos(tempDir));
    }

    @Test
    void armazenarDuasVezesComMesmoConteudoDevolveReferenciasDiferentes(@TempDir Path tempDir) {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);
        byte[] conteudo = {1, 2, 3};

        String referencia1 = adapter.armazenar(conteudo, "audio/webm");
        String referencia2 = adapter.armazenar(conteudo, "audio/webm");

        assertNotEquals(referencia1, referencia2);
    }

    @Test
    void armazenarComMimeTypeForaDaListaLancaAudioFormatoInvalidoExceptionSemGravar(@TempDir Path tempDir)
            throws IOException {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);

        assertThrows(
                AudioFormatoInvalidoException.class, () -> adapter.armazenar(new byte[] {1}, "image/png"));
        assertEquals(0, contarArquivos(tempDir));
    }

    @Test
    void armazenarComMimeTypeNuloLancaAudioFormatoInvalidoException(@TempDir Path tempDir) {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);

        assertThrows(AudioFormatoInvalidoException.class, () -> adapter.armazenar(new byte[] {1}, null));
    }

    @Test
    void armazenarComMimeTypeVazioLancaAudioFormatoInvalidoException(@TempDir Path tempDir) {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);

        assertThrows(AudioFormatoInvalidoException.class, () -> adapter.armazenar(new byte[] {1}, ""));
    }

    @Test
    void armazenarComTamanhoZeroLancaAudioTamanhoInvalidoExceptionSemGravar(@TempDir Path tempDir)
            throws IOException {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);

        assertThrows(
                AudioTamanhoInvalidoException.class, () -> adapter.armazenar(new byte[0], "audio/webm"));
        assertEquals(0, contarArquivos(tempDir));
    }

    @Test
    void armazenarComTamanhoAcimaDoLimiteLancaAudioTamanhoInvalidoExceptionSemGravar(@TempDir Path tempDir)
            throws IOException {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);
        byte[] conteudoGrande = new byte[(int) TAMANHO_MAXIMO_BYTES + 1];

        assertThrows(
                AudioTamanhoInvalidoException.class, () -> adapter.armazenar(conteudoGrande, "audio/webm"));
        assertEquals(0, contarArquivos(tempDir));
    }

    @Test
    void inicializarCriaDiretorioConfiguradoSeNaoExistir(@TempDir Path tempDir) {
        Path diretorioAindaNaoExiste = tempDir.resolve("audios");
        assertTrue(Files.notExists(diretorioAindaNaoExiste));

        criarAdapter(diretorioAindaNaoExiste);

        assertTrue(Files.isDirectory(diretorioAindaNaoExiste));
    }

    @Test
    void armazenarComFalhaDeEscritaLancaAudioArmazenamentoExceptionSemDeixarArquivoNovo(@TempDir Path tempDir)
            throws IOException {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);
        long antes = contarArquivos(tempDir);

        assertTrue(tempDir.toFile().setWritable(false), "não foi possível tornar o diretório de teste somente leitura");
        try {
            assertThrows(
                    AudioArmazenamentoException.class, () -> adapter.armazenar(new byte[] {1, 2, 3}, "audio/wav"));
        } finally {
            assertTrue(tempDir.toFile().setWritable(true), "falha ao restaurar permissão de escrita do diretório de teste");
        }

        assertEquals(antes, contarArquivos(tempDir));
    }

    // --- recuperar (AUD-02, AUD-06, Risks & Concerns) ---

    @Test
    void recuperarComReferenciaDeArmazenarDevolveBytesIdenticosParaWebm(@TempDir Path tempDir) {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);
        byte[] original = {10, 20, 30, 40, 50};

        String referencia = adapter.armazenar(original, "audio/webm");
        byte[] recuperado = adapter.recuperar(referencia);

        assertArrayEquals(original, recuperado);
    }

    @Test
    void recuperarComReferenciaDeArmazenarDevolveBytesIdenticosParaWav(@TempDir Path tempDir) {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);
        byte[] original = new byte[500];
        Arrays.fill(original, (byte) 7);

        String referencia = adapter.armazenar(original, "audio/wav");
        byte[] recuperado = adapter.recuperar(referencia);

        assertArrayEquals(original, recuperado);
    }

    @Test
    void recuperarComReferenciaInexistenteLancaAudioNaoEncontradoException(@TempDir Path tempDir) {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);

        assertThrows(
                AudioNaoEncontradoException.class, () -> adapter.recuperar("nao-existe.webm"));
    }

    @Test
    void recuperarComReferenciaTentandoEscaparDoDiretorioLancaAudioNaoEncontradoException(@TempDir Path tempDir)
            throws IOException {
        AudioStorageLocalAdapter adapter = criarAdapter(tempDir);
        // Um arquivo real fora do diretório configurado, para confirmar que o adapter não o lê.
        Path foraDoDiretorio = tempDir.resolveSibling("fora-do-diretorio.webm");
        Files.write(foraDoDiretorio, new byte[] {1, 2, 3});

        try {
            assertThrows(
                    AudioNaoEncontradoException.class,
                    () -> adapter.recuperar("../" + foraDoDiretorio.getFileName()));
        } finally {
            Files.deleteIfExists(foraDoDiretorio);
        }
    }
}
