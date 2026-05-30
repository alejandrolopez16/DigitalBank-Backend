-- Script seguro para crear vistas y triggers sin modificar el backend Java.
-- Base: esquema public de PostgreSQL.

BEGIN;

-- =====================================================================
-- VIEWS
-- =====================================================================

CREATE OR REPLACE VIEW public.vw_customer_accounts AS
SELECT
    c.document_number,
    c.name AS customer_name,
    c.email,
    c.status AS customer_status,
    fa.id AS financial_account_id,
    fa.balance,
    fa.status AS account_status,
    fa.created_at AS account_created_at
FROM public.customers c
INNER JOIN public.financial_accounts fa
    ON fa.customer_document = c.document_number;

CREATE OR REPLACE VIEW public.vw_transaction_details AS
SELECT
    t.reference,
    t.source_account,
    t.destination_account,
    t.amount,
    t.type,
    t.status,
    t.description,
    t.created_at,
    src_c.document_number AS source_customer_document,
    src_c.name AS source_customer_name,
    dst_c.document_number AS destination_customer_document,
    dst_c.name AS destination_customer_name
FROM public.transactions t
LEFT JOIN public.financial_accounts src_fa
    ON src_fa.id = t.source_account
LEFT JOIN public.customers src_c
    ON src_c.document_number = src_fa.customer_document
LEFT JOIN public.financial_accounts dst_fa
    ON dst_fa.id = t.destination_account
LEFT JOIN public.customers dst_c
    ON dst_c.document_number = dst_fa.customer_document;

CREATE OR REPLACE VIEW public.vw_transaction_audit_details AS
SELECT
    a.id,
    a.transaction_reference,
    a.source_account_id,
    a.destination_account_id,
    a.source_customer_document,
    a.destination_customer_document,
    a.operation_type,
    a.amount,
    a.balance_before_source,
    a.balance_after_source,
    a.balance_before_destination,
    a.balance_after_destination,
    a.status,
    a.created_at
FROM public.transaction_audit_logs a;

-- =====================================================================
-- TRIGGERS
-- =====================================================================

-- Mantiene actualizado el timestamp de seguridad automáticamente.
CREATE OR REPLACE FUNCTION public.fn_security_policies_set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at := now();
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_security_policies_set_updated_at ON public.security_policies;
CREATE TRIGGER trg_security_policies_set_updated_at
BEFORE UPDATE ON public.security_policies
FOR EACH ROW
EXECUTE FUNCTION public.fn_security_policies_set_updated_at();

-- Evita que el log de auditoría se modifique o elimine.
CREATE OR REPLACE FUNCTION public.fn_prevent_transaction_audit_log_modification()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'transaction_audit_logs es inmutable; la operación % no está permitida.', TG_OP;
END;
$$;

DROP TRIGGER IF EXISTS trg_prevent_transaction_audit_log_update ON public.transaction_audit_logs;
DROP TRIGGER IF EXISTS trg_prevent_transaction_audit_log_delete ON public.transaction_audit_logs;

CREATE TRIGGER trg_prevent_transaction_audit_log_update
BEFORE UPDATE ON public.transaction_audit_logs
FOR EACH ROW
EXECUTE FUNCTION public.fn_prevent_transaction_audit_log_modification();

CREATE TRIGGER trg_prevent_transaction_audit_log_delete
BEFORE DELETE ON public.transaction_audit_logs
FOR EACH ROW
EXECUTE FUNCTION public.fn_prevent_transaction_audit_log_modification();

COMMIT;