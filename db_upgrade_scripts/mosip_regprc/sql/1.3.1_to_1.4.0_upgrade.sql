\c mosip_regprc

INSERT INTO regprc.transaction_type (code, descr, lang_code, is_active, cr_by, cr_dtimes)
VALUES ('CREATE_DRAFT', 'transaction done', 'eng', TRUE, 'MOSIP_SYSTEM', now())
ON CONFLICT (code, lang_code) DO NOTHING;