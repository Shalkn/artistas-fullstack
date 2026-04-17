-- Ambientes em que `title` foi criado como BYTEA (Hibernate/DDL legado): PostgreSQL não possui lower(bytea).
-- Converte para VARCHAR preservando UTF-8; se já for varchar, não altera.
DO $$
BEGIN
  IF EXISTS (
    SELECT 1
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND table_name = 'album'
      AND column_name = 'title'
      AND udt_name = 'bytea'
  ) THEN
    ALTER TABLE album
      ALTER COLUMN title TYPE VARCHAR(500)
      USING convert_from(title, 'UTF8');
  END IF;
END $$;
