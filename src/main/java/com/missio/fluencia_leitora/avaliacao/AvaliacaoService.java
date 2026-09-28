package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoRequest;
import com.missio.fluencia_leitora.bancopalavras.ListaPalavras;
import com.missio.fluencia_leitora.bancopalavras.ListaPalavrasRepository;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.MatriculaRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.ConfiguracaoAvaliacao;
import com.missio.fluencia_leitora.cadastros.anoletivo.ConfiguracaoAvaliacaoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.security.PertencimentoProfessorGuard;
import com.missio.fluencia_leitora.common.texto.TokenizadorTexto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Ciclo de vida da avaliação de leitura (design.md, Components). Esta
 * versão cobre a criação (AVA-01..AVA-08).
 */
@Service
public class AvaliacaoService {

    private static final int TEMPO_MINIMO_SEGUNDOS = 10;
    private static final int TEMPO_MAXIMO_SEGUNDOS = 600;
    private static final int SERIE_PROIBE_NAO_CANONICA = 1;
    private static final int LIMITE_CARACTERES_PALAVRA = 60;
    private static final Pattern FORMATO_PALAVRA = Pattern.compile("^[\\p{L}-]+$");

    private final AvaliacaoRepository avaliacaoRepository;
    private final MatriculaRepository matriculaRepository;
    private final AnoLetivoRepository anoLetivoRepository;
    private final ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository;
    private final ListaPalavrasRepository listaPalavrasRepository;
    private final CicloRepository cicloRepository;
    private final PertencimentoProfessorGuard pertencimentoProfessorGuard;

    public AvaliacaoService(
            AvaliacaoRepository avaliacaoRepository,
            MatriculaRepository matriculaRepository,
            AnoLetivoRepository anoLetivoRepository,
            ConfiguracaoAvaliacaoRepository configuracaoAvaliacaoRepository,
            ListaPalavrasRepository listaPalavrasRepository,
            CicloRepository cicloRepository,
            PertencimentoProfessorGuard pertencimentoProfessorGuard) {
        this.avaliacaoRepository = avaliacaoRepository;
        this.matriculaRepository = matriculaRepository;
        this.anoLetivoRepository = anoLetivoRepository;
        this.configuracaoAvaliacaoRepository = configuracaoAvaliacaoRepository;
        this.listaPalavrasRepository = listaPalavrasRepository;
        this.cicloRepository = cicloRepository;
        this.pertencimentoProfessorGuard = pertencimentoProfessorGuard;
    }

    /**
     * AVA-01..AVA-08: valida o aluno, o tempo, a data e o conteúdo, copia as
     * palavras (ordem 1..n, {@code PENDENTE}) e as cópias da matrícula ativa,
     * e grava a avaliação como {@code CRIADA}.
     */
    @Transactional
    public Avaliacao criar(NovaAvaliacaoRequest request) {
        Matricula matricula = buscarMatriculaAvaliavel(request.alunoId());
        AnoLetivo anoLetivo = matricula.getAnoLetivo();
        int serie = matricula.getSerie();

        validarTempo(request.tempoSegundos());
        validarData(request.dataAvaliacao(), anoLetivo);
        Ciclo ciclo = cicloRepository.findById(request.cicloId())
                .orElseThrow(() -> referenciaInvalida("cicloId inválido"));

        List<PalavraDados> palavras = resolverConteudo(request, serie);
        validarSerieCanonica(serie, palavras);
        validarQuantidade(anoLetivo.getId(), serie, palavras.size());

        Professor professor = matricula.getProfessor();
        Avaliacao avaliacao = new Avaliacao(
                matricula.getAluno(),
                professor,
                professor == null ? null : professor.getNome(),
                matricula.getTurma(),
                matricula.getTurma().getNome(),
                serie,
                anoLetivo,
                ciclo,
                request.tipoLeitura(),
                request.dataAvaliacao(),
                request.tempoSegundos());
        palavras.forEach(palavra -> avaliacao.adicionarPalavra(palavra.palavra(), palavra.tipoPalavra()));

        return avaliacaoRepository.save(avaliacao);
    }

    /**
     * AVA-02: matrícula no ano letivo ATIVO, sem {@code anoFinalizado}, com
     * aluno ativo. AUTH-09: o professor precisa ser o da matrícula - uma
     * matrícula sem professor nunca casa com o professor que chama, então
     * também responde 404 (tasks.md, T12).
     */
    private Matricula buscarMatriculaAvaliavel(Long alunoId) {
        Matricula matricula = anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO).stream()
                .findFirst()
                .flatMap(anoAtivo -> matriculaRepository.findByAlunoIdAndAnoLetivoId(alunoId, anoAtivo.getId()))
                .orElseThrow(this::alunoNaoAvaliavel);

        pertencimentoProfessorGuard.verificar(
                matricula.getProfessor() == null ? null : matricula.getProfessor().getId());

