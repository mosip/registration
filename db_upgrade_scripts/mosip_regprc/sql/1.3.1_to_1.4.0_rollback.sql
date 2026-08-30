\c mosip_regprc

-- CREATE_DRAFT is additive master data. Safe to leave in transaction_type after
-- application rollback. DELETE is omitted because live registration_transaction
-- rows may reference this code (FK fk_regtrn_trntyp).
