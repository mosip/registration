-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.1.4
-- Purpose    		: Revoking Database Alter deployement done for release in Registration ProcessorDB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: Dec-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS ref_regtrn_id;

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS request_id;

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS res_text;

----------------------------------------------------------------------------------------------------