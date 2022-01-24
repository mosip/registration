CREATE TABLE regprc.registration(
	reg_id character varying(39) NOT NULL,
	process character varying(36) NOT NULL,
	ref_reg_id character varying(39),
	applicant_type character varying(36),
	status_code character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	status_comment character varying(256),
	latest_trn_id character varying(36),
	latest_trn_type_code character varying(36),
	latest_trn_status_code character varying(36),
	latest_trn_dtimes timestamp,
	reg_process_retry_count smallint,
	reg_stage_name character varying(36),
	trn_retry_count smallint,
	pkt_cr_dtimes timestamp,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	resume_timestamp timestamp,
	default_resume_action character varying(50),
	pause_rule_ids character varying(256),
	last_success_stage_name character varying(50),
	workflow_instance_id character varying(36) NOT NULL,
	source character varying,
	iteration integer DEFAULT 1,
	CONSTRAINT pk_reg_id PRIMARY KEY (workflow_instance_id)

);

create index idx_rgstrn_ltstrbcode_ltststscode on regprc.registration (latest_trn_dtimes, latest_trn_status_code);
CREATE INDEX IF NOT EXISTS idx_reg_latest_trn_dtimes ON regprc.registration USING btree (latest_trn_dtimes);

COMMENT ON TABLE regprc.registration IS 'Registration: Registration Processor table is to store registration requests that are being processed, Also maintains packet id details and status of the registration requests.';
COMMENT ON COLUMN regprc.registration.ref_reg_id IS 'reference Registration ID:Previous registartion ID using which UIN was generated, Which will be used as refrence for any update or correction request.';
COMMENT ON COLUMN regprc.registration.applicant_type IS 'Applicant Type: Type of the applicant who is raised registration request, Applicant type can be citizen, foreigner…etc';
COMMENT ON COLUMN regprc.registration.status_code IS 'Status Code: Status of the registration process, This will be the over all status of the registartion';
COMMENT ON COLUMN regprc.registration.lang_code IS 'Language Code : For multilanguage implementation this attribute Refers master.language.code. The value of some of the attributes in current record is stored in this respective language.';
COMMENT ON COLUMN regprc.registration.status_comment IS 'Status Comment: Comments captured as part of packet processing. This can be used in case someone wants to abort the transaction, comments can be provided as additional information.';
COMMENT ON COLUMN regprc.registration.latest_trn_id IS 'Latest Transaction ID: Transaction ID of the last registration processor side transaction that was executed for this registartion request. This can be used to handle/track transactions of an registration request.';
COMMENT ON COLUMN regprc.registration.latest_trn_type_code IS 'Latest Transaction Type Code: Transaction Type code of the last registration processor side transaction that was executed for this registartion request';
COMMENT ON COLUMN regprc.registration.latest_trn_status_code IS 'Latest Transaction Status Code:  Transaction status code of the last registration processor side transaction that was executed for this registartion request';
COMMENT ON COLUMN regprc.registration.latest_trn_dtimes IS 'latest Transation date and time:Date and time of last Transaction of the last registration processor side transaction that was executed for this registartion request';
COMMENT ON COLUMN regprc.registration.reg_process_retry_count IS 'Registration Process Retry Count: Number of time this registration process retried in case of failure or not completed ';
COMMENT ON COLUMN regprc.registration.reg_stage_name IS 'Registration Process Stage Name: Registration process stage/transaction name of the registration transaction';
COMMENT ON COLUMN regprc.registration.trn_retry_count IS 'Transaction Retry Count: Number of time this transaction retried in case of failure or not completed ';
COMMENT ON COLUMN regprc.registration.pkt_cr_dtimes IS 'Packet Created Date and Time: Date and time on which this packet is created at registration client';
COMMENT ON COLUMN regprc.registration.is_active IS 'IS_Active : Flag to mark whether the record is Active or In-active';
COMMENT ON COLUMN regprc.registration.cr_by IS 'Created By : ID or name of the user who create / insert record.';
COMMENT ON COLUMN regprc.registration.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
COMMENT ON COLUMN regprc.registration.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
COMMENT ON COLUMN regprc.registration.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
COMMENT ON COLUMN regprc.registration.is_deleted IS 'IS_Deleted : Flag to mark whether the record is Soft deleted.';
COMMENT ON COLUMN regprc.registration.del_dtimes IS 'Deleted DateTimestamp : Date and Timestamp when the record is soft deleted with is_deleted=TRUE';
