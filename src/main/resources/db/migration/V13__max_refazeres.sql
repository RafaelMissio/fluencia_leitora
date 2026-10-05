ALTER TABLE avaliacao_programada ADD COLUMN max_refazeres INT NOT NULL DEFAULT 3;
ALTER TABLE avaliacao ADD COLUMN max_refazeres INT NOT NULL DEFAULT 3;
