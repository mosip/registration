

\c mosip_regprc sysadmin

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS matched_score;

ALTER TABLE regprc.reg_manual_verification ADD COLUMN IF NOT EXISTS ref_regtrn_id character varying(36);

ALTER TABLE regprc.reg_manual_verification ADD COLUMN IF NOT EXISTS request_id character varying(36);

ALTER TABLE regprc.reg_manual_verification ADD COLUMN IF NOT EXISTS res_text bytea;

----------------------------------------------------------------------------------------------------
