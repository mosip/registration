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

--need to drop now after reverting to old columns add old constraint

ALTER TABLE regprc.individual_demographic_dedup DROP CONSTRAINT IF EXISTS fk_idemogd_reg CASCADE;
ALTER TABLE regprc.reg_manual_verification DROP CONSTRAINT IF EXISTS fk_rmnlver_reg CASCADE;
ALTER TABLE regprc.reg_bio_ref DROP CONSTRAINT IF EXISTS fk_regref_reg CASCADE;
ALTER TABLE regprc.reg_lost_uin_det DROP CONSTRAINT IF EXISTS fk_rlostd_reg CASCADE;
ALTER TABLE regprc.registration_transaction DROP CONSTRAINT IF EXISTS fk_regtrn_reg CASCADE;

DROP TABLE IF EXISTS regprc.additional_info_request CASCADE;
DROP TABLE IF EXISTS regprc.anonymous_profile CASCADE;
DROP TABLE IF EXISTS regprc.crypto_salt CASCADE;
DROP TABLE IF EXISTS regprc.reg_verification CASCADE;

ALTER TABLE regprc.registration_list RENAME COLUMN workflow_instance_id TO id;
ALTER TABLE regprc.registration_list RENAME COLUMN process TO reg_type;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS additional_info_req_id;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS  packet_id;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS sourc;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS ref_id;

ALTER TABLE regprc.individual_demographic_dedup DROP COLUMN IF EXISTS workflow_instance_id;
ALTER TABLE regprc.individual_demographic_dedup DROP COLUMN IF EXISTS process;
ALTER TABLE regprc.individual_demographic_dedup DROP COLUMN IF EXISTS iteration;

ALTER TABLE regprc.reg_manual_verification DROP COLUMN IF EXISTS workflow_instance_id;

ALTER TABLE regprc.reg_lost_uin_det DROP COLUMN IF EXISTS workflow_instance_id;

ALTER TABLE regprc.reg_bio_ref DROP COLUMN IF EXISTS workflow_instance_id;
ALTER TABLE regprc.reg_bio_ref DROP COLUMN IF EXISTS process;
ALTER TABLE regprc.reg_bio_ref DROP COLUMN IF EXISTS iteration;

ALTER TABLE regprc.registration RENAME COLUMN reg_id TO id;
ALTER TABLE regprc.registration RENAME COLUMN process TO reg_type;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS workflow_instance_id;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS source;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS iteration;

DROP INDEX IF EXISTS idx_rbioref_crdtimes;
DROP INDEX IF EXISTS idx_bio_ref_id;
DROP INDEX IF EXISTS idx_idemogd_namedobgender;
DROP INDEX IF EXISTS idx_rbioref_crdtimes;
DROP INDEX IF EXISTS idx_rmanvrn_reqid;
DROP INDEX IF EXISTS idx_rgstrn_ltstrbcode_ltststscode;
DROP INDEX IF EXISTS idx_reg_latest_trn_dtimes;
DROP INDEX IF EXISTS idx_rgstrnlst_pcktid;
DROP INDEX IF EXISTS idx_rgstrnlst_aireqid;
DROP INDEX IF EXISTS idx_reg_verification_reqId;
DROP INDEX IF EXISTS idx_reg_trn_reg_id;
DROP INDEX IF EXISTS idx_reg_trn_status_code;
DROP INDEX IF EXISTS idx_reg_trn_trntypecode;
DROP INDEX IF EXISTS idx_reg_trn_upd_dtimes;
DROP INDEX IF EXISTS idx_user_detail_cntr_id;
DROP INDEX IF EXISTS idx_abis_req_regtrn_id;

ALTER TABLE regprc.individual_demographic_dedup DROP CONSTRAINT IF EXISTS pk_idemogd_id;
ALTER TABLE regprc.individual_demographic_dedup ALTER COLUMN reg_id SET NOT NULL;
ALTER TABLE regprc.individual_demographic_dedup ADD CONSTRAINT pk_idemogd_id PRIMARY KEY (reg_id,lang_code);

ALTER TABLE regprc.reg_bio_ref DROP CONSTRAINT IF EXISTS pk_regbref_id;
ALTER TABLE regprc.reg_bio_ref ALTER COLUMN reg_id SET NOT NULL;
ALTER TABLE regprc.reg_bio_ref ADD CONSTRAINT pk_regbref_id PRIMARY KEY (reg_id);

