

\c mosip_regprc sysadmin

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS ref_regtrn_id;

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS request_id;

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS res_text;

----------------------------------------------------------------------------------------------------
