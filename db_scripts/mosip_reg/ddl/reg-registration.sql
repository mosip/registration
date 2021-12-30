

-- object: reg.registration | type: TABLE --
-- DROP TABLE IF EXISTS reg.registration CASCADE;
CREATE TABLE reg.registration(
	id character varying(39) NOT NULL,
	reg_type character varying(64) NOT NULL,
	ref_reg_id character varying(39),
	prereg_id character varying(64),
	status_code character varying(36) NOT NULL,
	lang_code character varying(3) NOT NULL,
	status_comment character varying(256),
	status_dtimes timestamp,
	ack_filename character varying(128),
	client_status_code character varying(36),
	server_status_code character varying(36),
	client_status_dtime timestamp,
	server_status_dtime timestamp,
	client_status_comment character varying(256),
	server_status_comment character varying(256),
	reg_usr_id character varying(256) NOT NULL,
	regcntr_id character varying(10) NOT NULL,
	approver_usr_id character varying(256) NOT NULL,
	approver_role_code character varying(36),
	file_upload_status character varying(64),
	upload_count smallint,
	upload_dtimes timestamp,
	latest_regtrn_id character varying(36),
	latest_trn_type_code character varying(36),
	latest_trn_status_code character varying(36),
	latest_trn_lang_code character varying(3),
	latest_regtrn_dtimes timestamp,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	CONSTRAINT pk_reg_id PRIMARY KEY (id)

);
-- ddl-end --
