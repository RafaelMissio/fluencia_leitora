package com.missio.fluencia_leitora.historicoevolucao;

import com.missio.fluencia_leitora.autenticacao.Usuario;
import com.missio.fluencia_leitora.autenticacao.UsuarioRepository;
import com.missio.fluencia_leitora.avaliacao.Avaliacao;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAudio;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoAudioRepository;
import com.missio.fluencia_leitora.avaliacao.AvaliacaoRepository;
import com.missio.fluencia_leitora.avaliacao.StatusAvaliacao;
import com.missio.fluencia_leitora.bancopalavras.TipoLeituraCodigo;
import com.missio.fluencia_leitora.cadastros.aluno.Aluno;
import com.missio.fluencia_leitora.cadastros.aluno.AlunoRepository;
import com.missio.fluencia_leitora.cadastros.aluno.Matricula;
import com.missio.fluencia_leitora.cadastros.aluno.MatriculaRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivo;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoRepository;
import com.missio.fluencia_leitora.cadastros.anoletivo.AnoLetivoService;
import com.missio.fluencia_leitora.cadastros.dominio.Ciclo;
import com.missio.fluencia_leitora.cadastros.dominio.CicloRepository;
import com.missio.fluencia_leitora.cadastros.professor.Professor;
import com.missio.fluencia_leitora.cadastros.professor.ProfessorRepository;
import com.missio.fluencia_leitora.cadastros.turma.Turma;
import com.missio.fluencia_leitora.cadastros.turma.TurmaRepository;
import com.missio.fluencia_leitora.common.security.JwtService;
import com.missio.fluencia_leitora.common.security.Perfil;
import com.missio.fluencia_leitora.regrasclassificacao.Fase;
import com.missio.fluencia_leitora.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * T6: {@code GET /alunos/{alunoId}/historico-avaliacoes} contra MySQL
 * real - caminho feliz com todos os campos do SDD §16, filtros isolados e
 * combinados, erros de conversão (400), pertencimento (404/200) e 401 sem
 * token (design.md, Error Handling Strategy).
 */
