-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.0.9
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Create By   		: Sadanandegowda DM
-- Created Date		: 27-Apr-2020
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

ALTER TABLE regprc.abis_response_det DROP COLUMN score;

INSERT INTO regprc.transaction_type(code, descr, lang_code, is_active, cr_by, cr_dtimes) VALUES ('SECUREZONE_NOTIFICATION', 'transaction notification', 'eng', TRUE, 'MOSIP_SYSTEM', now());
----------------------------------------------------------------------------------------------------