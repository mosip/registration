-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Mar-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE reg_prc.registration ADD COLUMN resume_timestamp timestamp;
ALTER TABLE reg_prc.registration ADD COLUMN default_resume_action character varying(36);

----------------------------------------------------------------------------------------------------
