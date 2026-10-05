import type { TentativasCiclo } from './tentativas'

const CORES: Record<string, string> = {
  ENTRADA: '#2563eb',
  ACOMPANHAMENTO: '#d97706',
  SAIDA: '#059669',
}
const ROTULOS: Record<string, string> = {
  ENTRADA: 'Entrada',
  ACOMPANHAMENTO: 'Acompanhamento',
  SAIDA: 'Saída',
}

const L = 520
const A = 260
const M = { top: 16, right: 16, bottom: 36, left: 40 }

/** Gráfico de barras: eixo X = nº da tentativa, eixo Y = palavras corretas, uma cor por ciclo. */
export function TentativasChart({ ciclos }: { ciclos: TentativasCiclo[] }) {
  const maxTentativas = Math.max(1, ...ciclos.map((c) => c.tentativas.length))
  const maxCorretas = Math.max(1, ...ciclos.flatMap((c) => c.tentativas.map((t) => t.quantidadeCorretas)))
  const largura = L - M.left - M.right
  const faixa = largura / maxTentativas
  const barra = Math.min(48, faixa * 0.6)
  const xCentro = (n: number) => M.left + faixa * (n - 0.5)
  const y = (v: number) => A - M.bottom - (v / maxCorretas) * (A - M.top - M.bottom)

  return (
    <figure style={{ margin: 0 }}>
      <svg
        viewBox={`0 0 ${L} ${A}`}
        role="img"
        aria-label="Gráfico de barras de corretas por tentativa, por ciclo"
        style={{ width: '100%', maxWidth: 640, height: 'auto' }}
      >
        {[0, 0.5, 1].map((f) => (
          <g key={f}>
            <line x1={M.left} x2={L - M.right} y1={y(maxCorretas * f)} y2={y(maxCorretas * f)} stroke="currentColor" opacity={0.15} />
            <text x={M.left - 6} y={y(maxCorretas * f) + 4} fontSize={11} textAnchor="end" fill="currentColor">
              {Math.round(maxCorretas * f)}
            </text>
          </g>
        ))}
        {Array.from({ length: maxTentativas }, (_, i) => i + 1).map((n) => (
          <text key={n} x={xCentro(n)} y={A - 14} fontSize={11} textAnchor="middle" fill="currentColor">
            {n}ª
          </text>
        ))}
        {ciclos.map(({ ciclo, tentativas }) => (
          <g key={ciclo} fill={CORES[ciclo]}>
            {tentativas.map((t) => (
              <g key={t.avaliacaoId}>
                <rect
                  x={xCentro(t.numero) - barra / 2}
                  y={y(t.quantidadeCorretas)}
                  width={barra}
                  height={A - M.bottom - y(t.quantidadeCorretas)}
                  rx={3}
                >
                  <title>{`${ROTULOS[ciclo]} - ${t.numero}ª tentativa: ${t.quantidadeCorretas} corretas`}</title>
                </rect>
                <text x={xCentro(t.numero)} y={y(t.quantidadeCorretas) - 4} fontSize={11} textAnchor="middle" fill="currentColor">
                  {t.quantidadeCorretas}
                </text>
              </g>
            ))}
          </g>
        ))}
      </svg>
      <figcaption style={{ display: 'flex', gap: 16, flexWrap: 'wrap', fontSize: 13 }}>
        {ciclos.map(({ ciclo }) => (
          <span key={ciclo}>
            <span aria-hidden style={{ color: CORES[ciclo] }}>■ </span>
            {ROTULOS[ciclo]} - corretas por tentativa
          </span>
        ))}
      </figcaption>
    </figure>
  )
}