@AutoConfigureMockMvc
@Transactional
class HistoricoEvolucaoControllerIT extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnoLetivoRepository anoLetivoRepository;

    @Autowired
    private AnoLetivoService anoLetivoService;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private ProfessorRepository professorRepository;

    @Autowired
    private AlunoRepository alunoRepository;

    @Autowired
    private MatriculaRepository matriculaRepository;

    @Autowired
    private AvaliacaoRepository avaliacaoRepository;

    @Autowired
    private AvaliacaoAudioRepository avaliacaoAudioRepository;

    @Autowired
    private CicloRepository cicloRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    private String bearerProfessor(Professor professor) {
        Usuario usuario = usuarioRepository.save(new Usuario(
                "prof-hist-" + UUID.randomUUID() + "@escola.com", "hash-nao-usado", Perfil.PROFESSOR, professor.getId()));
        return "Bearer " + jwtService.emitir(usuario.getId());
    }

    /**
     * SPEC_DEVIATION: usa {@code AnoLetivoService.ativar} (não {@code
     * setSituacao} direto) porque CAD-04 permite só um ATIVO por vez - o
     * banco é compartilhado com outras *ControllerIT que não fazem
     * rollback (mesmo padrão de {@code AlunoControllerIT}), então setar
     * ATIVO sem encerrar o anterior deixaria mais de um ATIVO visível
     * quando `anoLetivoId` é omitido em `evolucao-ciclos` (T7).
     */
    private AnoLetivo novoAnoLetivoAtivo(int ano) {
        AnoLetivo anoLetivo = anoLetivoRepository.save(new AnoLetivo(ano, LocalDate.of(ano, 2, 1), LocalDate.of(ano, 12, 15)));
        return anoLetivoService.ativar(anoLetivo.getId());
    }

    private Matricula novaMatriculaAtiva(AnoLetivo anoLetivo, Professor professor) {
        Turma turma = turmaRepository.save(new Turma("Turma Historico IT", 2, anoLetivo, professor));
        Aluno aluno = alunoRepository.save(new Aluno("Aluno Historico IT"));
        return matriculaRepository.save(new Matricula(aluno, anoLetivo, turma, 2, professor));
    }

    private Avaliacao novaAvaliacaoFinalizada(
            Matricula matricula, Ciclo ciclo, TipoLeituraCodigo tipoLeitura, LocalDate dataAvaliacao) {
        Avaliacao avaliacao = new Avaliacao(
                matricula.getAluno(), matricula.getProfessor(), matricula.getProfessor().getNome(),
                matricula.getTurma(), matricula.getTurma().getNome(), matricula.getSerie(), matricula.getAnoLetivo(),
                ciclo, tipoLeitura, dataAvaliacao, 60);
        avaliacao.adicionarPalavra("gato", null);
        avaliacao.adicionarPalavra("casa", null);
        avaliacao.setStatus(StatusAvaliacao.FINALIZADA);
        avaliacao.setFinalizadoEm(Instant.now());
        avaliacao.setQuantidadeCorretas(2);
        avaliacao.setQuantidadeIncorretas(0);
        avaliacao.setQuantidadeNaoLidas(0);
        avaliacao.setPercentualAcerto(new BigDecimal("100.00"));
        avaliacao.setFase(Fase.LEITOR_FLUENTE);
        avaliacao.setNivel(null);
        avaliacao.setTempoUtilizadoSegundos(45);
        return avaliacaoRepository.saveAndFlush(avaliacao);
    }

    private Ciclo primeiroCiclo() {
        return cicloRepository.findAll().get(0);
    }

    @Test
    void historicoRetornaTodosOsCamposDoSdd16ParaProfessorDono() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3300);
        Professor professor = professorRepository.save(new Professor("Professor Historico"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        Avaliacao avaliacao = novaAvaliacaoFinalizada(matricula, primeiroCiclo(), TipoLeituraCodigo.PALAVRA, LocalDate.of(3300, 3, 1));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].avaliacaoId").value(avaliacao.getId()))
                .andExpect(jsonPath("$.content[0].anoLetivo").value(3300))
                .andExpect(jsonPath("$.content[0].serie").value(2))
                .andExpect(jsonPath("$.content[0].turma").value(matricula.getTurma().getNome()))
                .andExpect(jsonPath("$.content[0].professor").value(professor.getNome()))
                .andExpect(jsonPath("$.content[0].ciclo").value(primeiroCiclo().getCodigo()))
                .andExpect(jsonPath("$.content[0].tipoLeitura").value("PALAVRA"))
                .andExpect(jsonPath("$.content[0].dataAvaliacao").value("3300-03-01"))
                .andExpect(jsonPath("$.content[0].quantidadeTotal").value(2))
                .andExpect(jsonPath("$.content[0].quantidadeCorretas").value(2))
                .andExpect(jsonPath("$.content[0].quantidadeIncorretas").value(0))
                .andExpect(jsonPath("$.content[0].quantidadeNaoLidas").value(0))
                .andExpect(jsonPath("$.content[0].percentualAcerto").value(100.00))
                .andExpect(jsonPath("$.content[0].fase").value("LEITOR_FLUENTE"))
                .andExpect(jsonPath("$.content[0].nivel").doesNotExist())
                .andExpect(jsonPath("$.content[0].tempoUtilizadoSegundos").value(45))
                .andExpect(jsonPath("$.content[0].temAudio").value(false));
    }

    @Test
    void historicoComAudioMarcaTemAudioTrue() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3301);
        Professor professor = professorRepository.save(new Professor("Professor Historico Audio"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        Avaliacao avaliacao = novaAvaliacaoFinalizada(matricula, primeiroCiclo(), TipoLeituraCodigo.PALAVRA, LocalDate.of(3301, 3, 1));
        avaliacaoAudioRepository.save(new AvaliacaoAudio(avaliacao, "ref.wav", "audio/wav", 1024L));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].temAudio").value(true));
    }

    @Test
    void historicoFiltraPorAnoLetivoIdIsolado() throws Exception {
        AnoLetivo ano1 = novoAnoLetivoAtivo(3302);
        Professor professor = professorRepository.save(new Professor("Professor Filtro Ano"));
        Matricula matricula1 = novaMatriculaAtiva(ano1, professor);
        Avaliacao doAno1 = novaAvaliacaoFinalizada(matricula1, primeiroCiclo(), TipoLeituraCodigo.PALAVRA, LocalDate.of(3302, 3, 1));

        AnoLetivo ano2 = anoLetivoRepository.save(new AnoLetivo(3303, LocalDate.of(3303, 2, 1), LocalDate.of(3303, 12, 15)));
        Turma turma2 = turmaRepository.save(new Turma("Turma Ano2", 2, ano2, professor));
        Matricula matricula2 = matriculaRepository.save(new Matricula(matricula1.getAluno(), ano2, turma2, 2, professor));
        novaAvaliacaoFinalizada(matricula2, primeiroCiclo(), TipoLeituraCodigo.PALAVRA, LocalDate.of(3303, 3, 1));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula1.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("anoLetivoId", ano1.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].avaliacaoId").value(doAno1.getId()));
    }

    @Test
    void historicoFiltraPorTipoLeituraIsolado() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3304);
        Professor professor = professorRepository.save(new Professor("Professor Filtro Tipo"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        Avaliacao dePalavra = novaAvaliacaoFinalizada(matricula, primeiroCiclo(), TipoLeituraCodigo.PALAVRA, LocalDate.of(3304, 3, 1));
        novaAvaliacaoFinalizada(matricula, primeiroCiclo(), TipoLeituraCodigo.TEXTO_CURTO, LocalDate.of(3304, 3, 2));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].avaliacaoId").value(dePalavra.getId()));
    }

    @Test
    void historicoFiltraPorCicloIdIsolado() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3305);
        Professor professor = professorRepository.save(new Professor("Professor Filtro Ciclo"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        List<Ciclo> ciclos = cicloRepository.findAll();
        Avaliacao doCiclo0 = novaAvaliacaoFinalizada(matricula, ciclos.get(0), TipoLeituraCodigo.PALAVRA, LocalDate.of(3305, 3, 1));
        novaAvaliacaoFinalizada(matricula, ciclos.get(1), TipoLeituraCodigo.PALAVRA, LocalDate.of(3305, 3, 2));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("cicloId", ciclos.get(0).getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].avaliacaoId").value(doCiclo0.getId()));
    }

    @Test
    void historicoFiltrosCombinados() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3306);
        Professor professor = professorRepository.save(new Professor("Professor Filtro Combinado"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        List<Ciclo> ciclos = cicloRepository.findAll();
        Avaliacao alvo = novaAvaliacaoFinalizada(matricula, ciclos.get(0), TipoLeituraCodigo.PALAVRA, LocalDate.of(3306, 3, 1));
        novaAvaliacaoFinalizada(matricula, ciclos.get(1), TipoLeituraCodigo.PALAVRA, LocalDate.of(3306, 3, 2));
        novaAvaliacaoFinalizada(matricula, ciclos.get(0), TipoLeituraCodigo.TEXTO_CURTO, LocalDate.of(3306, 3, 3));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("anoLetivoId", anoLetivo.getId().toString())
                        .param("tipoLeitura", "PALAVRA")
                        .param("cicloId", ciclos.get(0).getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.content[0].avaliacaoId").value(alvo.getId()));
    }

    @Test
    void historicoComTipoLeituraInvalidoRetorna400() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3307);
        Professor professor = professorRepository.save(new Professor("Professor 400 Tipo"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("tipoLeitura", "NAO_EXISTE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historicoComCicloIdInvalidoRetorna400() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3308);
        Professor professor = professorRepository.save(new Professor("Professor 400 Ciclo"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("cicloId", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void historicoComProfessorNaoDonoRetorna404() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3309);
        Professor dono = professorRepository.save(new Professor("Professor Dono"));
        Professor outro = professorRepository.save(new Professor("Professor Outro"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, dono);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(outro)))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicoComCoordenadorRetorna200ParaQualquerAluno() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3310);
        Professor professor = professorRepository.save(new Professor("Professor Qualquer"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerCoordenador()))
                .andExpect(status().isOk());
    }

    @Test
    void historicoComAlunoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", 999999999L)
                        .header("Authorization", bearerCoordenador()))
                .andExpect(status().isNotFound());
    }

    @Test
    void historicoSemTokenRetorna401() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3311);
        Professor professor = professorRepository.save(new Professor("Professor Sem Token"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void historicoComAlunoSemFinalizadaRetorna200ComPaginaVazia() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3312);
        Professor professor = professorRepository.save(new Professor("Professor Sem Finalizada"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/historico-avaliacoes", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", org.hamcrest.Matchers.hasSize(0)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private Ciclo cicloPorCodigo(String codigo) {
        return cicloRepository.findAll().stream()
                .filter(ciclo -> ciclo.getCodigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void evolucaoCiclosRetornaOsTresCiclosParaAnoETipoPedidos() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3320);
        Professor professor = professorRepository.save(new Professor("Professor Evolucao Ciclos"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        novaAvaliacaoFinalizada(matricula, cicloPorCodigo("ENTRADA"), TipoLeituraCodigo.PALAVRA, LocalDate.of(3320, 3, 1));
        novaAvaliacaoFinalizada(matricula, cicloPorCodigo("ACOMPANHAMENTO"), TipoLeituraCodigo.PALAVRA, LocalDate.of(3320, 6, 1));
        novaAvaliacaoFinalizada(matricula, cicloPorCodigo("SAIDA"), TipoLeituraCodigo.PALAVRA, LocalDate.of(3320, 11, 1));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .header("Authorization", bearerCoordenador())
                        .param("anoLetivoId", anoLetivo.getId().toString())
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alunoId").value(matricula.getAluno().getId()))
                .andExpect(jsonPath("$.anoLetivo").value(3320))
                .andExpect(jsonPath("$.tipoLeitura").value("PALAVRA"))
                .andExpect(jsonPath("$.entrada.ciclo").value("ENTRADA"))
                .andExpect(jsonPath("$.entrada.quantidadeCorretas").value(2))
                .andExpect(jsonPath("$.acompanhamento.ciclo").value("ACOMPANHAMENTO"))
                .andExpect(jsonPath("$.saida.ciclo").value("SAIDA"));
    }

    @Test
    void evolucaoCiclosComCicloAusenteVemNulo() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3321);
        Professor professor = professorRepository.save(new Professor("Professor Ciclo Ausente"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        novaAvaliacaoFinalizada(matricula, cicloPorCodigo("ENTRADA"), TipoLeituraCodigo.PALAVRA, LocalDate.of(3321, 3, 1));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .header("Authorization", bearerCoordenador())
                        .param("anoLetivoId", anoLetivo.getId().toString())
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entrada.ciclo").value("ENTRADA"))
                .andExpect(jsonPath("$.acompanhamento").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.saida").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void evolucaoCiclosSemTipoLeituraRetorna400() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3322);
        Professor professor = professorRepository.save(new Professor("Professor Sem Tipo"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .header("Authorization", bearerCoordenador())
                        .param("anoLetivoId", anoLetivo.getId().toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void evolucaoCiclosComAnoLetivoInexistenteRetorna404() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3323);
        Professor professor = professorRepository.save(new Professor("Professor Ano Inexistente"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .header("Authorization", bearerCoordenador())
                        .param("anoLetivoId", "999999999")
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ANO_LETIVO_NAO_ENCONTRADO"));
    }

    @Test
    void evolucaoCiclosComAnoLetivoOmitidoUsaOAnoAtivo() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3324);
        Professor professor = professorRepository.save(new Professor("Professor Ano Ativo"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);
        novaAvaliacaoFinalizada(matricula, cicloPorCodigo("ENTRADA"), TipoLeituraCodigo.PALAVRA, LocalDate.of(3324, 3, 1));

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .header("Authorization", bearerCoordenador())
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.anoLetivo").value(3324));
    }

    @Test
    void evolucaoCiclosComProfessorRetorna403() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3325);
        Professor professor = professorRepository.save(new Professor("Professor Sem Acesso"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .header("Authorization", bearerProfessor(professor))
                        .param("anoLetivoId", anoLetivo.getId().toString())
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isForbidden());
    }

    @Test
    void evolucaoCiclosComAlunoInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", 999999999L)
                        .header("Authorization", bearerCoordenador())
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isNotFound());
    }

    @Test
    void evolucaoCiclosSemTokenRetorna401() throws Exception {
        AnoLetivo anoLetivo = novoAnoLetivoAtivo(3326);
        Professor professor = professorRepository.save(new Professor("Professor Sem Token Evolucao"));
        Matricula matricula = novaMatriculaAtiva(anoLetivo, professor);

        mockMvc.perform(get("/api/v1/alunos/{alunoId}/evolucao-ciclos", matricula.getAluno().getId())
                        .param("tipoLeitura", "PALAVRA"))
                .andExpect(status().isUnauthorized());
    }
}
