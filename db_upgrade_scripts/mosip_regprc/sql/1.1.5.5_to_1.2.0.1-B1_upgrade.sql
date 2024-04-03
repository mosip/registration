\c mosip_regprc

REASSIGN OWNED BY sysadmin TO postgres;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA regprc FROM regprcuser;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA regprc FROM sysadmin;

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA regprc TO regprcuser;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA regprc TO postgres;

----------------------------------------------Multiple table level changes on regprc db-------------------------------------------------------

ALTER TABLE regprc.individual_demographic_dedup DROP CONSTRAINT IF EXISTS fk_idemogd_reg CASCADE;
ALTER TABLE regprc.reg_manual_verification DROP CONSTRAINT IF EXISTS fk_rmnlver_reg CASCADE;
ALTER TABLE regprc.reg_bio_ref DROP CONSTRAINT IF EXISTS fk_regref_reg CASCADE;
ALTER TABLE regprc.reg_lost_uin_det DROP CONSTRAINT IF EXISTS fk_rlostd_reg CASCADE;
ALTER TABLE regprc.registration_transaction DROP CONSTRAINT IF EXISTS fk_regtrn_reg CASCADE;

CREATE TABLE regprc.additional_info_request(
	additional_info_process character varying(64),
	reg_id character varying(39),
	workflow_instance_id character varying(36),
	timestamp timestamp,
	additional_info_iteration integer,
	additional_info_req_id character varying(256),
	CONSTRAINT pk_addl_info_req PRIMARY KEY (workflow_instance_id , additional_info_req_id)

);

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON regprc.additional_info_request
   TO regprcuser;

CREATE TABLE regprc.anonymous_profile(
	id character varying(39) NOT NULL,
    	process_stage character varying(36) NOT NULL,
    	profile character varying NOT NULL,
    	cr_by character varying(256) NOT NULL,
    	cr_dtimes timestamp NOT NULL,
    	upd_by character varying(256),
    	upd_dtimes timestamp,
    	is_deleted boolean DEFAULT FALSE,
    	del_dtimes timestamp,
    	CONSTRAINT pk_anonymous_id PRIMARY KEY (id)
);
GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON regprc.anonymous_profile
   TO regprcuser;

CREATE TABLE regprc.reg_verification(
    workflow_instance_id character varying(36) NOT NULL,
	reg_id character varying(39) NOT NULL,
	verification_req_id character varying(39) NOT NULL,
	matched_type character varying(36),
	verification_usr_id character varying(256),
	response_text character varying(512),
	status_code character varying(36),
	reason_code character varying(36),
	status_comment character varying(256),
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_reg_ver_id PRIMARY KEY (workflow_instance_id)
);

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON regprc.reg_verification
   TO regprcuser;

ALTER TABLE regprc.registration_list RENAME COLUMN id TO workflow_instance_id;
ALTER TABLE regprc.registration_list RENAME COLUMN reg_type TO process;
ALTER TABLE regprc.registration_list ADD COLUMN additional_info_req_id character varying(256);
ALTER TABLE regprc.registration_list ADD COLUMN packet_id character varying;
ALTER TABLE regprc.registration_list ADD COLUMN source character varying;
ALTER TABLE regprc.registration_list ADD COLUMN ref_id character varying(512);

ALTER TABLE regprc.registration RENAME COLUMN id TO reg_id;
ALTER TABLE regprc.registration RENAME COLUMN reg_type TO process;
ALTER TABLE regprc.registration ADD COLUMN workflow_instance_id character varying(36);
ALTER TABLE regprc.registration ADD COLUMN source character varying;
ALTER TABLE regprc.registration ADD COLUMN iteration integer DEFAULT 1;

ALTER TABLE regprc.individual_demographic_dedup ADD COLUMN workflow_instance_id character varying(36);
ALTER TABLE regprc.individual_demographic_dedup ADD COLUMN process character varying(36);
ALTER TABLE regprc.individual_demographic_dedup ADD COLUMN iteration integer DEFAULT 1;


ALTER TABLE regprc.reg_manual_verification ADD COLUMN workflow_instance_id character varying(36);

ALTER TABLE regprc.reg_lost_uin_det ADD COLUMN workflow_instance_id character varying(36);

ALTER TABLE regprc.reg_bio_ref ADD COLUMN workflow_instance_id character varying(36);
ALTER TABLE regprc.reg_bio_ref ADD COLUMN process character varying(36);
ALTER TABLE regprc.reg_bio_ref ADD COLUMN iteration integer DEFAULT 1;

UPDATE regprc.registration_list SET packet_id = reg_id;
UPDATE regprc.registration a SET workflow_instance_id = b.workflow_instance_id FROM regprc.registration_list b WHERE a.reg_id = b.reg_id;
UPDATE regprc.individual_demographic_dedup a SET workflow_instance_id = b.workflow_instance_id FROM regprc.registration_list b WHERE a.reg_id = b.reg_id;
UPDATE regprc.reg_manual_verification a SET workflow_instance_id = b.workflow_instance_id FROM regprc.registration_list b WHERE a.reg_id = b.reg_id;
UPDATE regprc.reg_lost_uin_det a SET workflow_instance_id = b.workflow_instance_id FROM regprc.registration_list b WHERE a.reg_id = b.reg_id;
UPDATE regprc.reg_bio_ref a SET workflow_instance_id = b.workflow_instance_id FROM regprc.registration_list b WHERE a.reg_id = b.reg_id;

