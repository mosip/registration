-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.1.5
-- Purpose    		: Revoking Database Alter deployement done for release in Registration ProcessorDB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin


ALTER TABLE regprc.reg_manual_verification DROP COLUMN res_text;

----------------------------------------------------------------------------------------------------
