ALTER TABLE avaliacao ADD COLUMN numero_tentativa INT NOT NULL DEFAULT 1;

UPDATE avaliacao a
JOIN (
    SELECT a2.id, 2 + COUNT(b.id) AS n
    FROM avaliacao a2
    LEFT JOIN avaliacao b ON b.refeita_de_id = a2.refeita_de_id AND b.id < a2.id
    WHERE a2.refeita_de_id IS NOT NULL
    GROUP BY a2.id
) t ON t.id = a.id
SET a.numero_tentativa = t.n;
