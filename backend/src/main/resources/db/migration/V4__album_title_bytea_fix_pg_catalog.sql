-- Complemento à V3: detecta bytea via catálogo do PostgreSQL (mais confiável que information_schema em alguns casos).
DO $$
DECLARE
  coltype text;
BEGIN
  SELECT format_type(a.atttypid, a.atttypmod)
  INTO coltype
  FROM pg_attribute a
  JOIN pg_class c ON a.attrelid = c.oid
  JOIN pg_namespace n ON c.relnamespace = n.oid
  WHERE n.nspname = 'public'
    AND c.relname = 'album'
    AND a.attname = 'title'
    AND a.attnum > 0
    AND NOT a.attisdropped;

  IF coltype = 'bytea' THEN
    ALTER TABLE album
      ALTER COLUMN title TYPE VARCHAR(500)
      USING convert_from(title, 'UTF8');
  END IF;
END $$;
