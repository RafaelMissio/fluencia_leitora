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
}

// design.md declara `ResultadoCiclo { ... } | null` - o `| null` descreve como o
// tipo é usado nos campos que o referenciam (entrada/acompanhamento/saida),
// não a declaração da interface em si (TS não permite unir uma `interface` a
// `null` na própria declaração).
export interface ResultadoCiclo {
  ciclo: string
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
  quantidadeCorretas: number
  percentualAcerto: number
  fase: string | null
  nivel: number | null
  evolucao: EvolucaoValor
}

export interface EvolucaoAnualLinha {
  anoLetivo: number
  serie: number
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
export interface CriarAnoLetivoRequest { ano: number; dataInicio: string; dataFim: string }
export interface ConfiguracaoAvaliacaoResponse { id: number; serie: number; quantidadeMinima: number; quantidadeMaxima: number }
export interface AtualizarConfiguracaoRequest { quantidadeMinima: number; quantidadeMaxima: number }
export interface TurmaResponse { id: number; nome: string; serie: number; anoLetivoId: number; professorId: number | null; ativo: boolean }
export interface CriarTurmaRequest { nome: string; serie: number; anoLetivoId: number; professorId?: number | null }
export interface TurmaResumo { id: number; nome: string; serie: number }
export interface ProfessorResponse { id: number; nome: string; ativo: boolean; turmas: TurmaResumo[] }
export interface CriarAlunoRequest { nome: string; turmaId: number }
export interface CriarAlunoResponse { alunoId: number; matriculaId: number }
export interface MatriculaResponse {
  id: number; alunoId: number; anoLetivoId: number; turmaId: number; serie: number
  professorId: number | null; anoFinalizado: boolean
}
