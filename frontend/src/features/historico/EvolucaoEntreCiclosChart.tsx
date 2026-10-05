const L = 520
const A = 240
const M = { top: 16, right: 24, bottom: 36, left: 40 }

export interface PontoCiclo {
  rotulo: string
  corretas: number | null
}

/** Gráfico de linha: eixo X = ciclo (Entrada → Acompanhamento → Saída), eixo Y = palavras corretas do resultado final do ciclo. */
export function EvolucaoEntreCiclosChart({ pontos }: { pontos: PontoCiclo[] }) {
  const max = Math.max(1, ...pontos.map((p) => p.corretas ?? 0))
  const x = (i: number) => M.left + (i / Math.max(1, pontos.length - 1)) * (L - M.left - M.right)
  const y = (v: number) => A - M.bottom - (v / max) * (A - M.top - M.bottom)
  const preenchidos = pontos.flatMap((p, i) => (p.corretas === null ? [] : [{ ...p, i, corretas: p.corretas }]))

  return (
    <figure style={{ margin: 0 }}>
      <svg
        viewBox={`0 0 ${L} ${A}`}
        role="img"
        aria-label="Gráfico de evolução de corretas entre ciclos"
        style={{ width: '100%', maxWidth: 640, height: 'auto' }}
      >
        {[0, 0.5, 1].map((f) => (
          <g key={f}>
            <line x1={M.left} x2={L - M.right} y1={y(max * f)} y2={y(max * f)} stroke="currentColor" opacity={0.15} />
            <text x={M.left - 6} y={y(max * f) + 4} fontSize={11} textAnchor="end" fill="currentColor">
              {Math.round(max * f)}
            </text>
          </g>
        ))}
        {pontos.map((p, i) => (
          <text key={p.rotulo} x={x(i)} y={A - 14} fontSize={11} textAnchor="middle" fill="currentColor">
            {p.rotulo}
          </text>
        ))}
        <g stroke="#2563eb" fill="#2563eb">
          {preenchidos.length > 1 ? (
            <polyline fill="none" strokeWidth={2} points={preenchidos.map((p) => `${x(p.i)},${y(p.corretas)}`).join(' ')} />
          ) : null}
          {preenchidos.map((p) => (
            <circle key={p.rotulo} cx={x(p.i)} cy={y(p.corretas)} r={4}>
              <title>{`${p.rotulo}: ${p.corretas} corretas`}</title>
            </circle>
          ))}
        </g>
      </svg>
    </figure>
  )
}
