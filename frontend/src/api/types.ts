/**
 * Tipos TS espelhando os DTOs do backend (design.md, Data Models).
 * Nomes de campo idênticos aos DTOs Java (inclusive em português) para que
 * o mapeamento frontend/backend seja direto.
 */

// Autenticação
export type Perfil = 'PROFESSOR' | 'COORDENADOR'

export interface LoginResponse {
  accessToken: string
  expiresIn: number
  perfil: Perfil
  professorId: number | null
}

// Domínio fixo
export type TipoLeituraCodigo = 'PALAVRA' | 'PSEUDOPALAVRA' | 'TEXTO_CURTO'

export interface Ciclo {
  id: number
  codigo: 'ENTRADA' | 'ACOMPANHAMENTO' | 'SAIDA'
  nome: string
}

// Aluno
export interface AlunoBuscaItem {
  alunoId: number
  nome: string
  turma: string | null
  serie: number
  professor: string | null
  anoLetivo: number | null
  situacao: 'EM_ANDAMENTO' | 'FINALIZADO' | null
  /** Status do aluno: ativo ou inativo. */
  ativo?: boolean
  /** Status do aluno no ano letivo ativo. */
  status?: StatusMatricula | null
  /** Matrículas de todos os anos (ativo e inativos), do mais recente para o mais antigo. */
  matriculas?: MatriculaAno[]
}

export type StatusMatricula = 'CURSANDO' | 'APROVADO' | 'REPROVADO'
export interface MatriculaAno {
  matriculaId: number
  anoLetivoId: number
  anoLetivo: number
  situacaoAnoLetivo: 'PLANEJADO' | 'ATIVO' | 'ENCERRADO'
  turmaId: number
  turma: string
  serie: number
  professorId: number | null
  status: StatusMatricula
}

// Avaliação
export type StatusAvaliacao = 'CRIADA' | 'EM_ANDAMENTO' | 'PAUSADA' | 'FINALIZADA' | 'CANCELADA'
export type StatusPalavra = 'PENDENTE' | 'CORRETA' | 'INCORRETA' | 'NAO_LIDA'

export interface PalavraAvaliacao {
  ordem: number
  palavra: string
  tipoPalavra: string
  status: StatusPalavra
}

export interface AvaliacaoResponse {
  id: number
  alunoId: number
  professorId: number | null
  professorNome: string
  turmaId: number
  turmaNome: string
  serie: number
  anoLetivoId: number
  cicloId: number
  tipoLeitura: TipoLeituraCodigo
  dataAvaliacao: string
  tempoConfiguradoSegundos: number
  status: StatusAvaliacao
  iniciadoEm: string | null
  finalizadoEm: string | null
  quantidadeTotal: number
  tempoUtilizadoSegundos: number | null
  quantidadeCorretas: number | null
  quantidadeIncorretas: number | null
  quantidadeNaoLidas: number | null
  quantidadeLidas: number | null
  percentualAcerto: number | null
  fase: string | null
  nivel: number | null
  classificacaoPendente: boolean
  /** `false` quando a avaliação foi refeita (fica só como registro). */
  ativa: boolean
  /** Quantas vezes já foi refeita; `null` quando a resposta não traz a contagem. */
  refeitas: number | null
  maxRefazeres: number
  podeRefazer: boolean | null
  palavras: PalavraAvaliacao[]
}

export interface NovaAvaliacaoRequest {
  alunoId: number
  tipoLeitura: TipoLeituraCodigo
  cicloId: number
  dataAvaliacao: string
  tempoSegundos?: number
  listaPalavrasId?: number
  palavras?: { palavra: string; tipoPalavra: string }[]
  texto?: string
}

/** Avaliação que o aluno ainda precisa fazer; `avaliacaoId`/`status` nulos = configurada para a série e ainda não iniciada. */
export interface AvaliacaoPendente {
  programadaId: number | null
  avaliacaoId: number | null
  nome: string | null
  tipoLeitura: TipoLeituraCodigo
  cicloId: number
  tempoSegundos: number
  status: 'CRIADA' | 'EM_ANDAMENTO' | 'PAUSADA' | null
}

/** Avaliação configurada pelo coordenador para uma série. */
export interface AvaliacaoProgramada {
  id: number
  anoLetivoId: number
  nome: string
  serie: number
  cicloId: number
  tipoLeitura: TipoLeituraCodigo
  tempoSegundos: number
  listaPalavrasId: number | null
  palavras: string | null
  texto: string | null
  maxRefazeres: number
}

export interface NovaAvaliacaoProgramadaRequest {
  anoLetivoId?: number
  nome: string
  serie: number
  tipoLeitura: TipoLeituraCodigo
  cicloId: number
  tempoSegundos?: number
  listaPalavrasId?: number
  palavras?: string
  texto?: string
  maxRefazeres?: number
}

