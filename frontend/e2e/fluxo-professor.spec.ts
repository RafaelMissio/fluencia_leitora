import { expect, test } from '@playwright/test'

/**
 * E2E do fluxo completo do professor (SDD §20, spec.md Success Criteria):
 * login → busca do aluno → seleção → configurar avaliação → criar → tela de
 * execução → conceder microfone falso → iniciar → marcar palavras → esperar
 * o tempo configurado zerar → tela de resultado com os totais e o player de
 * áudio, sem intervenção manual (T29, `Where`/`Reuses`).
 *
 * PRÉ-REQUISITOS (fora do escopo de `frontend-web` - este teste não semeia
 * dados, só consome uma API já rodando):
 *
 * 1. Backend real em `http://localhost:8080` (ex.: `mvn spring-boot:run`)
 *    com MySQL de acordo com RNF001/AD-007, migrações do Flyway aplicadas.
 *    `vite.config.ts` já proxeia `/api` para essa porta (context.md, "CORS
 *    / integração dev") - o `webServer` deste `playwright.config.ts` só sobe
 *    o `npm run dev` do frontend, não o backend Spring Boot.
 * 2. Um usuário PROFESSOR já cadastrado (`POST /api/v1/usuarios`, restrito a
 *    COORDENADOR - não há seed automático nas migrações `V1..V9`, nenhuma
 *    delas insere linhas em `usuario`/`professor`/`aluno`/`turma`), com um
 *    aluno matriculado numa turma/ano letivo ATIVO, e ao menos uma lista de
 *    palavras cadastrada para a série desse aluno e para o tipo de leitura
 *    `PALAVRA`, dentro do mínimo/máximo da configuração da série
 *    (`ConfiguracaoAvaliacaoResponse`, seed padrão de `AnoLetivoService`:
 *    15-20 para a 1ª série, 20-60 para as demais).
 * 3. As credenciais e o nome do aluno são passados via variáveis de
 *    ambiente (nunca hardcoded - não há usuário fixo no banco):
 *    `E2E_PROFESSOR_EMAIL`, `E2E_PROFESSOR_SENHA`, `E2E_ALUNO_NOME` (nome ou
 *    trecho do nome, >= 2 caracteres, que retorne exatamente um resultado na
 *    busca).
 *
 * Sem esses três pontos o teste falha na primeira etapa (login) ou na busca
 * (nenhum aluno encontrado) - falha de dados de ambiente, não do código do
 * frontend.
 */

const PROFESSOR_EMAIL = process.env.E2E_PROFESSOR_EMAIL ?? ''
const PROFESSOR_SENHA = process.env.E2E_PROFESSOR_SENHA ?? ''
const ALUNO_NOME = process.env.E2E_ALUNO_NOME ?? ''

// spec.md, T29 "What": tempo baixo (10s) só para o teste zerar o cronômetro
// rápido - o formulário aceita qualquer valor válido (>0) em `tempo`.
const TEMPO_TESTE_SEGUNDOS = 10

test.describe('Fluxo completo do professor (SDD §20)', () => {
  test.skip(
    !PROFESSOR_EMAIL || !PROFESSOR_SENHA || !ALUNO_NOME,
    'E2E_PROFESSOR_EMAIL / E2E_PROFESSOR_SENHA / E2E_ALUNO_NOME não definidos - ver pré-requisitos no topo deste arquivo',
  )

  test('login → busca → configuração → iniciar → marcar → tempo zerado → resultado → áudio', async ({
    page,
    context,
  }) => {
    await context.grantPermissions(['microphone'])

    // 1. Login (spec.md P1 "Login", AC1)
    await page.goto('/login')
    await page.locator('#login-email').fill(PROFESSOR_EMAIL)
    await page.locator('#login-senha').fill(PROFESSOR_SENHA)
    await page.getByRole('button', { name: 'Entrar' }).click()
    await page.waitForURL('**/avaliar')

    // 2. Busca do aluno + seleção (spec.md P1 "Buscar aluno", AC1/AC2)
    await page.locator('#busca-aluno-nome').fill(ALUNO_NOME)
    const resultado = page.getByRole('button', { name: new RegExp(ALUNO_NOME) }).first()
    await expect(resultado).toBeVisible()
    await resultado.click()

    await expect(page.getByRole('region', { name: 'Resumo do aluno' })).toBeVisible()

    // 3. "Configurar avaliação" (ResumoAlunoPanel → ConfigurarAvaliacaoPage)
    await page.getByRole('button', { name: 'Configurar avaliação' }).click()
    await page.waitForURL('**/avaliacoes/nova**')

    // 4. Preenche o formulário com uma lista de palavras válida (spec.md P1
    // "Configurar avaliação", AC1/AC2) - `ciclo` já vem preenchido (AC1).
    await page.locator('#config-avaliacao-tipo-leitura').selectOption('PALAVRA')
    await expect(page.locator('#config-avaliacao-ciclo')).not.toHaveValue('')
    await page.locator('#config-avaliacao-tempo').fill(String(TEMPO_TESTE_SEGUNDOS))

    const fonteSelect = page.locator('#config-avaliacao-fonte')
    // "Digitar palavras" + ao menos 1 lista cadastrada (as listas chegam de forma assíncrona: espera a 2ª opção)
    await expect(fonteSelect.locator('option').nth(1)).toBeAttached()
    await fonteSelect.selectOption({ index: 1 })

    const criarButton = page.getByRole('button', { name: 'Criar avaliação' })
    await expect(criarButton).toBeEnabled()
    await criarButton.click()

    // 5. Tela de execução (spec.md P1 "Executar avaliação")
    await page.waitForURL('**/avaliacoes/*/executar')

    // 6. Iniciar: pede microfone falso (`--use-fake-device-for-media-stream`,
    // playwright.config.ts) antes de chamar a API `iniciar` (AC1/AC3).
    await page.getByRole('button', { name: 'Iniciar avaliação' }).click()
    await expect(page.getByLabel('Gravando')).toBeVisible()
    await expect(page.getByRole('timer')).toBeVisible()

    // 7. Marcar ao menos uma palavra (spec.md P1 "Marcar palavras", AC1)
    const primeiraPalavra = page.locator('ul li button').first()
    await expect(primeiraPalavra).toBeEnabled()
    await primeiraPalavra.click()
    await expect(primeiraPalavra).toHaveAccessibleName(/Correta/)

    // 8. Espera o cronômetro zerar (TEMPO_TESTE_SEGUNDOS + folga) → navega
    // automaticamente para o resultado (AC6, sem clique em "Finalizar").
    await page.waitForURL('**/avaliacoes/*/resultado', { timeout: (TEMPO_TESTE_SEGUNDOS + 15) * 1000 })

    // 9. Resultado com os totais (AC1) e o player de áudio, enviado
    // automaticamente após finalizar (FE-20/FE-22).
    await expect(page.getByRole('heading', { name: 'Resultado da avaliação' })).toBeVisible()
    await expect(page.locator('dt', { hasText: 'Total' })).toBeVisible()
    await expect(page.locator('audio')).toBeVisible({ timeout: 15000 })
  })
})
