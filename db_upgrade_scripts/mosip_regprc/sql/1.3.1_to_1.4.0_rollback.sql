\c mosip_regprc

DELETE FROM regprc.transaction_type
WHERE code = 'CREATE_DRAFT'
  AND lang_code = 'eng'
  AND cr_by = 'MOSIP_SYSTEM';