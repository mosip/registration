

-- object: reg.template_file_format | type: TABLE --
-- DROP TABLE IF EXISTS reg.template_file_format CASCADE;
CREATE TABLE reg.template_file_format(
	code character varying(36) NOT NULL,
	descr character varying(256) NOT NULL,
	lang_code character varying(3) NOT NULL,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
	cr_dtimes timestamp NOT NULL,
	upd_by character varying(256),
	upd_dtimes timestamp,
	is_deleted boolean,
	del_dtimes timestamp,
	CONSTRAINT pk_tffmt_code PRIMARY KEY (code,lang_code)

);
-- ddl-end --
