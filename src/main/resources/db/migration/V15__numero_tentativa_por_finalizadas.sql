UPDATE avaliacao a
JOIN (
    SELECT a2.id, 1 + COUNT(b.id) AS n
    FROM avaliacao a2
    LEFT JOIN avaliacao b
        ON COALESCE(b.refeita_de_id, b.id) = COALESCE(a2.refeita_de_id, a2.id)
        AND b.status = 'FINALIZADA'
        AND b.id < a2.id
    WHERE a2.status = 'FINALIZADA'
    GROUP BY a2.id
) t ON t.id = a.id
SET a.numero_tentativa = t.n;
