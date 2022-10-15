-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.1.4
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: Dec-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS matched_score;

ALTER TABLE regprc.reg_manual_verification ADD COLUMN IF NOT EXISTS ref_regtrn_id character varying(36);

ALTER TABLE regprc.reg_manual_verification ADD COLUMN IF NOT EXISTS request_id character varying(36);

ALTER TABLE regprc.reg_manual_verification ADD COLUMN IF NOT EXISTS res_text bytea;

----------------------------------------------------------------------------------------------------