ALTER TABLE regprc.reg_lost_uin_det DROP CONSTRAINT IF EXISTS pk_rlostd;
ALTER TABLE regprc.reg_lost_uin_det ALTER COLUMN reg_id SET NOT NULL;
ALTER TABLE regprc.reg_lost_uin_det ADD CONSTRAINT pk_rlostd PRIMARY KEY (reg_id);

ALTER TABLE regprc.reg_manual_verification DROP CONSTRAINT IF EXISTS pk_rmnlver_id;
ALTER TABLE regprc.reg_manual_verification ALTER COLUMN reg_id SET NOT NULL;
ALTER TABLE regprc.reg_manual_verification ADD CONSTRAINT pk_rmnlver_id PRIMARY KEY (reg_id,matched_ref_id,matched_ref_type);

ALTER TABLE regprc.registration DROP CONSTRAINT IF EXISTS pk_reg_id CASCADE;
ALTER TABLE regprc.registration ALTER COLUMN id SET NOT NULL;
ALTER TABLE regprc.registration ADD CONSTRAINT pk_reg_id PRIMARY KEY (id);

ALTER TABLE regprc.registration_list DROP CONSTRAINT IF EXISTS pk_reglist_id;
ALTER TABLE regprc.registration_list ALTER COLUMN id SET NOT NULL;
ALTER TABLE regprc.registration_list ADD CONSTRAINT pk_reglist_id PRIMARY KEY (id);

ALTER TABLE regprc.individual_demographic_dedup ADD CONSTRAINT fk_idemogd_reg FOREIGN KEY (reg_id)
REFERENCES regprc.registration (id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_manual_verification ADD CONSTRAINT fk_rmnlver_reg FOREIGN KEY (reg_id)
REFERENCES regprc.registration (id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_bio_ref ADD CONSTRAINT fk_regbrf_reg FOREIGN KEY (reg_id)
REFERENCES regprc.registration (id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_lost_uin_det ADD CONSTRAINT fk_rlostd_reg FOREIGN KEY (reg_id)
REFERENCES regprc.registration (id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.registration DROP COLUMN IF EXISTS resume_timestamp;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS default_resume_action;
ALTER TABLE regprc.registration DROP COLUMN IF EXISTS pause_rule_ids;

ALTER TABLE regprc.registration_transaction DROP CONSTRAINT IF EXISTS fk_regtrn_trntyp ;
ALTER TABLE regprc.reg_manual_verification  DROP CONSTRAINT IF EXISTS fk_rmnlver_trntyp ;
ALTER TABLE regprc.reg_demo_dedupe_list  DROP CONSTRAINT IF EXISTS fk_regded_regtrn ;

TRUNCATE TABLE regprc.transaction_type cascade ;

\COPY regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes) FROM './dml/regprc-transaction_type_rollback.csv' delimiter ',' HEADER  csv;

UPDATE regprc.registration_transaction SET trn_type_code='QUALITY_CHECK' WHERE trn_type_code='QUALITY_CLASSIFIER';
UPDATE regprc.reg_manual_verification SET trntyp_code='QUALITY_CHECK' WHERE trntyp_code='QUALITY_CLASSIFIER';

ALTER TABLE regprc.registration_transaction ADD CONSTRAINT  fk_regtrn_trntyp FOREIGN KEY (trn_type_code,lang_code)
REFERENCES regprc.transaction_type (code,lang_code) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_manual_verification ADD CONSTRAINT  fk_rmnlver_trntyp FOREIGN KEY (trntyp_code,lang_code)
REFERENCES regprc.transaction_type (code,lang_code) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_demo_dedupe_list ADD CONSTRAINT  fk_regded_regtrn FOREIGN KEY (regtrn_id)
REFERENCES regprc.registration_transaction (id) MATCH FULL
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.registration DROP COLUMN IF EXISTS last_success_stage_name;
-------------------------------------------------------drop columns to registration_list----------------------------------------------

ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS name;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS phone;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS email;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS center_id;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS registration_date;
ALTER TABLE regprc.registration_list DROP COLUMN IF EXISTS location_code;