CREATE INDEX IF NOT EXISTS idx_rbioref_crdtimes on regprc.reg_bio_ref (cr_dtimes);
CREATE INDEX IF NOT EXISTS idx_bio_ref_id ON regprc.reg_bio_ref USING btree (bio_ref_id);
DROP INDEX IF EXISTS idx_idemogd_namedobgender;
CREATE INDEX IF NOT EXISTS idx_idemogd_namedobgender on regprc.individual_demographic_dedup (name, dob,gender);
CREATE INDEX IF NOT EXISTS idx_rbioref_crdtimes on regprc.reg_bio_ref (cr_dtimes);
CREATE INDEX IF NOT EXISTS idx_rmanvrn_reqid on regprc.reg_manual_verification (request_id);
DROP INDEX IF EXISTS idx_rgstrn_ltstrbcode_ltststscode;
CREATE INDEX IF NOT EXISTS idx_rgstrn_ltstrbcode_ltststscode on regprc.registration (latest_trn_dtimes, latest_trn_status_code);
CREATE INDEX IF NOT EXISTS idx_reg_latest_trn_dtimes ON regprc.registration USING btree (latest_trn_dtimes);
CREATE INDEX IF NOT EXISTS idx_rgstrnlst_pcktid on regprc.registration_list (packet_id);
CREATE INDEX IF NOT EXISTS idx_rgstrnlst_aireqid on regprc.registration_list (additional_info_req_id);
CREATE INDEX IF NOT EXISTS idx_reg_verification_reqId on regprc.reg_verification (verification_req_id);
CREATE INDEX IF NOT EXISTS idx_reg_trn_reg_id ON regprc.registration_transaction USING btree (reg_id);
CREATE INDEX IF NOT EXISTS idx_reg_trn_status_code ON regprc.registration_transaction USING btree (status_code);
CREATE INDEX IF NOT EXISTS idx_reg_trn_trntypecode ON regprc.registration_transaction USING btree (trn_type_code);
CREATE INDEX IF NOT EXISTS idx_reg_trn_upd_dtimes ON regprc.registration_transaction USING btree (upd_dtimes);
CREATE INDEX IF NOT EXISTS idx_user_detail_cntr_id ON regprc.abis_request USING btree (bio_ref_id);
CREATE INDEX IF NOT EXISTS idx_abis_req_regtrn_id ON regprc.abis_request USING btree (ref_regtrn_id);

ALTER TABLE regprc.individual_demographic_dedup DROP CONSTRAINT pk_idemogd_id;
ALTER TABLE regprc.individual_demographic_dedup ALTER COLUMN workflow_instance_id SET NOT NULL;
ALTER TABLE regprc.individual_demographic_dedup ADD CONSTRAINT pk_idemogd_id PRIMARY KEY (workflow_instance_id,lang_code);

ALTER TABLE regprc.reg_bio_ref DROP CONSTRAINT pk_regbref_id;
ALTER TABLE regprc.reg_bio_ref ALTER COLUMN workflow_instance_id SET NOT NULL;
ALTER TABLE regprc.reg_bio_ref ADD CONSTRAINT pk_regbref_id PRIMARY KEY (bio_ref_id,workflow_instance_id);

ALTER TABLE regprc.reg_lost_uin_det DROP CONSTRAINT pk_rlostd;
ALTER TABLE regprc.reg_lost_uin_det ALTER COLUMN workflow_instance_id SET NOT NULL;
ALTER TABLE regprc.reg_lost_uin_det ADD CONSTRAINT pk_rlostd PRIMARY KEY (workflow_instance_id);

ALTER TABLE regprc.reg_manual_verification DROP CONSTRAINT pk_rmnlver_id;
ALTER TABLE regprc.reg_manual_verification ALTER COLUMN workflow_instance_id SET NOT NULL;
ALTER TABLE regprc.reg_manual_verification ADD CONSTRAINT pk_rmnlver_id PRIMARY KEY (workflow_instance_id,matched_ref_id,matched_ref_type);

ALTER TABLE regprc.registration DROP CONSTRAINT pk_reg_id CASCADE;
ALTER TABLE regprc.registration ALTER COLUMN workflow_instance_id SET NOT NULL;
ALTER TABLE regprc.registration ADD CONSTRAINT pk_reg_id PRIMARY KEY (workflow_instance_id);

