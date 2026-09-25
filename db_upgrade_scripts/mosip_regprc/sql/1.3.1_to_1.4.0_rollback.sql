\c mosip_regprc

-- CREATE_DRAFT is additive master data. Safe to leave in transaction_type after
-- application rollback. DELETE is omitted because live registration_transaction
-- rows may reference this code (FK fk_regtrn_trntyp).

ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS packet_meta_data;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS packet_operations_data;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS packet_captured_devices;
