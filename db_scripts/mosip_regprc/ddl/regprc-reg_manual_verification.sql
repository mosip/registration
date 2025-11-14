

-- object: regprc.reg_manual_verification | type: TABLE --
-- DROP TABLE IF EXISTS regprc.reg_manual_verification CASCADE;
CREATE TABLE regprc.reg_manual_verification(
	reg_id character varying(39) NOT NULL,
	matched_ref_id character varying(39) NOT NULL,
	matched_ref_type character varying(36) NOT NULL,
	mv_usr_id character varying(256),
	ref_regtrn_id character varying(36),
	request_id character varying(36),
	res_text bytea,
	status_code character varying(36),
	reason_code character varying(36),
	status_comment character varying(256),
	trntyp_code character varying(36),
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean DEFAULT FALSE,
	del_dtimes timestamp,
	workflow_instance_id character varying(36) NOT NULL,
	CONSTRAINT pk_rmnlver_id PRIMARY KEY (workflow_instance_id,matched_ref_id,matched_ref_type)

);
-- indexes section -------------------------------------------------
create index idx_rmanvrn_reqid on regprc.reg_manual_verification (request_id);

-- ddl-end --
COMMENT ON TABLE regprc.reg_manual_verification IS 'Manual Verification: Stores all the registration request which goes through manual verification process, registration can be assinged to single/multiple manual verifier as part of the verification process';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.reg_id IS 'Registration ID: ID of the registration request';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.matched_ref_id IS 'Mached Reference ID: Reference ID of the mached registrations, This id can be RID';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.matched_ref_type IS 'Mached reference ID Type: Type of the Reference ID';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.mv_usr_id IS 'Manual Verifier ID: User ID of the manual verifier';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.ref_regtrn_id IS 'Reference Transaction ID: Transaction id for the manual adjudication transaction refered from registration transaction table';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.request_id IS 'Request ID: Request id of the request is been sent for manual adjudication at external applications';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.res_text IS 'Response Text: Text which is recived from external application for manual adjudication';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.status_code IS 'Status Code : Status of the manual verification';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.reason_code IS 'Reason Code : Reason code provided by the manual verifier on reason for approve or reject the registration request as part of the verification process';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.status_comment IS 'Status Comment: Comments captured as part of manual verification process';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.trntyp_code IS 'Transaction Type Code : Code of the transaction type';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.lang_code IS 'Language Code : For multilanguage implementation this attribute Refers master.language.code. The value of some of the attributes in current record is stored in this respective language.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.is_active IS 'IS_Active : Flag to mark whether the record is Active or In-active';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.upd_by IS 'Updated By : ID or name of the user who update the record with new values';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.is_deleted IS 'IS_Deleted : Flag to mark whether the record is Soft deleted.';
-- ddl-end --
COMMENT ON COLUMN regprc.reg_manual_verification.del_dtimes IS 'Deleted DateTimestamp : Date and Timestamp when the record is soft deleted with is_deleted=TRUE';
-- ddl-end --

