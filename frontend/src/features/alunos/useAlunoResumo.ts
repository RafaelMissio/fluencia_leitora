import { useQuery } from '@tanstack/react-query'
import { request } from '../../api/client'
import type { AlunoBuscaItem, EvolucaoCiclosResponse, HistoricoAvaliacaoItem, Page } from '../../api/types'
import { useAuth } from '../../auth/AuthContext'
import { calcularCicloAtual, type CicloAtualCodigo } from './cicloAtual'

const QUANTIDADE_ULTIMAS_AVALIACOES = 5

export interface UltimaClassificacao {
  fase: string | null
  nivel: number | null
}

export interface AlunoResumo {
  aluno: AlunoBuscaItem | undefined
  cicloAtual: CicloAtualCodigo
  ultimasAvaliacoes: HistoricoAvaliacaoItem[]
  ultimaClassificacao: UltimaClassificacao | null
  /** Variação de `quantidadeCorretas` entre as 2 avaliações mais recentes comparáveis; `null` quando não há par (painel exibe "—"). */
  evolucao: number | null
  isLoading: boolean
  error: unknown
}

/**
 * context.md, "Acesso a evolução": para o PROFESSOR, a evolução no ano é
 * calculada no cliente comparando `quantidadeCorretas` das 2 avaliações mais
 * recentes do mesmo ciclo+tipoLeitura dentro do histórico já buscado (nunca
 * chama `evolucao-ciclos`, que é COORDENADOR-only e devolveria 403).
 *
 * Entre todos os pares (ciclo, tipoLeitura) com 2+ ocorrências em `itens`
 * (ordenado do mais recente para o mais antigo), escolhe o par cuja
 * ocorrência mais recente aparece primeiro na lista (SDD/context.md: "o
 * ciclo mais recente com 2 avaliações").
 */
function calcularEvolucaoProfessor(itens: HistoricoAvaliacaoItem[]): number | null {
  const primeiraOcorrenciaPorChave = new Map<string, number>()
  const segundaOcorrenciaPorChave = new Map<string, number>()

  itens.forEach((item, index) => {
    const chave = `${item.ciclo}|${item.tipoLeitura}`
    if (!primeiraOcorrenciaPorChave.has(chave)) {
      primeiraOcorrenciaPorChave.set(chave, index)
    } else if (!segundaOcorrenciaPorChave.has(chave)) {
      segundaOcorrenciaPorChave.set(chave, index)
    }
  })

  let melhorChave: string | null = null
  let melhorIndex = Number.POSITIVE_INFINITY
  for (const [chave, index] of primeiraOcorrenciaPorChave) {
    if (segundaOcorrenciaPorChave.has(chave) && index < melhorIndex) {
      melhorChave = chave
      melhorIndex = index
    }
  }

  if (melhorChave === null) {
    return null
  }

  const atual = itens[primeiraOcorrenciaPorChave.get(melhorChave)!]
  const anterior = itens[segundaOcorrenciaPorChave.get(melhorChave)!]
  return atual.quantidadeCorretas - anterior.quantidadeCorretas
}

/**
 * context.md: "Para COORDENADOR, o resumo pode usar `evolucao-ciclos`
 * diretamente (dado já autorizado)." A resposta não traz um delta pronto -
 * compara os dois ciclos preenchidos mais recentes (ENTRADA -> ACOMPANHAMENTO
 * -> SAIDA), a partir do par adjacente mais tardio disponível.
 */
function calcularEvolucaoCoordenador(resposta: EvolucaoCiclosResponse): number | null {
  const emOrdem = [resposta.entrada, resposta.acompanhamento, resposta.saida]

  for (let i = emOrdem.length - 1; i > 0; i--) {
    const atual = emOrdem[i]
    const anterior = emOrdem[i - 1]
    if (atual && anterior) {
      return atual.quantidadeCorretas - anterior.quantidadeCorretas
    }
  }

  return null
}

/**
 * Compõe o painel de resumo do aluno (EVO-01, spec.md P1 "Buscar aluno", AC2)
 * a partir de `GET /alunos/{id}` + `GET /alunos/{id}/historico-avaliacoes` e,
 * só para COORDENADOR, `GET /alunos/{id}/evolucao-ciclos` (context.md,
 * "Acesso a evolução" - PROFESSOR recebe 403 desse endpoint).
 */
export function useAlunoResumo(alunoId: number): AlunoResumo {
  const { perfil } = useAuth()

  const alunoQuery = useQuery({
    queryKey: ['aluno-resumo', 'aluno', alunoId],
    queryFn: () => request<AlunoBuscaItem>(`/alunos/${alunoId}`),
  })

  const historicoQuery = useQuery({
    queryKey: ['aluno-resumo', 'historico', alunoId],
    queryFn: () => request<Page<HistoricoAvaliacaoItem>>(`/alunos/${alunoId}/historico-avaliacoes?page=0`),
  })

  const todos = historicoQuery.data?.content ?? []
  // Inativas (refeitas) são listadas, mas não entram em ciclo atual, classificação nem evolução.
  const itens = todos.filter((item) => item.ativa)
  const cicloAtual = calcularCicloAtual(itens)
  const ultimasAvaliacoes = todos.slice(0, QUANTIDADE_ULTIMAS_AVALIACOES)
  const ultimaClassificacao = itens[0] ? { fase: itens[0].fase, nivel: itens[0].nivel } : null
  const tipoLeituraMaisRecente = itens[0]?.tipoLeitura

  const evolucaoCiclosQuery = useQuery({
    queryKey: ['aluno-resumo', 'evolucao-ciclos', alunoId, tipoLeituraMaisRecente],
    queryFn: () =>
      request<EvolucaoCiclosResponse>(
        `/alunos/${alunoId}/evolucao-ciclos?tipoLeitura=${tipoLeituraMaisRecente}`,
      ),
    // Critério crítico (context.md): PROFESSOR NUNCA chama evolucao-ciclos (403).
    enabled: perfil === 'COORDENADOR' && tipoLeituraMaisRecente !== undefined,
  })

  const evolucao =
    perfil === 'COORDENADOR'
      ? evolucaoCiclosQuery.data
        ? calcularEvolucaoCoordenador(evolucaoCiclosQuery.data)
        : null
      : calcularEvolucaoProfessor(itens)

  const isLoading =
    alunoQuery.isLoading ||
    historicoQuery.isLoading ||
    (perfil === 'COORDENADOR' && tipoLeituraMaisRecente !== undefined && evolucaoCiclosQuery.isLoading)

  const error = alunoQuery.error ?? historicoQuery.error ?? evolucaoCiclosQuery.error ?? null

  return {
    aluno: alunoQuery.data,
    cicloAtual,
    ultimasAvaliacoes,
    ultimaClassificacao,
    evolucao,
    isLoading,
    error,
  }
}
