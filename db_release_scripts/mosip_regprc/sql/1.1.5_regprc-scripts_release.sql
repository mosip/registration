-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.1.5
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Created By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Mar-2021		Ram Bhatt	    Reverting is_deleted not null changes for 1.1.5
-- Apr-2021		Ram Bhatt	    Packet Classification dml Changes
----------------------------------------------------------------------------------------------------

\c mosip_regprc sysadmin

TRUNCATE TABLE regprc.transaction_type cascade ;

\COPY regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes) FROM '../dml/regprc-transaction_type.csv' delimiter ',' HEADER csv;

ALTER TABLE regprc.reg_manual_verification ADD COLUMN res_text bytea;


----------------------------------------------------------------------------------------------------
