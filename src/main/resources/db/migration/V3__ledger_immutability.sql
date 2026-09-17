CREATE OR REPLACE FUNCTION prevent_ledger_entry_mutation()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION
        'ledger_entries are immutable: % operation is not allowed',
        TG_OP;
END;
$$;

CREATE TRIGGER trg_ledger_entries_immutable
BEFORE UPDATE OR DELETE
ON ledger_entries
FOR EACH ROW
EXECUTE FUNCTION prevent_ledger_entry_mutation();