        if (matricula.isAnoFinalizado() || !matricula.getAluno().isAtivo()) {
            throw alunoNaoAvaliavel();
        }
        return matricula;
    }

    /** AVA-05: 10-600 s (o {@code @Min}/{@code @Max} do DTO cobre a mesma regra na borda HTTP). */
    private void validarTempo(int tempoSegundos) {
        if (tempoSegundos < TEMPO_MINIMO_SEGUNDOS || tempoSegundos > TEMPO_MAXIMO_SEGUNDOS) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "VALIDACAO_INVALIDA",
                    "tempoSegundos deve estar entre " + TEMPO_MINIMO_SEGUNDOS + " e " + TEMPO_MAXIMO_SEGUNDOS);
        }
    }

    /** AVA-06: nunca no futuro e dentro do período do ano letivo ATIVO. */
    private void validarData(LocalDate data, AnoLetivo anoLetivo) {
        if (data.isAfter(LocalDate.now())
                || data.isBefore(anoLetivo.getDataInicio())
                || data.isAfter(anoLetivo.getDataFim())) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "DATA_AVALIACAO_INVALIDA",
                    "dataAvaliacao não pode ser futura e deve estar dentro do ano letivo ativo");
        }
    }

    /**
     * AVA-04: exatamente uma fonte entre lista, palavras digitadas e texto
     * (só em TEXTO_CURTO). AVA-08: o texto é tokenizado com a regra do
     * banco de palavras (PAL-08), e cada palavra digitada ou gerada pelo
     * texto tem o formato validado.
     */
    private List<PalavraDados> resolverConteudo(NovaAvaliacaoRequest request, int serie) {
        boolean temLista = request.listaPalavrasId() != null;
        boolean temPalavras = request.palavras() != null && !request.palavras().isEmpty();
        boolean temTexto = request.texto() != null && !request.texto().isBlank();
        int fontes = (temLista ? 1 : 0) + (temPalavras ? 1 : 0) + (temTexto ? 1 : 0);

        if (fontes != 1 || (temTexto && request.tipoLeitura() != TipoLeituraCodigo.TEXTO_CURTO)) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CONTEUDO_INVALIDO",
                    "Informe exatamente uma fonte de conteúdo: listaPalavrasId, palavras ou texto (só em TEXTO_CURTO)");
        }

        if (temLista) {
            return palavrasDaLista(request.listaPalavrasId(), serie, request.tipoLeitura());
        }

        List<PalavraDados> palavras = temPalavras
                ? request.palavras().stream().map(p -> new PalavraDados(p.palavra(), p.tipoPalavra())).toList()
                : TokenizadorTexto.tokenizar(request.texto()).stream().map(t -> new PalavraDados(t, null)).toList();
        validarFormato(palavras);
        return palavras;
    }

    /** AVA-04 (AC 5): a lista precisa ser da série da matrícula e do tipo de leitura da avaliação. */
    private List<PalavraDados> palavrasDaLista(Long listaId, int serie, TipoLeituraCodigo tipoLeitura) {
        ListaPalavras lista = listaPalavrasRepository.findById(listaId)
                .orElseThrow(() -> referenciaInvalida("listaPalavrasId inválido"));

        if (lista.getSerie() != serie || lista.getTipoLeitura() != tipoLeitura) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "LISTA_INCOMPATIVEL",
                    "A lista é de série ou tipo de leitura diferentes dos da avaliação");
        }
        return lista.getItens().stream().map(item -> new PalavraDados(item.getPalavra(), item.getTipoPalavra())).toList();
    }

    /** AVA-08 (AC 10): não vazia, até 60 caracteres, só letras e hífen; a exceção traz a posição (1..n). */
    private void validarFormato(List<PalavraDados> palavras) {
        for (int i = 0; i < palavras.size(); i++) {
            String palavra = palavras.get(i).palavra();
            if (palavra == null
                    || palavra.length() > LIMITE_CARACTERES_PALAVRA
                    || !FORMATO_PALAVRA.matcher(palavra).matches()) {
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "PALAVRA_INVALIDA",
                        "Palavra inválida: vazia, com mais de " + LIMITE_CARACTERES_PALAVRA
                                + " caracteres ou com caracteres diferentes de letras e hífen",
                        Map.of("posicao", i + 1));
            }
        }
    }

    /** AVA-07: no 1º ano, nenhuma palavra com tipo informado NAO_CANONICA; a exceção traz as posições (AD-008). */
    private void validarSerieCanonica(int serie, List<PalavraDados> palavras) {
        if (serie != SERIE_PROIBE_NAO_CANONICA) {
            return;
        }

        List<Integer> posicoesInvalidas = IntStream.range(0, palavras.size())
                .filter(i -> palavras.get(i).tipoPalavra() == TipoPalavra.NAO_CANONICA)
                .mapToObj(i -> i + 1)
                .toList();

        if (!posicoesInvalidas.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "NAO_CANONICA_PROIBIDA_1_ANO",
                    "Avaliação do 1º ano não pode conter palavra não canônica",
                    Map.of("posicoes", posicoesInvalidas));
        }
    }

    /**
     * AVA-03: quantidade dentro do mín./máx. do ano letivo e da série. Toda
     * série de todo ano letivo tem configuração (criada junto com o ano,
     * {@code AnoLetivoService}).
     */
    private void validarQuantidade(Long anoLetivoId, int serie, int informado) {
        ConfiguracaoAvaliacao configuracao =
                configuracaoAvaliacaoRepository.findByAnoLetivoIdAndSerie(anoLetivoId, serie).orElseThrow();

        if (informado < configuracao.getQuantidadeMinima() || informado > configuracao.getQuantidadeMaxima()) {
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "QUANTIDADE_PALAVRAS_FORA_DO_LIMITE",
                    "Quantidade de palavras fora do limite configurado para a série",
                    Map.of(
                            "minimo", configuracao.getQuantidadeMinima(),
                            "maximo", configuracao.getQuantidadeMaxima(),
                            "informado", informado));
        }
    }

    private BusinessException alunoNaoAvaliavel() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "ALUNO_NAO_AVALIAVEL",
                "Aluno sem matrícula ativa no ano letivo ativo, com ano finalizado ou inativo");
    }

    private BusinessException referenciaInvalida(String mensagem) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "REFERENCIA_INVALIDA", mensagem);
    }

    /** Uma palavra resolvida da fonte de conteúdo, antes de virar {@link PalavraAvaliacao}. */
    private record PalavraDados(String palavra, TipoPalavra tipoPalavra) {
    }
}
