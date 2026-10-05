package com.missio.fluencia_leitora.avaliacao;

import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoProgramadaRequest;
import com.missio.fluencia_leitora.avaliacao.dto.NovaAvaliacaoRequest;
import com.missio.fluencia_leitora.avaliacao.dto.PalavraDigitadaRequest;
import com.missio.fluencia_leitora.bancopalavras.TipoPalavra;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.SituacaoAnoLetivo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.common.error.BusinessException;
import com.missio.fluencia_leitora.common.texto.TokenizadorTexto;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * O coordenador configura avaliações por série no ano letivo ativo; cada
 * aluno da série as vê como pendentes ({@link AvaliacaoService#listarPendentes}).
 */
@Service
public class AvaliacaoProgramadaService {

    private final AvaliacaoProgramadaRepository repository;
    private final AnoLetivoRepository anoLetivoRepository;
    private final CicloRepository cicloRepository;
    private final AvaliacaoService avaliacaoService;

    public AvaliacaoProgramadaService(
            AvaliacaoProgramadaRepository repository,
            AnoLetivoRepository anoLetivoRepository,
            CicloRepository cicloRepository,
            AvaliacaoService avaliacaoService) {
        this.repository = repository;
        this.anoLetivoRepository = anoLetivoRepository;
        this.cicloRepository = cicloRepository;
        this.avaliacaoService = avaliacaoService;
    }

    @Transactional
    public AvaliacaoProgramada criar(NovaAvaliacaoProgramadaRequest request) {
        AnoLetivo anoLetivo = resolverAno(request.anoLetivoId());
        int serie = request.serie();

        List<PalavraDigitadaRequest> digitadas = request.palavras() == null || request.palavras().isBlank()
                ? null
                : TokenizadorTexto.tokenizar(request.palavras()).stream()
                        .map(p -> new PalavraDigitadaRequest(p, TipoPalavra.CANONICA))
                        .toList();
        avaliacaoService.validarConfiguracaoDaSerie(anoLetivo, serie, new NovaAvaliacaoRequest(
                null, request.tipoLeitura(), request.cicloId(), null, request.tempoSegundos(),
                request.listaPalavrasId(), digitadas, request.texto()));

        AvaliacaoProgramada programada = new AvaliacaoProgramada(
                request.nome().trim(),
                anoLetivo,
                serie,
                cicloRepository.getReferenceById(request.cicloId()),
                request.tipoLeitura(),
                request.tempoSegundos(),
                request.listaPalavrasId(),
                digitadas == null ? null : String.join(" ", digitadas.stream().map(PalavraDigitadaRequest::palavra).toList()),
                request.texto() == null || request.texto().isBlank() ? null : request.texto());
        programada.setMaxRefazeres(request.maxRefazeres());
        return repository.save(programada);
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoProgramada> listar(Integer serie, Long anoLetivoId) {
        AnoLetivo anoLetivo = resolverAno(anoLetivoId);
        return serie == null
                ? repository.findByAnoLetivoIdAndAtivaTrueOrderBySerieAscIdAsc(anoLetivo.getId())
                : repository.findByAnoLetivoIdAndSerieAndAtivaTrueOrderByIdAsc(anoLetivo.getId(), serie);
    }

    /** Remove da lista de pendentes de todos; as avaliações já iniciadas seguem normalmente. */
    @Transactional
    public void desativar(Long id) {
        AvaliacaoProgramada programada = repository.findById(id)
                .filter(AvaliacaoProgramada::isAtiva)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "Avaliação programada não encontrada"));
        programada.setAtiva(false);
    }

    private AnoLetivo resolverAno(Long anoLetivoId) {
        if (anoLetivoId == null) {
            return anoAtivo();
        }
        return anoLetivoRepository.findById(anoLetivoId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "RECURSO_NAO_ENCONTRADO", "Ano letivo não encontrado"));
    }

    private AnoLetivo anoAtivo() {
        return anoLetivoRepository.findBySituacao(SituacaoAnoLetivo.ATIVO).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY, "SEM_ANO_LETIVO_ATIVO", "Não há ano letivo ativo"));
    }
}
