-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.2
-- Purpose    		: Revoking Database Alter deployement done for release in Registration ProcessorDB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Mar-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Apr-2021		Ram Bhatt	   Added resume_remove_tags column to registration table
-----------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.registration DROP COLUMN IF EXISTS resume_timestamp;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS default_resume_action;

--------------------------------------------------------------------------------------------
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS resume_remove_tags;
----------------------------------------------------------------------------------------------------