ALTER TABLE regprc.individual_demographic_dedup ADD CONSTRAINT fk_idemogd_reg FOREIGN KEY (workflow_instance_id)
REFERENCES regprc.registration (workflow_instance_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_manual_verification ADD CONSTRAINT fk_rmnlver_reg FOREIGN KEY (workflow_instance_id)
REFERENCES regprc.registration (workflow_instance_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_bio_ref ADD CONSTRAINT fk_regbrf_reg FOREIGN KEY (workflow_instance_id)
REFERENCES regprc.registration (workflow_instance_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

ALTER TABLE regprc.reg_lost_uin_det ADD CONSTRAINT fk_rlostd_reg FOREIGN KEY (workflow_instance_id)
REFERENCES regprc.registration (workflow_instance_id) MATCH SIMPLE
ON DELETE NO ACTION ON UPDATE NO ACTION;

-----------------------------------------------------------------------------------------------------------------------------------------
ALTER TABLE regprc.registration ADD COLUMN resume_timestamp timestamp;
ALTER TABLE regprc.registration ADD COLUMN default_resume_action character varying(50);


---------------------------------------------------------------------------------------------------

ALTER TABLE regprc.registration ADD COLUMN pause_rule_ids character varying(256);

----------------------------------------------------------------------------------------------------
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('VERIFICATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('QUALITY_CLASSIFIER','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('WORKFLOW_RESUME','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('CMD_VALIDATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('SUPERVISOR_VALIDATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('OPERATOR_VALIDATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('INTRODUCER_VALIDATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('INTERNAL_WORKFLOW_ACTION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('BIOMETRIC_EXTRACTION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('FINALIZATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
INSERT INTO regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes,upd_by,upd_dtimes,is_deleted,del_dtimes) VALUES
	 ('MANUAL_ADJUDICATION','transaction_done','eng',true,'MOSIP_SYSTEM',now(),NULL,NULL,false,NULL);
------------------------------------------------------------------------------------------------------


ALTER TABLE regprc.registration ADD COLUMN last_success_stage_name CHARACTER VARYING(50);
-------------------------------------------------------Add columns to registration_list----------------------------------------------

ALTER TABLE regprc.registration_list ADD COLUMN name character varying;
ALTER TABLE regprc.registration_list ADD COLUMN phone character varying;
ALTER TABLE regprc.registration_list ADD COLUMN email character varying;
ALTER TABLE regprc.registration_list ADD COLUMN center_id character varying;
ALTER TABLE regprc.registration_list ADD COLUMN registration_date date;
ALTER TABLE regprc.registration_list ADD COLUMN location_code character varying;

-------------------------------------------------------------------------------------------------------------------------------------------
-------------------------------------------------Creation of crypto salt table--------------------------------------------------------------

CREATE TABLE regprc.crypto_salt(
	id integer NOT NULL,
	salt character varying(36) NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256) ,
	upd_dtimes timestamp ,
	CONSTRAINT pk_rides PRIMARY KEY (id));

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE
   ON regprc.crypto_salt
   TO regprcuser;

--------------------------------------------------------------------------------------------------------------------------------------------

---------------------------------------------------------FOR HISTORY DATA----------------------------------------------


UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_RECEIVER' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'ExternalStage' where latest_trn_type_code='EXTERNAL_INTEGRATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'ManualVerificationStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'PrintingStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');
UPDATE regprc.registration SET last_success_stage_name = 'BiometricAuthenticationStage' where latest_trn_type_code='BIOMETRIC_AUTHENTICATION' and latest_trn_status_code in ('SUCCESS' , 'PROCESSED');

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='EXTERNAL_INTEGRATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'ExternalStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'NEW';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketClassifierStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='QualityCheckerStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='OSIValidatorStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ExternalStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'ExternalStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='DemoDedupeStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BioDedupeStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and process = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process = 'NEW';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='BIOMETRIC_AUTHENTICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BiometricAuthenticationStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process =  'UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketClassifierStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='QualityCheckerStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='OSIValidatorStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BiometricAuthenticationStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BiometricAuthenticationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='DemoDedupeStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BioDedupeStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and process =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process =  'UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'LOST';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketClassifierStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='QualityCheckerStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='OSIValidatorStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BioDedupeStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and process = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process = 'LOST';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='DemoDedupeStage' and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and process = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process = 'RES_UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process = 'RES_REPRINT';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process = 'RES_REPRINT';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process= 'ACTIVATED';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and process= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process= 'ACTIVATED';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and process ='DEACTIVATED';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and process ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and process ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and process = 'DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and process = 'DEACTIVATED';

-------------------------------------------------------------------------------------------------------------------------------------
DROP INDEX IF EXISTS regprc.idx_reg_verification_reqid;

DROP INDEX IF EXISTS regprc.idx_idemogd_namedob;

ALTER TABLE regprc.reg_bio_ref ALTER COLUMN process TYPE character varying COLLATE pg_catalog."default";

CREATE INDEX IF NOT EXISTS idx_reglist_reg_id ON regprc.registration_list USING btree (reg_id COLLATE pg_catalog."default" ASC NULLS LAST) TABLESPACE pg_default;