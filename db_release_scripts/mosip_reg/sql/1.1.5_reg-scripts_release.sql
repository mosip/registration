-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_reg
-- Release Version 	: 1.1.5
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Jan-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------

\c mosip_reg sysadmin

ALTER TABLE reg.app_authentication_method ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.app_detail ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.app_role_priority ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.applicant_valid_document ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.authentication_method ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.biometric_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.blacklisted_words ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.daysofweek_list ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.device_master ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.device_provider ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.device_spec ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.device_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.doc_category ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.doc_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.dynamic_field ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.foundational_trust_provider ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.gender ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.global_param ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.id_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.identity_schema ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.individual_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.key_alias ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.key_policy_def ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.key_store ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.key_store_DRBY ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.language ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.location ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.machine_master ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.machine_spec ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.machine_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.mosip_device_service ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.pre_registration_list ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.process_list ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reason_category ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reason_list ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_center_device ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_center_machine ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_center_machine_device ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_center_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_center_user ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_center_user_machine ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_device_sub_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.reg_device_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.registered_device_master ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.registration_center ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.registration_transaction ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.role_list ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.schema_definition ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.screen_authorization ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.screen_detail ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.sync_control ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.sync_job_def ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.sync_transaction ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.template ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.template_file_format ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.template_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.title ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.transaction_type ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.user_biometric ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.user_detail ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.user_pwd ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.user_role ALTER COLUMN is_deleted SET NOT NULL;
ALTER TABLE reg.valid_document ALTER COLUMN is_deleted SET NOT NULL;

ALTER TABLE reg.app_authentication_method ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.app_detail ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.app_role_priority ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.applicant_valid_document ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.authentication_method ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.biometric_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.blacklisted_words ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.daysofweek_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.device_master ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.device_provider ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.device_spec ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.device_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.doc_category ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.doc_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.dynamic_field ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.foundational_trust_provider ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.gender ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.global_param ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.id_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.identity_schema ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.individual_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.key_alias ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.key_policy_def ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.key_store ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.key_store_DRBY ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.language ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.location ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.machine_master ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.machine_spec ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.machine_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.mosip_device_service ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.pre_registration_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.process_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reason_category ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reason_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_center_device ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_center_machine ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_center_machine_device ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_center_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_center_user ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_center_user_machine ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_device_sub_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.reg_device_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.registered_device_master ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.registration_center ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.registration_transaction ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.role_list ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.schema_definition ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.screen_authorization ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.screen_detail ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.sync_control ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.sync_job_def ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.sync_transaction ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.template ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.template_file_format ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.template_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.title ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.transaction_type ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.user_biometric ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.user_detail ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.user_pwd ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.user_role ALTER COLUMN is_deleted SET DEFAULT FALSE;
ALTER TABLE reg.valid_document ALTER COLUMN is_deleted SET DEFAULT FALSE;

------------------------------------ALTER TABLE ADD COLUMN----------------------------------------

ALTER TABLE reg.sync_job_def ADD COLUMN job_type character varying(128);

----------------------------------------------------------------------------------------------------

