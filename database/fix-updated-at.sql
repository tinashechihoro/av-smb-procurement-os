DO $$
DECLARE t TEXT;
BEGIN
    FOR t IN
        SELECT table_name FROM information_schema.columns
        WHERE column_name = 'created_at' AND table_schema = 'public'
        EXCEPT
        SELECT table_name FROM information_schema.columns
        WHERE column_name = 'updated_at' AND table_schema = 'public'
    LOOP
        EXECUTE format('ALTER TABLE %I ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now()', t);
        RAISE NOTICE 'Added updated_at to %', t;
    END LOOP;
END $$;
