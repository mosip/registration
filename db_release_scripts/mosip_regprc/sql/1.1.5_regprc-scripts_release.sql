-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.1.5
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Created By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         	Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Mar-2021		Ram Bhatt	    	Reverting is_deleted not null changes for 1.1.5
-- Apr-2021		Ram Bhatt	    	Packet Classification dml Changes
-- Sep-2021		Chandra Keshav Mishra	updated changes for transaction_type table
----------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

--ALTER TABLE regprc.registration_list ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.reg_manual_verification ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.abis_application ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.abis_request ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.abis_response ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.abis_response_det ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.reg_demo_dedupe_list ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.registration_transaction ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.reg_lost_uin_det ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.reg_bio_ref ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.individual_demographic_dedup ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.registration ALTER COLUMN is_deleted SET NOT NULL;
--ALTER TABLE regprc.transaction_type ALTER COLUMN is_deleted SET NOT NULL;

--ALTER TABLE regprc.registration_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.reg_manual_verification ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.abis_application ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.abis_request ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.abis_response ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.abis_response_det ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.reg_demo_dedupe_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.registration_transaction ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.reg_lost_uin_det ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.reg_bio_ref ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.individual_demographic_dedup ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.registration ALTER COLUMN is_deleted SET DEFAULT FALSE;
--ALTER TABLE regprc.transaction_type ALTER COLUMN is_deleted SET DEFAULT FALSE;


ALTER TABLE regprc.reg_manual_verification ADD COLUMN res_text bytea;

INSERT INTO regprc.transaction_type (code, descr, lang_code, is_active, cr_by, cr_dtimes, upd_by, upd_dtimes, is_deleted, del_dtimes) Select 'PACKET_CLASSIFICATION', 'transcation done', 'eng', true, 'MOSIP_SYSTEM', now(), null, null, null, null WHERE NOT EXISTS (SELECT code FROM regprc.transaction_type WHERE code = 'PACKET_CLASSIFICATION');



----------------------------------------------------------------------------------------------------
