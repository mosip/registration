

-- object: reg.template_type | type: TABLE --
-- DROP TABLE IF EXISTS reg.template_type CASCADE;
CREATE TABLE reg.template_type(
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
	CONSTRAINT pk_tmpltyp_code PRIMARY KEY (code,lang_code)

);
-- ddl-end --