// Histórico / evolução
export interface HistoricoAvaliacaoItem {
  avaliacaoId: number
  anoLetivo: number
  serie: number
  turma: string
  professor: string
  ciclo: string
  tipoLeitura: string
  dataAvaliacao: string
  quantidadeTotal: number
  quantidadeCorretas: number
  quantidadeIncorretas: number
  quantidadeNaoLidas: number
  percentualAcerto: number
  fase: string | null
  nivel: number | null
  tempoUtilizadoSegundos: number | null
  temAudio: boolean
  /** `false` quando a avaliação foi refeita; aparece no histórico mas não entra na evolução. */
  ativa: boolean
  /** Id da primeira avaliação da cadeia de tentativas; `null` na original. */
  refeitaDeId?: number | null
  nomeAvaliacao?: string | null
  numeroTentativa?: number
}

// design.md declara `ResultadoCiclo { ... } | null` - o `| null` descreve como o
// tipo é usado nos campos que o referenciam (entrada/acompanhamento/saida),
// não a declaração da interface em si (TS não permite unir uma `interface` a
// `null` na própria declaração).
export interface ResultadoCiclo {
  ciclo: string
  nomeAvaliacao: string | null
  tentativas: number
  dataAvaliacao: string
  quantidadeCorretas: number
  percentualAcerto: number
  fase: string | null
  nivel: number | null
}

export interface EvolucaoCiclosResponse {
  alunoId: number
  anoLetivo: number
  tipoLeitura: string
  entrada: ResultadoCiclo | null
  acompanhamento: ResultadoCiclo | null
  saida: ResultadoCiclo | null
}

export interface EvolucaoValor {
  absoluta: number | null
  percentual: number | null
}

// Mesma observação de ResultadoCiclo acima quanto ao `| null` de design.md.
export interface CicloAnual {
  ciclo: string
  nomeAvaliacao: string | null
  tentativas: number
  quantidadeCorretas: number
  percentualAcerto: number
  fase: string | null
  nivel: number | null
  evolucao: EvolucaoValor
}

export interface EvolucaoAnualLinha {
  anoLetivo: number
  serie: number
  turma: string | null
  entrada: CicloAnual | null
  acompanhamento: CicloAnual | null
  saida: CicloAnual | null
}

export interface EvolucaoAnualResponse {
  alunoId: number
  tipoLeitura: string
  anos: EvolucaoAnualLinha[]
}

// Envelope de página (Spring Data, usado por busca de aluno e histórico)
export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

// Erro (RFC 7807 + extensões do GlobalExceptionHandler)
export interface ApiError {
  status: number
  code?: string
  detail?: string
  errors?: { field: string; message: string }[]
}

// Cadastros (P2)
export interface AnoLetivoResponse {
  id: number
  ano: number
  dataInicio: string
  dataFim: string
  situacao: string
  ativo: boolean
}
export interface CriarAnoLetivoRequest { ano: number; dataInicio: string; dataFim: string; situacao?: string }
export interface ConfiguracaoAvaliacaoResponse { id: number; serie: number; quantidadeMinima: number; quantidadeMaxima: number }
export interface AtualizarConfiguracaoRequest { quantidadeMinima: number; quantidadeMaxima: number }
export interface TurmaResponse { id: number; nome: string; serie: number; anoLetivoId: number; professorId: number | null; ativo: boolean }
export interface AlunoDaTurma { alunoId: number; nome: string; alunoAtivo: boolean; matriculaId: number; status: StatusMatricula }
export interface CriarTurmaRequest { nome: string; serie: number; anoLetivoId: number; professorId?: number | null }
export interface TurmaResumo { id: number; nome: string; serie: number }
export interface ProfessorResponse { id: number; nome: string; ativo: boolean; turmas: TurmaResumo[] }
export interface CriarAlunoRequest { nome: string; turmaId: number }
export interface CriarAlunoResponse { alunoId: number; matriculaId: number }
export interface MatriculaResponse {
  id: number; alunoId: number; anoLetivoId: number; turmaId: number; serie: number
  professorId: number | null; anoFinalizado: boolean; status: StatusMatricula
}
export interface UsuarioResponse { id: number; email: string; perfil: Perfil; professorId: number | null; ativo: boolean }
export interface CriarUsuarioRequest { email: string; senha: string; perfil: Perfil; professorId?: number | null }
export type TipoPalavra = 'CANONICA' | 'NAO_CANONICA'
export interface ItemPalavra { palavra: string; tipoPalavra: TipoPalavra }
export interface ListaPalavrasRequest {
  nome: string; serie: number; tipoLeitura: TipoLeituraCodigo; tipoPalavra?: TipoPalavra
  texto?: string; itens?: ItemPalavra[]
}
export interface ListaPalavrasResponse {
  id: number; nome: string; serie: number; tipoLeitura: TipoLeituraCodigo; tipoPalavra: TipoPalavra | null
  texto: string | null; ativo: boolean; quantidadePalavras: number
  itens: (ItemPalavra & { ordem: number })[]
  version: number
}
export type Fase = 'PRE_LEITOR' | 'LEITOR_INICIANTE' | 'LEITOR_FLUENTE'
export interface FaixaRequest {
  quantidadeMinimaAcertos: number; quantidadeMaximaAcertos: number | null; fase: Fase; nivel: number | null
}
export interface RegraClassificacaoResponse extends FaixaRequest { id: number; serie: number; ativo: boolean }
