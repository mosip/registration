-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.0.5
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: 03-Jan-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.individual_demographic_dedup ADD COLUMN mobile_number character varying(64);

ALTER TABLE regprc.individual_demographic_dedup ADD COLUMN email character varying(512);

ALTER TABLE regprc.individual_demographic_dedup ADD COLUMN pincode character varying(64);


ALTER TABLE regprc.abis_request DROP CONSTRAINT IF EXISTS uk_abisreq;
----------------------------------------------------------------------------------------------------