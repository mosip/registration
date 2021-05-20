-- -------------------------------------------------------------------------------------------------
-- Database Name: mosip_regprc
-- Release Version 	: 1.2
-- Purpose    		: Database Alter scripts for the release for Registration Processor DB.       
-- Create By   		: Ram Bhatt
-- Created Date		: Mar-2021
-- 
-- Modified Date        Modified By         Comments / Remarks
-- -------------------------------------------------------------------------------------------------
-- Apr-2021		Ram Bhatt	   Added resume_remove_tags column to registration table
-- Apr-2021		Ram Bhatt	   Added rows to transaction_type.csv
-- May-2021		Ram Bhatt	   Creation of last_success_stage_name in registration table
----------------------------------------------------------------------------------------------------
\c mosip_regprc sysadmin

ALTER TABLE regprc.registration ADD COLUMN resume_timestamp timestamp;
ALTER TABLE regprc.registration ADD COLUMN default_resume_action character varying(50);

---------------------------------------------------------------------------------------------------

ALTER TABLE regprc.registration ADD COLUMN resume_remove_tags character varying(256);

----------------------------------------------------------------------------------------------------

TRUNCATE TABLE regprc.transaction_type cascade ;

\COPY regprc.transaction_type (code,descr,lang_code,is_active,cr_by,cr_dtimes) FROM '../dml/regprc-transaction_type.csv' delimiter ',' HEADER  csv;

------------------------------------------------------------------------------------------------------


ALTER TABLE regprc.registration ADD COLUMN last_success_stage_name CHARACTER VARYING(50);

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

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='EXTERNAL_INTEGRATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'ExternalStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'NEW';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketClassifierStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='QualityCheckerStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='OSIValidatorStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ExternalStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'ExternalStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='DemoDedupeStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BioDedupeStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and reg_type = 'NEW';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type = 'NEW';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='BIOMETRIC_AUTHENTICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BiometricAuthenticationStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type =  'UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketClassifierStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='QualityCheckerStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='OSIValidatorStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BiometricAuthenticationStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BiometricAuthenticationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='DemoDedupeStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BioDedupeStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and reg_type =  'UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type =  'UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_CLASSIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='QUALITY_CHECK' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='OSI_VALIDATE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='BIOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'LOST';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketClassifierStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'PacketClassifierStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='QualityCheckerStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'QualityCheckerStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='OSIValidatorStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'OSIValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='BioDedupeStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'BioDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and reg_type = 'LOST';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type = 'LOST';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='VALIDATE_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='DEMOGRAPHIC_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='MANUAL_VERIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketValidatorStage' and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'PacketValidatorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='DemoDedupeStage' and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='ManualVerificationStage' and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'DemoDedupeStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and reg_type = 'RES_UPDATE';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type = 'RES_UPDATE';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type = 'RES_REPRINT';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type = 'RES_REPRINT';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type = 'RES_REPRINT';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type= 'ACTIVATED';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and reg_type= 'ACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type= 'ACTIVATED';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='SECUREZONE_NOTIFICATION' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='UPLOAD_PACKET' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='UIN_GENERATOR' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PRINT_SERVICE' and latest_trn_status_code not in ('SUCCESS' , 'PROCESSED') and reg_type ='DEACTIVATED';

UPDATE regprc.registration SET last_success_stage_name = 'PacketReceiverStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='SecurezoneNotificationStage' and reg_type ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'SecurezoneNotificationStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PacketUploaderStage' and reg_type ='DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'PacketUploaderStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='UinGeneratorStage' and reg_type = 'DEACTIVATED';
UPDATE regprc.registration SET last_success_stage_name = 'UinGeneratorStage' where latest_trn_type_code='PACKET_REPROCESS' and reg_stage_name ='PrintingStage' and reg_type = 'DEACTIVATED';

-------------------------------------------------------------------------------------------------------------------------------------

